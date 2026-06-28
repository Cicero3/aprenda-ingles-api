package com.englishapp.progress.application

import com.englishapp.common.curriculum.CurriculumCatalog
import com.englishapp.common.grading.Grader
import com.englishapp.common.learner.LearnerProfilePort
import com.englishapp.progress.api.dto.AttemptResult
import com.englishapp.progress.api.dto.LessonProgressSummary
import com.englishapp.progress.api.dto.SubmitAttemptsRequest
import com.englishapp.progress.api.dto.SubmitAttemptsResponse
import com.englishapp.progress.domain.ExerciseAttempt
import com.englishapp.progress.domain.LessonProgress
import com.englishapp.progress.domain.LessonProgressId
import com.englishapp.progress.domain.LessonStatus
import com.englishapp.progress.infrastructure.ExerciseAttemptRepository
import com.englishapp.progress.infrastructure.LessonProgressRepository
import jakarta.persistence.EntityNotFoundException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SubmitAttemptsUseCase(
    private val curriculumCatalog: CurriculumCatalog,
    private val grader: Grader,
    private val attemptRepository: ExerciseAttemptRepository,
    private val lessonProgressRepository: LessonProgressRepository,
    private val learnerProfilePort: LearnerProfilePort
) {
    companion object {
        const val XP_PER_CORRECT = 10
        const val COMPLETION_THRESHOLD = 80.0
        const val MASTERY_THRESHOLD = 100.0
    }

    /**
     * Grava tentativas + progresso da lição + XP em uma única transação (CLAUDE.md §3.4).
     *
     * Limitação conhecida (MVP): XP é creditado por acerto a cada submissão, sem
     * idempotência — reenviar a mesma lição farmaria XP. Anti-farming fica para fase
     * posterior (creditar só no primeiro acerto de cada exercício).
     */
    @Transactional
    fun execute(userId: UUID, request: SubmitAttemptsRequest): SubmitAttemptsResponse {
        val lessonId = request.lessonId
        val results = mutableListOf<AttemptResult>()
        var correctCount = 0
        var totalXp = 0

        for (item in request.attempts) {
            val exercise = curriculumCatalog.findActiveExercise(item.exerciseId)
                ?: throw EntityNotFoundException("Exercise ${item.exerciseId} not found or inactive")

            require(exercise.lessonId == lessonId) {
                "Exercise ${item.exerciseId} does not belong to lesson $lessonId"
            }

            val isCorrect = grader.isCorrect(
                type = exercise.type,
                payload = exercise.payload,
                correctAnswer = exercise.correctAnswer,
                userAnswer = item.userAnswer
            )
            val xpEarned = if (isCorrect) XP_PER_CORRECT else 0
            if (isCorrect) correctCount++
            totalXp += xpEarned

            attemptRepository.save(
                ExerciseAttempt(
                    userId = userId,
                    exerciseId = exercise.id,
                    exerciseVersion = exercise.version,
                    userAnswer = item.userAnswer,
                    isCorrect = isCorrect,
                    timeSpentMs = item.timeSpentMs
                )
            )

            results.add(
                AttemptResult(
                    exerciseId = exercise.id,
                    isCorrect = isCorrect,
                    xpEarned = xpEarned,
                    correctAnswer = if (isCorrect) null else exercise.correctAnswer,
                    feedback = if (isCorrect) null else exercise.feedbackOnError
                )
            )
        }

        val submitted = request.attempts.size
        val totalActive = curriculumCatalog.countActiveExercises(lessonId)
        val batchScore = if (submitted == 0) 0.0
            else BigDecimal(correctCount * 100.0 / submitted).setScale(2, RoundingMode.HALF_UP).toDouble()

        val progress = updateLessonProgress(userId, lessonId, batchScore)

        if (totalXp > 0) {
            learnerProfilePort.addXp(userId, totalXp)
        }

        return SubmitAttemptsResponse(
            results = results,
            lessonProgress = LessonProgressSummary(
                status = progress.status.name,
                currentScore = batchScore,
                exercisesRemaining = maxOf(0, totalActive - submitted)
            )
        )
    }

    private fun updateLessonProgress(userId: UUID, lessonId: UUID, batchScore: Double): LessonProgress {
        val id = LessonProgressId(userId, lessonId)
        val progress = lessonProgressRepository.findById(id).orElseGet {
            LessonProgress(id = id)
        }

        progress.attemptsCount += 1
        val score = BigDecimal(batchScore).setScale(2, RoundingMode.HALF_UP)
        if (score > progress.bestScore) {
            progress.bestScore = score
        }

        val best = progress.bestScore.toDouble()
        progress.status = when {
            best >= MASTERY_THRESHOLD -> LessonStatus.mastered
            best >= COMPLETION_THRESHOLD -> LessonStatus.completed
            else -> LessonStatus.in_progress
        }
        if (progress.status == LessonStatus.completed || progress.status == LessonStatus.mastered) {
            if (progress.completedAt == null) progress.completedAt = Instant.now()
        }
        progress.updatedAt = Instant.now()

        return lessonProgressRepository.save(progress)
    }
}
