package com.englishapp.curriculum.domain

import com.fasterxml.jackson.databind.JsonNode
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.*
import org.hibernate.annotations.Type
import java.time.Instant
import java.util.UUID

/**
 * Exercício pedagógico. IMUTÁVEL após publicação (CLAUDE.md §3.2):
 * para alterar, criar nova linha com version + 1 e desativar a anterior.
 */
@Entity
@Table(name = "exercises")
class Exercise(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "lesson_id", nullable = false, columnDefinition = "uuid")
    val lessonId: UUID,

    @Column(name = "order_index", nullable = false)
    val orderIndex: Int,

    @Column(nullable = false)
    val type: String,

    @Type(JsonType::class)
    @Column(name = "question_payload", nullable = false, columnDefinition = "jsonb")
    val questionPayload: JsonNode,

    @Type(JsonType::class)
    @Column(name = "correct_answer", nullable = false, columnDefinition = "jsonb")
    val correctAnswer: JsonNode,

    @Column(name = "feedback_on_error", columnDefinition = "text")
    val feedbackOnError: String? = null,

    @Column(nullable = false)
    val version: Int = 1,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
