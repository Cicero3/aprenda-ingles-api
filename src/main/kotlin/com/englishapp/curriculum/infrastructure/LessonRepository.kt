package com.englishapp.curriculum.infrastructure

import com.englishapp.curriculum.domain.Lesson
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LessonRepository : JpaRepository<Lesson, UUID> {
    fun findByModuleIdAndIsPublishedTrueOrderByOrderIndexAsc(moduleId: UUID): List<Lesson>

    fun findByModuleIdAndIsPublishedTrueOrderByOrderIndexAsc(moduleId: UUID, pageable: Pageable): Page<Lesson>

    /** Lições publicadas de vários módulos numa só query (evita N+1 ao montar a lista de módulos). */
    fun findByModuleIdInAndIsPublishedTrue(moduleIds: Collection<UUID>): List<Lesson>

    fun findByIdAndIsPublishedTrue(id: UUID): Lesson?
}
