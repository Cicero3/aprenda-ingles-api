package com.englishapp.curriculum.infrastructure

import com.englishapp.curriculum.domain.Lesson
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LessonRepository : JpaRepository<Lesson, UUID> {
    fun findByModuleIdAndIsPublishedTrueOrderByOrderIndexAsc(moduleId: UUID): List<Lesson>

    fun findByModuleIdAndIsPublishedTrue(moduleId: UUID, pageable: Pageable): Page<Lesson>

    fun findByIdAndIsPublishedTrue(id: UUID): Lesson?
}
