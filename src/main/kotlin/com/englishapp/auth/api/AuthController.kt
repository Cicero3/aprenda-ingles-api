package com.englishapp.auth.api

import com.englishapp.auth.api.dto.AuthResponse
import com.englishapp.auth.api.dto.LoginRequest
import com.englishapp.auth.api.dto.RegisterRequest
import com.englishapp.auth.application.LoginUserUseCase
import com.englishapp.auth.application.RegisterUserUseCase
import com.englishapp.auth.security.JwtTokenProvider
import com.englishapp.auth.security.UserPrincipal
import com.englishapp.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val registerUserUseCase: RegisterUserUseCase,
    private val loginUserUseCase: LoginUserUseCase,
    private val jwtTokenProvider: JwtTokenProvider
) {
    @PostMapping("/register")
    fun register(
        @Valid @RequestBody request: RegisterRequest
    ): ResponseEntity<ApiResponse<AuthResponse>> {
        val user = registerUserUseCase.execute(request.email, request.password)
        val token = jwtTokenProvider.generateToken(user.id, user.email)

        val response = AuthResponse(
            userId = user.id.toString(),
            email = user.email,
            accessToken = token,
            expiresIn = jwtTokenProvider.getExpirationMs()
        )

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(data = response))
    }

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest
    ): ResponseEntity<ApiResponse<AuthResponse>> {
        val user = loginUserUseCase.execute(request.email, request.password)
        val token = jwtTokenProvider.generateToken(user.id, user.email)

        val response = AuthResponse(
            userId = user.id.toString(),
            email = user.email,
            accessToken = token,
            expiresIn = jwtTokenProvider.getExpirationMs()
        )

        return ResponseEntity.ok(ApiResponse(data = response))
    }

    @GetMapping("/me")
    fun me(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        return ResponseEntity.ok(
            ApiResponse(
                data = mapOf(
                    "userId" to principal.id.toString(),
                    "email" to principal.email
                )
            )
        )
    }
}