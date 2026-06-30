package com.englishapp.progress.api

import com.englishapp.AbstractIntegrationTest
import com.englishapp.auth.api.dto.RegisterRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
class CurriculumProgressFlowIT : AbstractIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val moduleId = "11111111-1111-1111-1111-111111111111"
    // Lição 1 do Módulo 1 (seed V999): "Os Protagonistas e o Agora (Presente)" — 4 exercícios.
    private val lessonId = "22222222-2222-2222-2222-222222222001"
    private val lesson2Id = "22222222-2222-2222-2222-222222222002" // "O Lado do Não..." (2ª na trilha)
    private val exMcHeIs = "33333333-0000-0000-0000-000000000101" // mc: "He is" (index 2)
    private val exFibAre = "33333333-0000-0000-0000-000000000102" // fib: "They ___ my friends" -> "are"
    private val exMcItIs = "33333333-0000-0000-0000-000000000103" // mc: "It is cold today." (index 1)
    private val exTr = "33333333-0000-0000-0000-000000000104"     // translation -> "I am a teacher."

    private fun registerAndGetToken(): String {
        val email = "learner-${java.util.UUID.randomUUID()}@test.com"
        val result = mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest(email, "test-user-password-2024", acceptedTerms = true)))
        ).andExpect(status().isCreated).andReturn()
        return objectMapper.readTree(result.response.contentAsString)
            .get("data").get("accessToken").asText()
    }

    @Test
    fun `should list published modules`() {
        val token = registerAndGetToken()
        mockMvc.perform(get("/api/v1/modules").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].title").value("O Mestre dos Disfarces — Verbo TO BE"))
            .andExpect(jsonPath("$.meta.totalElements").value(1))
            // agregados de progresso para um aluno novo: 5 lições, nada concluído
            .andExpect(jsonPath("$.data[0].lessonCount").value(5))
            .andExpect(jsonPath("$.data[0].completedLessonCount").value(0))
            .andExpect(jsonPath("$.data[0].progressPercent").value(0))
    }

    @Test
    fun `should expose per-lesson progress and linear gating in lessons listing`() {
        val token = registerAndGetToken()

        // Antes de qualquer tentativa: L1 liberada (not_started), L2 bloqueada.
        mockMvc.perform(
            get("/api/v1/modules/$moduleId/lessons").header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].id").value(lessonId))
            .andExpect(jsonPath("$.data[0].status").value("not_started"))
            .andExpect(jsonPath("$.data[0].locked").value(false))
            .andExpect(jsonPath("$.data[1].id").value(lesson2Id))
            .andExpect(jsonPath("$.data[1].locked").value(true))

        // Submete L1 com 3/4 corretos -> in_progress (score 75, < 80, não conclui).
        val body = """
            {
              "lessonId": "$lessonId",
              "attempts": [
                { "exerciseId": "$exMcHeIs", "userAnswer": { "selected_index": 2 } },
                { "exerciseId": "$exFibAre", "userAnswer": { "text": "is" } },
                { "exerciseId": "$exMcItIs", "userAnswer": { "selected_index": 1 } },
                { "exerciseId": "$exTr", "userAnswer": { "text": "i am a teacher" } }
              ]
            }
        """.trimIndent()
        mockMvc.perform(
            post("/api/v1/attempts")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isOk)

        // Agora L1 reflete in_progress + bestScore 75; L2 segue bloqueada (L1 não concluída).
        mockMvc.perform(
            get("/api/v1/modules/$moduleId/lessons").header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].status").value("in_progress"))
            .andExpect(jsonPath("$.data[0].bestScore").value(75.0))
            .andExpect(jsonPath("$.data[0].locked").value(false))
            .andExpect(jsonPath("$.data[1].locked").value(true))
    }

    @Test
    fun `should return lesson with exercises but without gabarito`() {
        val token = registerAndGetToken()
        mockMvc.perform(get("/api/v1/lessons/$lessonId").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.exercises.length()").value(4))
            .andExpect(jsonPath("$.data.exercises[0].payload").exists())
            // gabarito e feedback NUNCA expostos ao aluno
            .andExpect(jsonPath("$.data.exercises[0].correctAnswer").doesNotExist())
            .andExpect(jsonPath("$.data.exercises[0].feedback").doesNotExist())
    }

    @Test
    fun `should grade attempts deterministically and award xp`() {
        val token = registerAndGetToken()
        val body = """
            {
              "lessonId": "$lessonId",
              "attempts": [
                { "exerciseId": "$exMcHeIs", "userAnswer": { "selected_index": 2 } },
                { "exerciseId": "$exFibAre", "userAnswer": { "text": "is" } },
                { "exerciseId": "$exMcItIs", "userAnswer": { "selected_index": 1 } },
                { "exerciseId": "$exTr", "userAnswer": { "text": "i am a teacher" } }
              ]
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/attempts")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.results.length()").value(4))
            // exercício 1 (mc "He is") correto: xp 10, sem feedback
            .andExpect(jsonPath("$.data.results[0].isCorrect").value(true))
            .andExpect(jsonPath("$.data.results[0].xpEarned").value(10))
            .andExpect(jsonPath("$.data.results[0].feedback").doesNotExist())
            // exercício 2 (fib) errado ("is" em vez de "are"): revela gabarito + feedback
            .andExpect(jsonPath("$.data.results[1].isCorrect").value(false))
            .andExpect(jsonPath("$.data.results[1].xpEarned").value(0))
            .andExpect(jsonPath("$.data.results[1].correctAnswer.text").value("are"))
            .andExpect(jsonPath("$.data.results[1].feedback").exists())
            // exercício 3 (mc "It is cold") correto
            .andExpect(jsonPath("$.data.results[2].isCorrect").value(true))
            // exercício 4 (translation) correto mesmo sem capitalização/pontuação
            .andExpect(jsonPath("$.data.results[3].isCorrect").value(true))
            // 3 de 4 corretos -> 75.00, abaixo de 80 -> in_progress
            .andExpect(jsonPath("$.data.lessonProgress.status").value("in_progress"))
            .andExpect(jsonPath("$.data.lessonProgress.currentScore").value(75.00))
            .andExpect(jsonPath("$.data.lessonProgress.exercisesRemaining").value(0))

        // dashboard reflete o XP creditado (3 acertos x 10)
        mockMvc.perform(get("/api/v1/users/me/progress").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.totalXp").value(30))
            .andExpect(jsonPath("$.data.modules[0].nextLessonId").value(lessonId))
    }

    @Test
    fun `should return 404 for unknown lesson`() {
        val token = registerAndGetToken()
        mockMvc.perform(
            get("/api/v1/lessons/99999999-9999-9999-9999-999999999999")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isNotFound)
    }
}
