package com.koalagroup.koala.api.dto

import com.koalagroup.koala.domain.entities.AuditAction
import com.koalagroup.koala.domain.entities.AuditLog
import java.time.Instant
import java.util.UUID

data class AuditLogDto(
    val id: Long,
    /** The administrator who performed the action; empty if the account no longer exists. */
    val actorId: UUID?,
    /** The user the change applied to; empty if the account no longer exists. */
    val targetUserId: UUID?,
    val action: AuditAction,
    val oldValue: Map<String, String?>?,
    val newValue: Map<String, String?>?,
    val createdAt: Instant
)

fun AuditLog.toDto(): AuditLogDto {
    return AuditLogDto(
        id = this.id ?: error("Persisted audit log is missing an id"),
        actorId = this.actorId,
        targetUserId = this.targetUserId,
        action = this.action,
        oldValue = this.oldValue,
        newValue = this.newValue,
        createdAt = this.createdAt ?: error("Persisted audit log is missing a creation time")
    )
}
