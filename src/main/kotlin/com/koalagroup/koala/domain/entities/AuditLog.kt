package com.koalagroup.koala.domain.entities

import java.time.Instant
import java.util.UUID

data class AuditLog(
    val id: Long? = null,
    /** The administrator performing the action. */
    val actorId: UUID?,
    /** The user the change applies to. */
    val targetUserId: UUID?,
    val action: AuditAction,
    /** State before the change, e.g. {"role": "USER"}. */
    val oldValue: Map<String, String?>? = null,
    /** State after the change, e.g. {"role": "ORGANISER"}. */
    val newValue: Map<String, String?>? = null,
    val createdAt: Instant? = null
)
