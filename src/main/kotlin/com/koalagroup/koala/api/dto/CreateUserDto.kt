package com.koalagroup.koala.api.dto

import com.koalagroup.koala.domain.entities.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CreateUserDto(
    @field:NotBlank(message = "Email address is required.")
    @field:Email(message = "A valid email address is required.")
    val email: String,

    @field:NotBlank(message = "Password is required.")
    @field:Size(
        min = 8,
        max = 64,
        message = "Password must be between 8 and 64 characters long."
    )
    val password: String,

    @field:NotBlank(message = "First name is required.")
    @field:Size(max = 100, message = "First name cannot exceed 100 characters.")
    val firstName: String,

    @field:NotBlank(message = "Last name is required.")
    @field:Size(max = 100, message = "Last name cannot exceed 100 characters.")
    val lastName: String,

    @field:NotNull(message = "Role is required.")
    var role: Role,

    @field:Pattern(
        regexp = "^[A-Za-z0-9]{6}$",
        message = "Organiser code must be exactly 6 alphanumeric characters."
    )
    val organiserCode: String? = null,
)