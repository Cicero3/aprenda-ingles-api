package com.englishapp.curriculum.api.dto

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

data class ModuleSummary(
    val id: UUID,
    val title: String,
    val description: String?,
    val level: String,
    val orderIndex: Int
)

data class LessonSummary(
    val id: UUID,
    val title: String,
    val orderIndex: Int,
    val estimatedMinutes: Int
)

/**
 * Exercício exposto ao aluno: contém apenas o enunciado/payload.
 * NUNCA inclui correct_answer nem feedback_on_error (revelados só na correção).
 */
data class ExercisePublic(
    val id: UUID,
    val orderIndex: Int,
    val type: String,
    val payload: JsonNode
)

data class LessonDetail(
    val id: UUID,
    val moduleId: UUID,
    val title: String,
    val content: JsonNode,
    val exercises: List<ExercisePublic>
)
