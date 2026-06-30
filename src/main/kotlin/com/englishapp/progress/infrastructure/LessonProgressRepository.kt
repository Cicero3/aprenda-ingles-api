package com.englishapp.progress.infrastructure

import com.englishapp.progress.domain.LessonProgress
import com.englishapp.progress.domain.LessonProgressId
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LessonProgressRepository : JpaRepository<LessonProgress, LessonProgressId> {
    fun findByIdUserId(userId: UUID): List<LessonProgress>

    fun findByIdUserIdAndIdLessonIdIn(userId: UUID, lessonIds: Collection<UUID>): List<LessonProgress>
}
