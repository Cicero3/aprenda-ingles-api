package com.englishapp.auth.api.dto

import jakarta.validation.constraints.NotBlank

data class RefreshRequest(
    @field:NotBlank(message = "refreshToken é obrigatório")
    val refreshToken: String
)
