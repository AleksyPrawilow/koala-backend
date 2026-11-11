package com.koalagroup.koala.domain.repositories

import com.koalagroup.koala.domain.entities.Role

interface RoleRepository {
    fun getAll(): List<Role>
}