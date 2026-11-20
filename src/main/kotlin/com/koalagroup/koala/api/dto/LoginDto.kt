package com.koalagroup.koala.api.dto

import com.koalagroup.koala.domain.entities.User
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class LoginRequest(
    @field:NotBlank(message = "Email address is required.")
    @field:Email(message = "A valid email address is required.")
    val email: String,

    @field:NotBlank(message = "Password is required.")
    @field:Size(max = 64, message = "Password cannot exceed 64 characters.")
    val password: String
)

data class LoginResponse(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: String
)

fun User.toLoginResponse(): LoginResponse =
    LoginResponse(
        id = id ?: error("Persisted user is missing an id"),
        firstName = firstName,
        lastName = lastName,
        email = email,
        role = role.name
    )
