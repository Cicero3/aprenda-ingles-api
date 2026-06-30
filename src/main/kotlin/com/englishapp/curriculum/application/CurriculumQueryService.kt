package com.englishapp.curriculum.application

import com.englishapp.common.progress.LessonProgressQuery
import com.englishapp.common.progress.LessonProgressSnapshot
import com.englishapp.common.progress.LessonProgressStatus
import com.englishapp.curriculum.api.dto.ExercisePublic
import com.englishapp.curriculum.api.dto.LessonDetail
import com.englishapp.curriculum.api.dto.LessonSummary
import com.englishapp.curriculum.api.dto.ModuleSummary
import com.englishapp.curriculum.domain.Lesson
import com.englishapp.curriculum.infrastructure.ExerciseRepository
import com.englishapp.curriculum.infrastructure.LessonRepository
import com.englishapp.curriculum.infrastructure.ModuleRepository
import jakarta.persistence.EntityNotFoundException
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CurriculumQueryService(
    private val moduleRepository: ModuleRepository,
    private val lessonRepository: LessonRepository,
    private val exerciseRepository: ExerciseRepository,
    private val lessonProgressQuery: LessonProgressQuery
) {
    fun listModules(userId: UUID, pageable: Pageable): Page<ModuleSummary> {
        val page = moduleRepository.findByIsPublishedTrueOrderByOrderIndexAsc(pageable)
        val moduleIds = page.content.map { it.id }

        // Uma query para todas as lições da página + uma para o progresso (evita N+1, CLAUDE.md §9.2).
        val lessonsByModule = if (moduleIds.isEmpty()) emptyMap()
            else lessonRepository.findByModuleIdInAndIsPublishedTrue(moduleIds).groupBy { it.moduleId }
        val allLessonIds = lessonsByModule.values.flatten().map { it.id }
        val snapshots = lessonProgressQuery.snapshotsFor(userId, allLessonIds)

        return page.map { module ->
            val lessons = lessonsByModule[module.id].orEmpty()
            val total = lessons.size
            val completed = lessons.count { snapshots[it.id]?.status?.isCompleted == true }
            ModuleSummary(
                id = module.id,
                title = module.title,
                description = module.description,
                level = module.level,
                orderIndex = module.orderIndex,
                lessonCount = total,
                completedLessonCount = completed,
                progressPercent = if (total == 0) 0 else completed * 100 / total
            )
        }
    }

    fun listLessons(moduleId: UUID, userId: UUID, pageable: Pageable): Page<LessonSummary> {
        if (!moduleRepository.existsById(moduleId)) {
            throw EntityNotFoundException("Module $moduleId not found")
        }

        // Gating é linear, então precisamos da lista COMPLETA ordenada (não só a página)
        // para saber se a lição anterior foi concluída.
        val allOrdered = lessonRepository.findByModuleIdAndIsPublishedTrueOrderByOrderIndexAsc(moduleId)
        val snapshots = lessonProgressQuery.snapshotsFor(userId, allOrdered.map { it.id })
        val states = computeLessonStates(allOrdered, snapshots)

        return lessonRepository.findByModuleIdAndIsPublishedTrueOrderByOrderIndexAsc(moduleId, pageable).map { lesson ->
            val state = states.getValue(lesson.id)
            LessonSummary(
                id = lesson.id,
                title = lesson.title,
                orderIndex = lesson.orderIndex,
                estimatedMinutes = lesson.estimatedMinutes,
                status = state.status,
                bestScore = state.bestScore,
                locked = state.locked
            )
        }
    }

    fun getLesson(lessonId: UUID): LessonDetail {
        val lesson = lessonRepository.findByIdAndIsPublishedTrue(lessonId)
            ?: throw EntityNotFoundException("Lesson $lessonId not found")

        val exercises = exerciseRepository
            .findByLessonIdAndIsActiveTrueOrderByOrderIndexAsc(lessonId)
            .map {
                // Nunca expor correct_answer nem feedback_on_error ao aluno.
                ExercisePublic(
                    id = it.id,
                    orderIndex = it.orderIndex,
                    type = it.type,
                    payload = it.questionPayload
                )
            }

        return LessonDetail(
            id = lesson.id,
            moduleId = lesson.moduleId,
            title = lesson.title,
            content = lesson.content,
            exercises = exercises
        )
    }

    private data class LessonState(val status: String, val bestScore: Double, val locked: Boolean)

    /**
     * Calcula status/score/locked por lição. Gating linear: a primeira lição está sempre
     * liberada; cada lição seguinte só destrava quando a anterior está concluída.
     */
    private fun computeLessonStates(
        ordered: List<Lesson>,
        snapshots: Map<UUID, LessonProgressSnapshot>
    ): Map<UUID, LessonState> {
        val result = LinkedHashMap<UUID, LessonState>(ordered.size)
        var previousCompleted = true // primeira lição sempre liberada
        for (lesson in ordered) {
            val snapshot = snapshots[lesson.id]
            val status = snapshot?.status ?: LessonProgressStatus.not_started
            result[lesson.id] = LessonState(
                status = status.name,
                bestScore = snapshot?.bestScore ?: 0.0,
                locked = !previousCompleted
            )
            previousCompleted = status.isCompleted
        }
        return result
    }
}
