package com.koalagroup.koala.domain.repositories

import com.koalagroup.koala.domain.entities.AuditLog
import com.koalagroup.koala.domain.queries.AuditLogQuery
import com.koalagroup.koala.domain.queries.PagedResult
import java.util.UUID

interface AuditLogRepository {
    fun create(auditLog: AuditLog): AuditLog
    fun findByTargetUserId(targetUserId: UUID): List<AuditLog>
    fun search(query: AuditLogQuery): PagedResult<AuditLog>
}
