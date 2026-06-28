package com.englishapp.progress.domain

import com.fasterxml.jackson.databind.JsonNode
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.*
import org.hibernate.annotations.Type
import java.time.Instant
import java.util.UUID

/**
 * Tentativa de exercício. Append-only e armazenada em tabela particionada
 * por mês (CLAUDE.md §6.3). Grava sempre exerciseVersion do momento (§3.2).
 * createdAt é definido na criação para rotear a linha à partição correta.
 */
@Entity
@Table(name = "exercise_attempts")
class ExerciseAttempt(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    val userId: UUID,

    @Column(name = "exercise_id", nullable = false, columnDefinition = "uuid")
    val exerciseId: UUID,

    @Column(name = "exercise_version", nullable = false)
    val exerciseVersion: Int,

    @Type(JsonType::class)
    @Column(name = "user_answer", nullable = false, columnDefinition = "jsonb")
    val userAnswer: JsonNode,

    @Column(name = "is_correct", nullable = false)
    val isCorrect: Boolean,

    @Column(name = "time_spent_ms")
    val timeSpentMs: Int? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
