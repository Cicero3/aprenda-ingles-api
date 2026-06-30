package com.englishapp.curriculum.api.dto

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

data class ModuleSummary(
    val id: UUID,
    val title: String,
    val description: String?,
    val level: String,
    val orderIndex: Int,
    /** Total de lições publicadas no módulo. */
    val lessonCount: Int,
    /** Quantas dessas lições o aluno já concluiu (completed/mastered). */
    val completedLessonCount: Int,
    /** Percentual concluído (0–100), arredondado para baixo. */
    val progressPercent: Int
)

data class LessonSummary(
    val id: UUID,
    val title: String,
    val orderIndex: Int,
    val estimatedMinutes: Int,
    /** Progresso do aluno: not_started | in_progress | completed | mastered. */
    val status: String,
    /** Melhor pontuação do aluno na lição (0–100). 0 quando ainda não tentou. */
    val bestScore: Double,
    /** true quando a lição anterior na trilha ainda não foi concluída (gating linear). */
    val locked: Boolean
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
