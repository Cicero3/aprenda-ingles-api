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

    private val lessonId = "22222222-2222-2222-2222-222222222222"
    private val exMc = "33333333-3333-3333-3333-333333333333"
    private val exFib = "44444444-4444-4444-4444-444444444444"
    private val exTr = "55555555-5555-5555-5555-555555555555"

    private fun registerAndGetToken(): String {
        val email = "learner-${System.currentTimeMillis()}@test.com"
        val result = mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest(email, "test-user-password-2024")))
        ).andExpect(status().isCreated).andReturn()
        return objectMapper.readTree(result.response.contentAsString)
            .get("data").get("accessToken").asText()
    }

    @Test
    fun `should list published modules`() {
        val token = registerAndGetToken()
        mockMvc.perform(get("/api/v1/modules").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].title").value("Verbo TO BE - Fundamentos"))
            .andExpect(jsonPath("$.meta.totalElements").value(1))
    }

    @Test
    fun `should return lesson with exercises but without gabarito`() {
        val token = registerAndGetToken()
        mockMvc.perform(get("/api/v1/lessons/$lessonId").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.exercises.length()").value(3))
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
                { "exerciseId": "$exMc", "userAnswer": { "selected_index": 2 } },
                { "exerciseId": "$exFib", "userAnswer": { "text": "are" } },
                { "exerciseId": "$exTr", "userAnswer": { "text": "we were tired" } }
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
            .andExpect(jsonPath("$.data.results.length()").value(3))
            // exercício 1 (mc) correto: xp 10, sem feedback
            .andExpect(jsonPath("$.data.results[0].isCorrect").value(true))
            .andExpect(jsonPath("$.data.results[0].xpEarned").value(10))
            .andExpect(jsonPath("$.data.results[0].feedback").doesNotExist())
            // exercício 2 (fib) errado: revela gabarito + feedback
            .andExpect(jsonPath("$.data.results[1].isCorrect").value(false))
            .andExpect(jsonPath("$.data.results[1].xpEarned").value(0))
            .andExpect(jsonPath("$.data.results[1].correctAnswer.text").value("is"))
            .andExpect(jsonPath("$.data.results[1].feedback").exists())
            // 2 de 3 corretos -> 66.67, abaixo de 80 -> in_progress
            .andExpect(jsonPath("$.data.lessonProgress.status").value("in_progress"))
            .andExpect(jsonPath("$.data.lessonProgress.currentScore").value(66.67))
            .andExpect(jsonPath("$.data.lessonProgress.exercisesRemaining").value(0))

        // dashboard reflete o XP creditado (2 acertos x 10)
        mockMvc.perform(get("/api/v1/users/me/progress").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.totalXp").value(20))
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
