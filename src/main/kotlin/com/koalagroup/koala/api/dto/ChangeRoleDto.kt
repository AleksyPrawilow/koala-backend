package com.koalagroup.koala.api.dto

import jakarta.validation.constraints.NotBlank

data class ChangeRoleDto(
    @field:NotBlank(message = "Role is required.")
    val role: String
)
