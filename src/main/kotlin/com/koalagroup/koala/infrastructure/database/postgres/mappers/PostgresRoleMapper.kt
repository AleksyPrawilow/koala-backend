package com.koalagroup.koala.infrastructure.database.postgres.mappers

import com.koalagroup.koala.domain.entities.Role
import com.koalagroup.koala.infrastructure.database.postgres.entities.PostgresRoleEntity

fun Role.toEntity(): PostgresRoleEntity {
    return PostgresRoleEntity(
        id = this.id,
        name = this.name
    )
}

fun PostgresRoleEntity.toDomain(): Role {
    return Role(
        id = this.id ?: -1,
        name = this.name
    )
}