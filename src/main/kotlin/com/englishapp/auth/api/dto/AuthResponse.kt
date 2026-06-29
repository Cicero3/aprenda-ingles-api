package com.englishapp.auth.api.dto

data class AuthResponse(
    val userId: String,
    val email: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long
)
