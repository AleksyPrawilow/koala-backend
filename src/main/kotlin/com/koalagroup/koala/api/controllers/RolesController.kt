package com.koalagroup.koala.api.controllers

import com.koalagroup.koala.domain.entities.Role
import com.koalagroup.koala.domain.services.RolesService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/roles")
class RolesController(
    private val rolesService: RolesService
) {
    @GetMapping
    fun getRoles(): ResponseEntity<List<Role>> {
        return ResponseEntity.ok(rolesService.getRoles())
    }
}