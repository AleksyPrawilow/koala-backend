package com.koalagroup.koala.api.controllers

import com.koalagroup.koala.api.CurrentActor
import com.koalagroup.koala.api.dto.AdminUserDto
import com.koalagroup.koala.api.dto.ChangeRoleDto
import com.koalagroup.koala.api.dto.ChangeStatusDto
import com.koalagroup.koala.api.dto.PagedResponse
import com.koalagroup.koala.api.dto.toAdminUserDto
import com.koalagroup.koala.api.dto.toResponse
import com.koalagroup.koala.domain.entities.UserStatus
import com.koalagroup.koala.domain.services.AdminUserManagementService
import com.koalagroup.koala.domain.services.AdminUsersService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("api/admin/users")
@Tag(name = "Admin - users")
class AdminUsersController(
    private val adminUsersService: AdminUsersService,
    private val adminUserManagementService: AdminUserManagementService,
    private val currentActor: CurrentActor
) {
    @GetMapping
    @Operation(
        summary = "List users",
        description = "Paginated list of all users. Optional filters are combined with AND."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Requested page of users."),
            ApiResponse(responseCode = "400", description = "Invalid page, size, sort or status parameter.")
        ]
    )
    fun getUsers(
        @Parameter(description = "Case-insensitive text matched against first name, last name and e-mail. Multiple words must all match.")
        @RequestParam("search", required = false) search: String?,
        @Parameter(description = "Exact role name, e.g. USER, GUARDIAN, ORGANISER, ADMIN.")
        @RequestParam("role", required = false) role: String?,
        @Parameter(description = "Account status.")
        @RequestParam("status", required = false) status: UserStatus?,
        @Parameter(description = "Zero-based page index.")
        @RequestParam("page", defaultValue = "0") page: Int,
        @Parameter(description = "Page size (1-100).")
        @RequestParam("size", defaultValue = "20") size: Int,
        @Parameter(description = "Sort as 'field,asc|desc'. Fields: firstName, lastName, email, role, status, createdAt.")
        @RequestParam("sort", defaultValue = "lastName,asc") sort: String
    ): ResponseEntity<PagedResponse<AdminUserDto>> {
        val users = adminUsersService.getUsers(search, role, status, page, size, sort)
        return ResponseEntity.ok(users.toResponse { it.toAdminUserDto() })
    }

    @PatchMapping("{id}/role")
    @Operation(
        summary = "Change a user's role",
        description = "Assigns one of the existing roles. Administrators cannot change their own role or demote the last active administrator."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Role changed (or already set - nothing is logged)."),
            ApiResponse(responseCode = "400", description = "Unknown or blank role, or malformed user id."),
            ApiResponse(responseCode = "403", description = "Administrators cannot change their own role."),
            ApiResponse(responseCode = "404", description = "User not found."),
            ApiResponse(responseCode = "409", description = "This is the last active administrator and cannot be demoted.")
        ]
    )
    fun changeRole(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ChangeRoleDto
    ): ResponseEntity<AdminUserDto> {
        val user = adminUserManagementService.changeRole(currentActor.id(), id, request.role)
        return ResponseEntity.ok(user.toAdminUserDto())
    }

    @PatchMapping("{id}/status")
    @Operation(
        summary = "Change an account status",
        description = "Blocks (BLOCKED), unblocks (ACTIVE), deactivates (INACTIVE) or reactivates (ACTIVE) an account. " +
            "Administrators cannot change their own status or block the last active administrator."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Status changed (or already set - nothing is logged)."),
            ApiResponse(responseCode = "400", description = "Invalid status, or malformed user id."),
            ApiResponse(responseCode = "403", description = "Administrators cannot change their own status."),
            ApiResponse(responseCode = "404", description = "User not found."),
            ApiResponse(responseCode = "409", description = "This is the last active administrator and cannot be blocked or deactivated.")
        ]
    )
    fun changeStatus(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ChangeStatusDto
    ): ResponseEntity<AdminUserDto> {
        val user = adminUserManagementService.changeStatus(currentActor.id(), id, request.status!!)
        return ResponseEntity.ok(user.toAdminUserDto())
    }

    @DeleteMapping("{id}")
    @Operation(
        summary = "Delete (deactivate) a user",
        description = "Soft delete: the account and its data are kept and the status becomes INACTIVE. Can be undone with the status endpoint."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Account deactivated (also when it already was)."),
            ApiResponse(responseCode = "400", description = "Malformed user id."),
            ApiResponse(responseCode = "403", description = "Administrators cannot deactivate themselves."),
            ApiResponse(responseCode = "404", description = "User not found."),
            ApiResponse(responseCode = "409", description = "This is the last active administrator and cannot be deactivated.")
        ]
    )
    fun deleteUser(@PathVariable id: UUID): ResponseEntity<Void> {
        adminUserManagementService.deactivate(currentActor.id(), id)
        return ResponseEntity.noContent().build()
    }
}
