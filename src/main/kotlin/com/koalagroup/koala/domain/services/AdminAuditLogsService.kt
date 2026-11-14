package com.koalagroup.koala.domain.services

import com.koalagroup.koala.domain.entities.AuditAction
import com.koalagroup.koala.domain.entities.AuditLog
import com.koalagroup.koala.domain.exceptions.InvalidQueryException
import com.koalagroup.koala.domain.queries.AuditLogQuery
import com.koalagroup.koala.domain.queries.PagedResult
import com.koalagroup.koala.domain.queries.SortDirection
import com.koalagroup.koala.domain.repositories.AuditLogRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class AdminAuditLogsService(
    private val auditLogRepository: AuditLogRepository
) {
    fun getAuditLogs(
        actorId: UUID?,
        targetUserId: UUID?,
        action: AuditAction?,
        from: Instant?,
        to: Instant?,
        page: Int,
        size: Int,
        sort: String
    ): PagedResult<AuditLog> {
        if (page < 0) throw InvalidQueryException("Page must be 0 or greater.")
        if (size !in 1..MAX_PAGE_SIZE) throw InvalidQueryException("Size must be between 1 and $MAX_PAGE_SIZE.")
        if (from != null && to != null && from.isAfter(to)) {
            throw InvalidQueryException("'from' must not be after 'to'.")
        }

        return auditLogRepository.search(
            AuditLogQuery(
                actorId = actorId,
                targetUserId = targetUserId,
                action = action,
                from = from,
                to = to,
                page = page,
                size = size,
                direction = parseSortDirection(sort)
            )
        )
    }

    /** Logs can only be sorted by creation time: `createdAt` or `createdAt,asc|desc`. */
    private fun parseSortDirection(sort: String): SortDirection {
        val parts = sort.split(",").map { it.trim() }
        if (parts.size > 2 || !parts[0].equals(SORT_FIELD, ignoreCase = true)) {
            throw InvalidQueryException("Cannot sort by '${parts[0]}'. Allowed field: $SORT_FIELD.")
        }
        return when (parts.getOrNull(1)?.lowercase()) {
            null, "asc" -> SortDirection.ASC
            "desc" -> SortDirection.DESC
            else -> throw InvalidQueryException("Sort direction must be 'asc' or 'desc'.")
        }
    }

    private companion object {
        const val MAX_PAGE_SIZE = 100_000
        const val SORT_FIELD = "createdAt"
    }
}
