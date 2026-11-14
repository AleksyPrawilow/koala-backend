package com.koalagroup.koala.domain.entities

import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID? = null,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: Role,
    val passwordSalt: String,
    val passwordHash: String,
    val status: UserStatus = UserStatus.ACTIVE,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)