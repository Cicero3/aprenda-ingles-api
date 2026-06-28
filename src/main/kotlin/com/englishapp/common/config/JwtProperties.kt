package com.englishapp.common.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "jwt")
class JwtProperties {
    var secret: String = "change-me-in-production-must-be-at-least-256-bits-long-xxxxxxxxxx"
    var expirationMs: Long = 86400000 // 24h
    var issuer: String = "english-app"
}