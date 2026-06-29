package com.englishapp.auth.api

import com.englishapp.AbstractIntegrationTest
import com.englishapp.auth.api.dto.RegisterRequest
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertNotEquals
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
class RefreshTokenFlowIT : AbstractIntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper

    private fun register(): JsonNode {
        val email = "refresh-${java.util.UUID.randomUUID()}@test.com"
        val result = mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest(email, "test-user-password-2024", acceptedTerms = true)))
        ).andExpect(status().isCreated).andReturn()
        return objectMapper.readTree(result.response.contentAsString).get("data")
    }

    private fun refreshBody(token: String) = """{"refreshToken":"$token"}"""

    @Test
    fun `register should issue access and refresh tokens`() {
        val data = register()
        org.junit.jupiter.api.Assertions.assertTrue(data.get("accessToken").asText().isNotBlank())
        org.junit.jupiter.api.Assertions.assertTrue(data.get("refreshToken").asText().isNotBlank())
    }

    @Test
    fun `refresh should rotate tokens and invalidate the used refresh token`() {
        val data = register()
        val refresh1 = data.get("refreshToken").asText()

        val refreshed = mockMvc.perform(
            post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody(refresh1))
        ).andExpect(status().isOk).andReturn()
        val refresh2 = objectMapper.readTree(refreshed.response.contentAsString).get("data").get("refreshToken").asText()
        assertNotEquals(refresh1, refresh2) // rotacionou

        // reusar o refresh token antigo -> 401 (uso único)
        mockMvc.perform(
            post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody(refresh1))
        ).andExpect(status().isUnauthorized)

        // o novo refresh token ainda funciona
        mockMvc.perform(
            post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody(refresh2))
        ).andExpect(status().isOk)
    }

    @Test
    fun `logout should revoke refresh and immediately block the access token`() {
        val data = register()
        val access = data.get("accessToken").asText()
        val refresh = data.get("refreshToken").asText()

        // antes do logout: o access token funciona
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer $access"))
            .andExpect(status().isOk)

        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer $access"))
            .andExpect(status().isNoContent)

        // access token revogado imediatamente (denylist) -> 403
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer $access"))
            .andExpect(status().isForbidden)

        // refresh token também foi revogado -> 401
        mockMvc.perform(
            post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody(refresh))
        ).andExpect(status().isUnauthorized)
    }
}
