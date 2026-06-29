package com.englishapp.auth.application

import com.englishapp.auth.api.dto.AuthResponse
import com.englishapp.auth.infrastructure.RefreshTokenStore
import com.englishapp.auth.infrastructure.UserRepository
import com.englishapp.auth.security.JwtTokenProvider
import com.englishapp.common.exceptions.UnauthorizedException
import org.springframework.stereotype.Service

@Service
class RefreshAccessTokenUseCase(
    private val refreshTokenStore: RefreshTokenStore,
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider
) {
    /**
     * Troca um refresh token válido por um novo par (access + refresh), rotacionando.
     * Lança IllegalArgumentException (401 no handler) se o token for inválido/usado/expirado
     * ou se a conta não existir/estiver excluída.
     */
    fun execute(rawRefreshToken: String): AuthResponse {
        val userId = refreshTokenStore.consume(rawRefreshToken)
            ?: throw UnauthorizedException("Refresh token inválido ou expirado")

        val user = userRepository.findById(userId).orElse(null)
        if (user == null || user.deletedAt != null) {
            throw UnauthorizedException("Conta indisponível")
        }

        val accessToken = jwtTokenProvider.generateToken(user.id, user.email, user.role)
        val newRefreshToken = refreshTokenStore.issue(user.id)

        return AuthResponse(
            userId = user.id.toString(),
            email = user.email,
            accessToken = accessToken,
            refreshToken = newRefreshToken,
            expiresIn = jwtTokenProvider.getExpirationMs()
        )
    }
}
