package com.koalagroup.koala.infrastructure.security

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("app.security.jwt")
data class JwtProperties(
    val secret: String = "",
    val expiration: Duration = Duration.ofMinutes(15),
    val cookieSecure: Boolean = true,
    val cookieSameSite: String = "Lax"
)

object JwtCookie {
    const val NAME = "AUTH_TOKEN"
}
