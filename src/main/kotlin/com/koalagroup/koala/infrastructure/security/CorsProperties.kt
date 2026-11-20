package com.koalagroup.koala.infrastructure.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.security.cors")
data class CorsProperties(
    val allowedOriginPatterns: List<String> = listOf("http://localhost:*", "https://localhost:*")
)
