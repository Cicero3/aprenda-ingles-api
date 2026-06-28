package com.englishapp.progress.api.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class SubmitAttemptsRequest(
    @field:NotNull(message = "lessonId é obrigatório")
    val lessonId: UUID,

    @field:NotEmpty(message = "attempts não pode ser vazio")
    @field:Valid
    val attempts: List<AttemptItem>
)

data class AttemptItem(
    @field:NotNull(message = "exerciseId é obrigatório")
    val exerciseId: UUID,

    @field:NotNull(message = "userAnswer é obrigatório")
    val userAnswer: JsonNode,

    val timeSpentMs: Int? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AttemptResult(
    val exerciseId: UUID,
    val isCorrect: Boolean,
    val xpEarned: Int,
    val correctAnswer: JsonNode? = null,
    val feedback: String? = null
)

data class LessonProgressSummary(
    val status: String,
    val currentScore: Double,
    val exercisesRemaining: Int
)

data class SubmitAttemptsResponse(
    val results: List<AttemptResult>,
    val lessonProgress: LessonProgressSummary
)

data class ModuleProgress(
    val moduleId: UUID,
    val title: String,
    val progressPercent: Int,
    val nextLessonId: UUID?
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RecentError(
    val exerciseId: UUID,
    val lessonTitle: String?,
    val yourAnswer: JsonNode,
    val correctAnswer: JsonNode?
)

data class ProgressDashboardResponse(
    val userId: UUID,
    val totalXp: Int,
    val streakDays: Int,
    val currentLevel: String,
    val modules: List<ModuleProgress>,
    val recentErrors: List<RecentError>
)
