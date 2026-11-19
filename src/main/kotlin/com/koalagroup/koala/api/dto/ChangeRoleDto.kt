package com.koalagroup.koala.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class ChangeRoleDto(
    @field:NotBlank(message = "Role is required.")
    @field:Schema(description = "Name of an existing role (case-insensitive): USER, GUARDIAN, ORGANISER or ADMIN.", example = "ORGANISER")
    val role: String
)
