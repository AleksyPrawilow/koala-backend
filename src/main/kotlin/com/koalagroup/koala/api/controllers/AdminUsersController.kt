package com.koalagroup.koala.api.controllers

import com.koalagroup.koala.api.dto.AdminUserDto
import com.koalagroup.koala.api.dto.PagedResponse
import com.koalagroup.koala.api.dto.toAdminUserDto
import com.koalagroup.koala.api.dto.toResponse
import com.koalagroup.koala.domain.entities.UserStatus
import com.koalagroup.koala.domain.services.AdminUsersService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/admin/users")
@Tag(name = "Admin - users")
class AdminUsersController(
    private val adminUsersService: AdminUsersService
) {
    // TODO: restrict to the ADMIN role once authentication / RBAC (Spring Security + JWT) is in place.
    //  Until then the endpoint is open, like the rest of the API (see SecurityConfig).
    @GetMapping
    @Operation(
        summary = "List users",
        description = "Paginated list of all users. Optional filters are combined with AND."
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
}
