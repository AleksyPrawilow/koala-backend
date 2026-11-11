package com.koalagroup.koala.domain.services

import com.koalagroup.koala.domain.entities.Role
import com.koalagroup.koala.domain.repositories.RoleRepository
import org.springframework.stereotype.Service

@Service
class RolesService(
    private val rolesRepository: RoleRepository
) {
    fun getRoles(): List<Role> {
        return rolesRepository.getAll()
    }
}