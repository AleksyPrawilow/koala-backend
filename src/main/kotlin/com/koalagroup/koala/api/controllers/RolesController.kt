package com.koalagroup.koala.api.controllers

import com.koalagroup.koala.domain.entities.Role
import com.koalagroup.koala.domain.services.RolesService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/roles")
@Tag(name = "Roles")
class RolesController(
    private val rolesService: RolesService
) {
    @GetMapping
    @Operation(
        summary = "List roles",
        description = "Returns every role defined in the system (id and name)."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "List of all roles.")
        ]
    )
    fun getRoles(): ResponseEntity<List<Role>> {
        return ResponseEntity.ok(rolesService.getRoles())
    }
}