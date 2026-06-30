package com.englishapp.progress.application

import com.englishapp.common.curriculum.CurriculumCatalog
import com.englishapp.common.curriculum.ExerciseView
import com.englishapp.common.grading.Grader
import com.englishapp.common.learner.LearnerProfilePort
import com.englishapp.progress.api.dto.AttemptItem
import com.englishapp.progress.api.dto.SubmitAttemptsRequest
import com.englishapp.progress.domain.ExerciseAttempt
import com.englishapp.progress.domain.LessonProgress
import com.englishapp.progress.infrastructure.ExerciseAttemptRepository
import com.englishapp.progress.infrastructure.LessonProgressRepository
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SubmitAttemptsUseCaseTest {

    private val curriculumCatalog = mockk<CurriculumCatalog>()
    private val grader = mockk<Grader>()
    private val attemptRepository = mockk<ExerciseAttemptRepository>()
    private val lessonProgressRepository = mockk<LessonProgressRepository>()
    private val learnerProfilePort = mockk<LearnerProfilePort>()

    private val useCase = SubmitAttemptsUseCase(
        curriculumCatalog, grader, attemptRepository, lessonProgressRepository, learnerProfilePort
    )

    private val userId = UUID.randomUUID()
    private val lessonId = UUID.randomUUID()
    private val exA = UUID.randomUUID()
    private val exB = UUID.randomUUID()
    private val empty: JsonNode = JsonNodeFactory.instance.objectNode()

    @BeforeEach
    fun setup() {
        every { curriculumCatalog.findActiveExercise(exA) } returns view(exA)
        every { curriculumCatalog.findActiveExercise(exB) } returns view(exB)
        every { curriculumCatalog.countActiveExercises(lessonId) } returns 2
        every { grader.isCorrect(any(), any(), any(), any()) } returns true
        every { attemptRepository.save(any()) } answers { firstArg<ExerciseAttempt>() }
        every { lessonProgressRepository.findById(any()) } returns Optional.empty()
        every { lessonProgressRepository.save(any()) } answers { firstArg<LessonProgress>() }
        every { learnerProfilePort.addXp(any(), any()) } just Runs
    }

    private fun view(id: UUID) = ExerciseView(
        id = id, lessonId = lessonId, type = "multiple_choice", version = 1,
        payload = empty, correctAnswer = empty, feedbackOnError = null
    )

    private fun request(vararg exerciseIds: UUID) = SubmitAttemptsRequest(
        lessonId = lessonId,
        attempts = exerciseIds.map { AttemptItem(exerciseId = it, userAnswer = empty) }
    )

    @Test
    fun `credita XP para exercicios acertados pela primeira vez`() {
        every { attemptRepository.findCorrectlyAnsweredExerciseIds(userId, any()) } returns emptyList()

        val response = useCase.execute(userId, request(exA, exB))

        assertThat(response.results.map { it.xpEarned }).containsExactly(10, 10)
        verify(exactly = 1) { learnerProfilePort.addXp(userId, 20) }
    }

    @Test
    fun `nao re-credita XP ao refazer exercicios ja acertados`() {
        every { attemptRepository.findCorrectlyAnsweredExerciseIds(userId, any()) } returns listOf(exA, exB)

        val response = useCase.execute(userId, request(exA, exB))

        assertThat(response.results.map { it.xpEarned }).containsExactly(0, 0)
        // ainda corretos -> score 100 (mastered), mas sem XP
        assertThat(response.lessonProgress.currentScore).isEqualTo(100.0)
        verify(exactly = 0) { learnerProfilePort.addXp(any(), any()) }
    }

    @Test
    fun `credita XP so para o exercicio recem-acertado`() {
        // exA já estava correto antes; exB não.
        every { attemptRepository.findCorrectlyAnsweredExerciseIds(userId, any()) } returns listOf(exA)

        val response = useCase.execute(userId, request(exA, exB))

        assertThat(response.results[0].xpEarned).isEqualTo(0) // exA
        assertThat(response.results[1].xpEarned).isEqualTo(10) // exB
        verify(exactly = 1) { learnerProfilePort.addXp(userId, 10) }
    }

    @Test
    fun `deduplica acertos do mesmo exercicio dentro do lote`() {
        every { attemptRepository.findCorrectlyAnsweredExerciseIds(userId, any()) } returns emptyList()

        val response = useCase.execute(userId, request(exA, exA))

        assertThat(response.results.map { it.xpEarned }).containsExactly(10, 0)
        verify(exactly = 1) { learnerProfilePort.addXp(userId, 10) }
    }
}
