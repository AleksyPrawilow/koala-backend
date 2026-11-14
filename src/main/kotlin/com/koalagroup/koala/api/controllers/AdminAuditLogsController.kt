package com.koalagroup.koala.api.controllers

import com.koalagroup.koala.api.dto.AuditLogDto
import com.koalagroup.koala.api.dto.PagedResponse
import com.koalagroup.koala.api.dto.toDto
import com.koalagroup.koala.api.dto.toResponse
import com.koalagroup.koala.domain.entities.AuditAction
import com.koalagroup.koala.domain.services.AdminAuditLogsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("api/admin/audit-logs")
@Tag(name = "Admin - audit logs")
class AdminAuditLogsController(
    private val adminAuditLogsService: AdminAuditLogsService
) {
    // TODO: restrict to the ADMIN role once authentication / RBAC (Spring Security + JWT) is in place.
    //  Until then the endpoint is open, like the rest of the API (see SecurityConfig).
    @GetMapping
    @Operation(
        summary = "List audit log entries",
        description = "Paginated history of role and status changes, newest first. Optional filters are combined with AND."
    )
    fun getAuditLogs(
        @Parameter(description = "Only changes made by this administrator.")
        @RequestParam("actorId", required = false) actorId: UUID?,
        @Parameter(description = "Only changes made to this user.")
        @RequestParam("userId", required = false) userId: UUID?,
        @Parameter(description = "ROLE_CHANGED or STATUS_CHANGED.")
        @RequestParam("action", required = false) action: AuditAction?,
        @Parameter(description = "Earliest change time (inclusive), ISO-8601, e.g. 2026-11-01T00:00:00Z.")
        @RequestParam("from", required = false) from: Instant?,
        @Parameter(description = "Latest change time (inclusive), ISO-8601.")
        @RequestParam("to", required = false) to: Instant?,
        @Parameter(description = "Zero-based page index.")
        @RequestParam("page", defaultValue = "0") page: Int,
        @Parameter(description = "Page size (1-100).")
        @RequestParam("size", defaultValue = "20") size: Int,
        @Parameter(description = "Sort as 'createdAt,asc|desc'.")
        @RequestParam("sort", defaultValue = "createdAt,desc") sort: String
    ): ResponseEntity<PagedResponse<AuditLogDto>> {
        val logs = adminAuditLogsService.getAuditLogs(actorId, userId, action, from, to, page, size, sort)
        return ResponseEntity.ok(logs.toResponse { it.toDto() })
    }
}
