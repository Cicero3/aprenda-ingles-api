package com.englishapp.common.util

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class PasswordHasher {
    private val encoder = BCryptPasswordEncoder(10)

    fun hash(rawPassword: String): String = encoder.encode(rawPassword)

    fun verify(rawPassword: String, hashedPassword: String): Boolean =
        encoder.matches(rawPassword, hashedPassword)
}