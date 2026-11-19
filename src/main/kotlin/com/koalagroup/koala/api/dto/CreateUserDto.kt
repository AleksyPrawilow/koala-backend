package com.koalagroup.koala.api.dto

import com.koalagroup.koala.domain.entities.Role
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CreateUserDto(
    @field:NotBlank(message = "Email address is required.")
    @field:Email(message = "A valid email address is required.")
    @field:Schema(description = "E-mail address, also used to log in. Must be unique.", example = "jan.kowalski@example.com")
    val email: String,

    @field:NotBlank(message = "Password is required.")
    @field:Size(
        min = 8,
        max = 64,
        message = "Password must be between 8 and 64 characters long."
    )
    @field:Schema(description = "Password, 8-64 characters.", example = "Haslo1234")
    val password: String,

    @field:NotBlank(message = "First name is required.")
    @field:Size(max = 100, message = "First name cannot exceed 100 characters.")
    @field:Schema(description = "First name, up to 100 characters.", example = "Jan")
    val firstName: String,

    @field:NotBlank(message = "Last name is required.")
    @field:Size(max = 100, message = "Last name cannot exceed 100 characters.")
    @field:Schema(description = "Last name, up to 100 characters.", example = "Kowalski")
    val lastName: String,

    @field:NotNull(message = "Role is required.")
    @field:Schema(description = "Role chosen during registration, as returned by GET /api/roles. The ADMIN role cannot be chosen.")
    var role: Role,

    @field:Pattern(
        regexp = "^[A-Za-z0-9]{6}$",
        message = "Organiser code must be exactly 6 alphanumeric characters."
    )
    @field:Schema(description = "One-time invitation code, required only for the ORGANISER role: exactly 6 alphanumeric characters.", example = "AB12CD")
    val organiserCode: String? = null,
)