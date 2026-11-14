package com.koalagroup.koala.infrastructure.database.postgres.mappers

import com.koalagroup.koala.domain.entities.AuditLog
import com.koalagroup.koala.infrastructure.database.postgres.entities.PostgresAuditLogEntity

fun AuditLog.toEntity(): PostgresAuditLogEntity {
    return PostgresAuditLogEntity(
        id = this.id,
        actorId = this.actorId,
        targetUserId = this.targetUserId,
        action = this.action,
        oldValue = this.oldValue,
        newValue = this.newValue,
        createdAt = this.createdAt
    )
}

fun PostgresAuditLogEntity.toDomain(): AuditLog {
    return AuditLog(
        id = this.id,
        actorId = this.actorId,
        targetUserId = this.targetUserId,
        action = this.action,
        oldValue = this.oldValue,
        newValue = this.newValue,
        createdAt = this.createdAt
    )
}
