package com.englishapp.auth.api

import com.englishapp.auth.api.dto.AuthResponse
import com.englishapp.auth.api.dto.LoginRequest
import com.englishapp.auth.api.dto.RefreshRequest
import com.englishapp.auth.api.dto.RegisterRequest
import com.englishapp.auth.application.LoginUserUseCase
import com.englishapp.auth.application.RefreshAccessTokenUseCase
import com.englishapp.auth.application.RegisterUserUseCase
import com.englishapp.auth.infrastructure.RefreshTokenStore
import com.englishapp.auth.infrastructure.TokenDenylist
import com.englishapp.auth.security.JwtTokenProvider
import com.englishapp.auth.security.UserPrincipal
import com.englishapp.common.dto.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "Auth", description = "Registro, login, refresh e logout (JWT)")
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val registerUserUseCase: RegisterUserUseCase,
    private val loginUserUseCase: LoginUserUseCase,
    private val refreshAccessTokenUseCase: RefreshAccessTokenUseCase,
    private val refreshTokenStore: RefreshTokenStore,
    private val tokenDenylist: TokenDenylist,
    private val jwtTokenProvider: JwtTokenProvider
) {
    @PostMapping("/register")
    fun register(
        @Valid @RequestBody request: RegisterRequest
    ): ResponseEntity<ApiResponse<AuthResponse>> {
        val user = registerUserUseCase.execute(request.email, request.password, request.acceptedTerms)
        val response = issueTokens(user.id, user.email, user.role)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse(data = response))
    }

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest
    ): ResponseEntity<ApiResponse<AuthResponse>> {
        val user = loginUserUseCase.execute(request.email, request.password)
        val response = issueTokens(user.id, user.email, user.role)
        return ResponseEntity.ok(ApiResponse(data = response))
    }

    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody request: RefreshRequest
    ): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = refreshAccessTokenUseCase.execute(request.refreshToken)
        return ResponseEntity.ok(ApiResponse(data = response))
    }

    @PostMapping("/logout")
    fun logout(
        @AuthenticationPrincipal principal: UserPrincipal,
        httpRequest: HttpServletRequest
    ): ResponseEntity<Void> {
        // Revoga todas as sessões (refresh tokens) do usuário.
        refreshTokenStore.revokeAllForUser(principal.id)
        // Invalida imediatamente o access token atual (denylist por jti até expirar).
        resolveAccessToken(httpRequest)?.let { token ->
            jwtTokenProvider.extractJti(token)?.let { jti ->
                tokenDenylist.denylist(jti, jwtTokenProvider.remainingMillis(token))
            }
        }
        return ResponseEntity.noContent().build()
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

    private fun issueTokens(userId: java.util.UUID, email: String, role: String): AuthResponse {
        val accessToken = jwtTokenProvider.generateToken(userId, email, role)
        val refreshToken = refreshTokenStore.issue(userId)
        return AuthResponse(
            userId = userId.toString(),
            email = email,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = jwtTokenProvider.getExpirationMs()
        )
    }

    private fun resolveAccessToken(request: HttpServletRequest): String? {
        val bearer = request.getHeader("Authorization")
        return if (bearer != null && bearer.startsWith("Bearer ")) bearer.substring(7) else null
    }
}
