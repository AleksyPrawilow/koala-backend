package com.koalagroup.koala.api.dto

import com.koalagroup.koala.domain.entities.User
import com.koalagroup.koala.domain.entities.UserStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class AdminUserDto(
    @field:Schema(description = "User id.")
    val id: UUID,
    @field:Schema(example = "Jan")
    val firstName: String,
    @field:Schema(example = "Kowalski")
    val lastName: String,
    @field:Schema(example = "jan.kowalski@example.com")
    val email: String,
    @field:Schema(description = "Role name.", example = "USER")
    val role: String,
    @field:Schema(description = "Account status.")
    val status: UserStatus
)

fun User.toAdminUserDto(): AdminUserDto {
    return AdminUserDto(
        id = this.id ?: error("Persisted user is missing an id"),
        firstName = this.firstName,
        lastName = this.lastName,
        email = this.email,
        role = this.role.name,
        status = this.status
    )
}
