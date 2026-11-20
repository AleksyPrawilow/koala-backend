package com.koalagroup.koala.infrastructure.mail

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "koala.mail")
data class MailProperties(
    val enabled: Boolean = true,
    val from: String = "no-reply@koala.local"
)
