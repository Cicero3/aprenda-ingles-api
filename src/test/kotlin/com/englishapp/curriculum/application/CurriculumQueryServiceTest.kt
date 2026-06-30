package com.englishapp.curriculum.application

import com.englishapp.common.progress.LessonProgressQuery
import com.englishapp.common.progress.LessonProgressSnapshot
import com.englishapp.common.progress.LessonProgressStatus
import com.englishapp.curriculum.domain.Lesson
import com.englishapp.curriculum.domain.Module
import com.englishapp.curriculum.infrastructure.ExerciseRepository
import com.englishapp.curriculum.infrastructure.LessonRepository
import com.englishapp.curriculum.infrastructure.ModuleRepository
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class CurriculumQueryServiceTest {

    private val moduleRepository = mockk<ModuleRepository>()
    private val lessonRepository = mockk<LessonRepository>()
    private val exerciseRepository = mockk<ExerciseRepository>()
    private val lessonProgressQuery = mockk<LessonProgressQuery>()

    private val service = CurriculumQueryService(
        moduleRepository, lessonRepository, exerciseRepository, lessonProgressQuery
    )

    private val userId = UUID.randomUUID()
    private val moduleId = UUID.randomUUID()
    private val l1 = UUID.randomUUID()
    private val l2 = UUID.randomUUID()
    private val l3 = UUID.randomUUID()
    private val pageable = PageRequest.of(0, 20)

    private fun lesson(id: UUID, order: Int) = Lesson(
        id = id,
        moduleId = moduleId,
        title = "Lição $order",
        orderIndex = order,
        estimatedMinutes = 10,
        content = JsonNodeFactory.instance.objectNode(),
        isPublished = true
    )

    private fun module() = Module(
        id = moduleId,
        title = "Módulo 1",
        description = "desc",
        level = "A1",
        orderIndex = 1,
        isPublished = true
    )

    private val orderedLessons = listOf(lesson(l1, 1), lesson(l2, 2), lesson(l3, 3))

    @Test
    fun `listLessons mapeia status, score e gating linear conforme o progresso`() {
        every { moduleRepository.existsById(moduleId) } returns true
        every { lessonRepository.findByModuleIdAndIsPublishedTrueOrderByOrderIndexAsc(moduleId) } returns orderedLessons
        every { lessonRepository.findByModuleIdAndIsPublishedTrueOrderByOrderIndexAsc(moduleId, pageable) } returns
            PageImpl(orderedLessons, pageable, 3)
        every { lessonProgressQuery.snapshotsFor(userId, any()) } returns mapOf(
            l1 to LessonProgressSnapshot(l1, LessonProgressStatus.completed, 90.0),
            l2 to LessonProgressSnapshot(l2, LessonProgressStatus.in_progress, 50.0)
        )

        val result = service.listLessons(moduleId, userId, pageable).content

        // L1: concluída, sempre liberada
        assertThat(result[0].status).isEqualTo("completed")
        assertThat(result[0].bestScore).isEqualTo(90.0)
        assertThat(result[0].locked).isFalse()
        // L2: em andamento, liberada porque L1 está concluída
        assertThat(result[1].status).isEqualTo("in_progress")
        assertThat(result[1].bestScore).isEqualTo(50.0)
        assertThat(result[1].locked).isFalse()
        // L3: sem progresso -> not_started, BLOQUEADA porque L2 não foi concluída
        assertThat(result[2].status).isEqualTo("not_started")
        assertThat(result[2].bestScore).isEqualTo(0.0)
        assertThat(result[2].locked).isTrue()
    }

    @Test
    fun `listLessons libera so a primeira licao quando nao ha progresso algum`() {
        every { moduleRepository.existsById(moduleId) } returns true
        every { lessonRepository.findByModuleIdAndIsPublishedTrueOrderByOrderIndexAsc(moduleId) } returns orderedLessons
        every { lessonRepository.findByModuleIdAndIsPublishedTrueOrderByOrderIndexAsc(moduleId, pageable) } returns
            PageImpl(orderedLessons, pageable, 3)
        every { lessonProgressQuery.snapshotsFor(userId, any()) } returns emptyMap()

        val result = service.listLessons(moduleId, userId, pageable).content

        assertThat(result.map { it.locked }).containsExactly(false, true, true)
        assertThat(result.map { it.status }).containsOnly("not_started")
    }

    @Test
    fun `listModules agrega lessonCount, concluidas e percentual`() {
        every { moduleRepository.findByIsPublishedTrueOrderByOrderIndexAsc(pageable) } returns PageImpl(listOf(module()), pageable, 1)
        every { lessonRepository.findByModuleIdInAndIsPublishedTrue(listOf(moduleId)) } returns orderedLessons
        every { lessonProgressQuery.snapshotsFor(userId, any()) } returns mapOf(
            l1 to LessonProgressSnapshot(l1, LessonProgressStatus.completed, 100.0),
            l2 to LessonProgressSnapshot(l2, LessonProgressStatus.in_progress, 40.0)
        )

        val module = service.listModules(userId, pageable).content.single()

        assertThat(module.lessonCount).isEqualTo(3)
        assertThat(module.completedLessonCount).isEqualTo(1) // só L1 (mastered/completed contam; in_progress não)
        assertThat(module.progressPercent).isEqualTo(33) // 1/3 -> 33
    }
}
