package com.koalagroup.koala.infrastructure.database.postgres.repositories

import com.koalagroup.koala.domain.entities.AuditLog
import com.koalagroup.koala.domain.repositories.AuditLogRepository
import com.koalagroup.koala.infrastructure.database.postgres.entities.PostgresAuditLogJpaRepository
import com.koalagroup.koala.infrastructure.database.postgres.mappers.toDomain
import com.koalagroup.koala.infrastructure.database.postgres.mappers.toEntity
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class PostgresAuditLogRepository(
    private val jpaRepository: PostgresAuditLogJpaRepository
) : AuditLogRepository {
    override fun create(auditLog: AuditLog): AuditLog {
        return jpaRepository.save(auditLog.toEntity()).toDomain()
    }

    override fun findByTargetUserId(targetUserId: UUID): List<AuditLog> {
        return jpaRepository.findByTargetUserIdOrderByCreatedAtDesc(targetUserId).map { it.toDomain() }
    }
}
