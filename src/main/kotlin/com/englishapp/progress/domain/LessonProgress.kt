package com.englishapp.progress.domain

import jakarta.persistence.*
import java.io.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Embeddable
class LessonProgressId(
    @Column(name = "user_id", columnDefinition = "uuid")
    val userId: UUID,

    @Column(name = "lesson_id", columnDefinition = "uuid")
    val lessonId: UUID
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LessonProgressId) return false
        return userId == other.userId && lessonId == other.lessonId
    }

    override fun hashCode(): Int = 31 * userId.hashCode() + lessonId.hashCode()
}

enum class LessonStatus { not_started, in_progress, completed, mastered }

@Entity
@Table(name = "lesson_progress")
class LessonProgress(
    @EmbeddedId
    val id: LessonProgressId,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: LessonStatus = LessonStatus.not_started,

    @Column(name = "best_score", nullable = false)
    var bestScore: BigDecimal = BigDecimal.ZERO,

    @Column(name = "attempts_count", nullable = false)
    var attemptsCount: Int = 0,

    @Column(name = "completed_at")
    var completedAt: Instant? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
