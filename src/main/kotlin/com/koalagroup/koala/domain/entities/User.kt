package com.koalagroup.koala.domain.entities

import java.util.UUID

data class User(
    val id: UUID? = null,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: Role,
    val passwordSalt: String,
    val passwordHash: String
)