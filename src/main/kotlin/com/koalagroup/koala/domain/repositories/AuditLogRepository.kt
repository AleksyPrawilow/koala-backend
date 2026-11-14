package com.koalagroup.koala.domain.repositories

import com.koalagroup.koala.domain.entities.AuditLog
import java.util.UUID

interface AuditLogRepository {
    fun create(auditLog: AuditLog): AuditLog
    fun findByTargetUserId(targetUserId: UUID): List<AuditLog>
}
