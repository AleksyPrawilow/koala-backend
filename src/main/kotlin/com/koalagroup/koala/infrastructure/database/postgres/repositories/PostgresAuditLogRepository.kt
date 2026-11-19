package com.koalagroup.koala.infrastructure.database.postgres.repositories

import com.koalagroup.koala.domain.entities.AuditLog
import com.koalagroup.koala.domain.queries.AuditLogQuery
import com.koalagroup.koala.domain.queries.PagedResult
import com.koalagroup.koala.domain.queries.SortDirection
import com.koalagroup.koala.domain.repositories.AuditLogRepository
import com.koalagroup.koala.infrastructure.database.postgres.entities.PostgresAuditLogJpaRepository
import com.koalagroup.koala.infrastructure.database.postgres.mappers.toDomain
import com.koalagroup.koala.infrastructure.database.postgres.mappers.toEntity
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
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

    override fun search(query: AuditLogQuery): PagedResult<AuditLog> {
        val direction = if (query.direction == SortDirection.ASC) Sort.Direction.ASC else Sort.Direction.DESC
        // The id tie-breaker keeps the page boundaries stable for entries created at the same time.
        val sort = Sort.by(Sort.Order(direction, "createdAt"), Sort.Order(direction, "id"))

        val result = jpaRepository.findAll(
            PostgresAuditLogSpecifications.from(query),
            PageRequest.of(query.page, query.size, sort)
        )

        return PagedResult(
            items = result.content.map { it.toDomain() },
            page = query.page,
            size = query.size,
            totalElements = result.totalElements
        )
    }
}