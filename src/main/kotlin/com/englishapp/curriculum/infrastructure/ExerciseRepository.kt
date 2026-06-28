package com.englishapp.curriculum.infrastructure

import com.englishapp.curriculum.domain.Exercise
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ExerciseRepository : JpaRepository<Exercise, UUID> {
    fun findByLessonIdAndIsActiveTrueOrderByOrderIndexAsc(lessonId: UUID): List<Exercise>

    fun findByIdAndIsActiveTrue(id: UUID): Exercise?
}
