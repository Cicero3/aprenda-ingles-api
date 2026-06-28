package com.englishapp.auth.security

import com.englishapp.common.config.JwtProperties
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    private val jwtProperties: JwtProperties
) {
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(
        jwtProperties.secret.toByteArray(StandardCharsets.UTF_8)
    )

    fun generateToken(userId: UUID, email: String): String {
        val now = Instant.now()
        val expiry = now.plusMillis(jwtProperties.expirationMs)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .issuer(jwtProperties.issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(secretKey)
            .compact()
    }

    fun validateToken(token: String): Boolean = try {
        Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
        true
    } catch (e: Exception) {
        false
    }

    fun extractUserId(token: String): UUID {
        val claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
        return UUID.fromString(claims.subject)
    }

    fun getExpirationMs(): Long = jwtProperties.expirationMs
}