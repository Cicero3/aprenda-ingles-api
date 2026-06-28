package com.englishapp.curriculum.application

import com.englishapp.curriculum.api.dto.ExercisePublic
import com.englishapp.curriculum.api.dto.LessonDetail
import com.englishapp.curriculum.api.dto.LessonSummary
import com.englishapp.curriculum.api.dto.ModuleSummary
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
    private val exerciseRepository: ExerciseRepository
) {
    fun listModules(pageable: Pageable): Page<ModuleSummary> =
        moduleRepository.findByIsPublishedTrue(pageable).map {
            ModuleSummary(
                id = it.id,
                title = it.title,
                description = it.description,
                level = it.level,
                orderIndex = it.orderIndex
            )
        }

    fun listLessons(moduleId: UUID, pageable: Pageable): Page<LessonSummary> {
        if (!moduleRepository.existsById(moduleId)) {
            throw EntityNotFoundException("Module $moduleId not found")
        }
        return lessonRepository.findByModuleIdAndIsPublishedTrue(moduleId, pageable).map {
            LessonSummary(
                id = it.id,
                title = it.title,
                orderIndex = it.orderIndex,
                estimatedMinutes = it.estimatedMinutes
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
}
