package com.englishapp.auth.application

import com.englishapp.auth.domain.User
import com.englishapp.auth.infrastructure.UserRepository
import com.englishapp.common.util.PasswordHasher
import org.springframework.stereotype.Service

@Service
class LoginUserUseCase(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher
) {
    fun execute(email: String, password: String): User {
        val normalizedEmail = email.trim().lowercase()

        val user = userRepository.findByEmail(normalizedEmail)
            ?: throw IllegalArgumentException("Email ou senha incorretos")

        if (!passwordHasher.verify(password, user.passwordHash)) {
            throw IllegalArgumentException("Email ou senha incorretos")
        }

        return user
    }
}