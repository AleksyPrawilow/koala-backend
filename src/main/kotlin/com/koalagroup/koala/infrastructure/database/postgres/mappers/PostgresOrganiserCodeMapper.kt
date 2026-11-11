package com.koalagroup.koala.infrastructure.database.postgres.mappers

import com.koalagroup.koala.domain.entities.OrganiserCode
import com.koalagroup.koala.domain.entities.Role
import com.koalagroup.koala.infrastructure.database.postgres.entities.PostgresOrganiserCodeEntity
import com.koalagroup.koala.infrastructure.database.postgres.entities.PostgresRoleEntity

fun OrganiserCode.toEntity(): PostgresOrganiserCodeEntity {
    return PostgresOrganiserCodeEntity(
        id = this.id,
        code = this.code,
        used = this.used
    )
}

fun PostgresOrganiserCodeEntity.toDomain(): OrganiserCode {
    return OrganiserCode(
        id = this.id ?: -1,
        code = this.code,
        used = this.used
    )
}