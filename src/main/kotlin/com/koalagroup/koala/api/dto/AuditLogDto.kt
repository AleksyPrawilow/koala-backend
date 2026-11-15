package com.koalagroup.koala.api.dto

import com.koalagroup.koala.domain.entities.AuditAction
import com.koalagroup.koala.domain.entities.AuditLog
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

data class AuditLogDto(
    @field:Schema(description = "Audit entry id.")
    val id: Long,
    /** The administrator who performed the action; empty if the account no longer exists. */
    @field:Schema(description = "Administrator who performed the action; null if unknown or the account no longer exists.")
    val actorId: UUID?,
    /** The user the change applied to; empty if the account no longer exists. */
    @field:Schema(description = "User the change applied to; null if the account no longer exists.")
    val targetUserId: UUID?,
    @field:Schema(description = "Kind of change: ROLE_CHANGED or STATUS_CHANGED.")
    val action: AuditAction,
    @field:Schema(description = "State before the change.", example = "{\"role\": \"USER\"}")
    val oldValue: Map<String, String?>?,
    @field:Schema(description = "State after the change.", example = "{\"role\": \"ORGANISER\"}")
    val newValue: Map<String, String?>?,
    @field:Schema(description = "When the change was made (UTC).", example = "2026-11-14T10:15:30Z")
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
