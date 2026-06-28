package com.englishapp.common.curriculum

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

/**
 * Porta de leitura do conteúdo pedagógico exposta a outras features
 * (ex.: progress). Evita que progress importe classes de curriculum
 * diretamente (CLAUDE.md §4). Implementada por um adapter em curriculum.
 */
interface CurriculumCatalog {
    fun findActiveExercise(exerciseId: UUID): ExerciseView?
    fun countActiveExercises(lessonId: UUID): Int
    fun findLessonTitle(lessonId: UUID): String?
    fun listPublishedModules(): List<ModuleView>
}

data class ExerciseView(
    val id: UUID,
    val lessonId: UUID,
    val type: String,
    val version: Int,
    val payload: JsonNode,
    val correctAnswer: JsonNode,
    val feedbackOnError: String?
)

data class ModuleView(
    val id: UUID,
    val title: String,
    val orderIndex: Int,
    val lessons: List<LessonRef>
)

data class LessonRef(
    val id: UUID,
    val orderIndex: Int
)
