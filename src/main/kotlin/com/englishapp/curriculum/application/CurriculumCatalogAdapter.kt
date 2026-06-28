package com.englishapp.curriculum.application

import com.englishapp.common.curriculum.CurriculumCatalog
import com.englishapp.common.curriculum.ExerciseView
import com.englishapp.common.curriculum.LessonRef
import com.englishapp.common.curriculum.ModuleView
import com.englishapp.curriculum.infrastructure.ExerciseRepository
import com.englishapp.curriculum.infrastructure.LessonRepository
import com.englishapp.curriculum.infrastructure.ModuleRepository
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class CurriculumCatalogAdapter(
    private val moduleRepository: ModuleRepository,
    private val lessonRepository: LessonRepository,
    private val exerciseRepository: ExerciseRepository
) : CurriculumCatalog {

    override fun findActiveExercise(exerciseId: UUID): ExerciseView? =
        exerciseRepository.findByIdAndIsActiveTrue(exerciseId)?.let {
            ExerciseView(
                id = it.id,
                lessonId = it.lessonId,
                type = it.type,
                version = it.version,
                payload = it.questionPayload,
                correctAnswer = it.correctAnswer,
                feedbackOnError = it.feedbackOnError
            )
        }

    override fun countActiveExercises(lessonId: UUID): Int =
        exerciseRepository.findByLessonIdAndIsActiveTrueOrderByOrderIndexAsc(lessonId).size

    override fun findLessonTitle(lessonId: UUID): String? =
        lessonRepository.findById(lessonId).map { it.title }.orElse(null)

    override fun listPublishedModules(): List<ModuleView> =
        moduleRepository.findByIsPublishedTrueOrderByOrderIndexAsc().map { module ->
            val lessons = lessonRepository
                .findByModuleIdAndIsPublishedTrueOrderByOrderIndexAsc(module.id)
                .map { LessonRef(id = it.id, orderIndex = it.orderIndex) }
            ModuleView(
                id = module.id,
                title = module.title,
                orderIndex = module.orderIndex,
                lessons = lessons
            )
        }
}
