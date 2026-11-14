package com.koalagroup.koala.domain.queries

import com.koalagroup.koala.domain.entities.AuditAction
import java.time.Instant
import java.util.UUID

data class AuditLogQuery(
    /** The administrator who performed the action. */
    val actorId: UUID? = null,
    /** The user the change applies to. */
    val targetUserId: UUID? = null,
    val action: AuditAction? = null,
    /** Inclusive lower bound of the creation time. */
    val from: Instant? = null,
    /** Inclusive upper bound of the creation time. */
    val to: Instant? = null,
    val page: Int = 0,
    val size: Int = 20,
    val direction: SortDirection = SortDirection.DESC
)
