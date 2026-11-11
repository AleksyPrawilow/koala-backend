package com.koalagroup.koala.infrastructure.database.postgres.mappers

import com.koalagroup.koala.domain.entities.User
import com.koalagroup.koala.infrastructure.database.postgres.entities.PostgresUserEntity
import java.util.UUID

fun User.toEntity(): PostgresUserEntity {
    return PostgresUserEntity(
        id = this.id,
        firstName = this.firstName,
        lastName = this.lastName,
        email = this.email,
        passwordSalt = this.passwordSalt,
        passwordHash = this.passwordHash,
        role = this.role.toEntity()
    )
}

fun PostgresUserEntity.toDomain(): User {
    return User(
        id = this.id ?: UUID.randomUUID(),
        firstName = this.firstName,
        lastName = this.lastName,
        email = this.email,
        passwordSalt = this.passwordSalt,
        passwordHash = this.passwordHash,
        role = this.role.toDomain()
    )
}