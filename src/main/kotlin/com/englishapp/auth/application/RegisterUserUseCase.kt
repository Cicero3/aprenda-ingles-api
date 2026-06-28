package com.englishapp.auth.application

import com.englishapp.auth.domain.User
import com.englishapp.auth.domain.UserProfile
import com.englishapp.auth.infrastructure.UserProfileRepository
import com.englishapp.auth.infrastructure.UserRepository
import com.englishapp.common.util.PasswordHasher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegisterUserUseCase(
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository,
    private val passwordHasher: PasswordHasher
) {
    @Transactional
    fun execute(email: String, password: String): User {
        val normalizedEmail = email.trim().lowercase()

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw IllegalArgumentException("Email já registrado")
        }

        if (password.length < 8) {
            throw IllegalArgumentException("Senha deve ter pelo menos 8 caracteres")
        }

        val user = User(
            email = normalizedEmail,
            passwordHash = passwordHasher.hash(password)
        )
        val savedUser = userRepository.save(user)

        // Não passar userId explicitamente - o @MapsId cuida disso
        val profile = UserProfile(
            user = savedUser
        )
        userProfileRepository.save(profile)

        return savedUser
    }
}