package com.koalagroup.koala.api.dto

import com.koalagroup.koala.domain.entities.User
import com.koalagroup.koala.domain.entities.UserStatus
import java.util.UUID

data class AdminUserDto(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: String,
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
