package com.englishapp.progress.application

import com.englishapp.common.curriculum.CurriculumCatalog
import com.englishapp.common.learner.LearnerProfilePort
import com.englishapp.progress.api.dto.ModuleProgress
import com.englishapp.progress.api.dto.ProgressDashboardResponse
import com.englishapp.progress.api.dto.RecentError
import com.englishapp.progress.domain.LessonStatus
import com.englishapp.progress.infrastructure.ExerciseAttemptRepository
import com.englishapp.progress.infrastructure.LessonProgressRepository
import jakarta.persistence.EntityNotFoundException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ProgressQueryService(
    private val curriculumCatalog: CurriculumCatalog,
    private val learnerProfilePort: LearnerProfilePort,
    private val lessonProgressRepository: LessonProgressRepository,
    private val attemptRepository: ExerciseAttemptRepository
) {
    companion object {
        const val RECENT_ERRORS_LIMIT = 5
        const val RECENT_ERRORS_WINDOW_DAYS = 30L
    }

    fun getDashboard(userId: UUID): ProgressDashboardResponse {
        val profile = learnerProfilePort.getProfile(userId)
            ?: throw EntityNotFoundException("Profile for user $userId not found")

        val completedLessonIds = lessonProgressRepository.findByIdUserId(userId)
            .filter { it.status == LessonStatus.completed || it.status == LessonStatus.mastered }
            .map { it.id.lessonId }
            .toSet()

        val modules = curriculumCatalog.listPublishedModules().map { module ->
            val total = module.lessons.size
            val done = module.lessons.count { it.id in completedLessonIds }
            val nextLesson = module.lessons
                .sortedBy { it.orderIndex }
                .firstOrNull { it.id !in completedLessonIds }
            ModuleProgress(
                moduleId = module.id,
                title = module.title,
                progressPercent = if (total == 0) 0 else (done * 100 / total),
                nextLessonId = nextLesson?.id
            )
        }

        val since = Instant.now().minus(RECENT_ERRORS_WINDOW_DAYS, ChronoUnit.DAYS)
        val recentErrors = attemptRepository
            .findRecentErrors(userId, since, PageRequest.of(0, RECENT_ERRORS_LIMIT))
            .map { attempt ->
                val exercise = curriculumCatalog.findActiveExercise(attempt.exerciseId)
                RecentError(
                    exerciseId = attempt.exerciseId,
                    lessonTitle = exercise?.lessonId?.let { curriculumCatalog.findLessonTitle(it) },
                    yourAnswer = attempt.userAnswer,
                    correctAnswer = exercise?.correctAnswer
                )
            }

        return ProgressDashboardResponse(
            userId = userId,
            totalXp = profile.totalXp,
            streakDays = profile.streakDays,
            currentLevel = profile.currentLevel,
            modules = modules,
            recentErrors = recentErrors
        )
    }
}
