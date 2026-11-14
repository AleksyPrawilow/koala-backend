package com.koalagroup.koala.infrastructure.database.postgres.repositories

import com.koalagroup.koala.domain.entities.Role
import com.koalagroup.koala.domain.repositories.RoleRepository
import com.koalagroup.koala.infrastructure.database.postgres.entities.PostgresRoleJpaRepository
import com.koalagroup.koala.infrastructure.database.postgres.mappers.toDomain
import org.springframework.stereotype.Repository

@Repository
class PostgresRoleRepository(
    private val jpaRepository: PostgresRoleJpaRepository
) : RoleRepository {
    override fun getAll(): List<Role> {
        return jpaRepository.findAll().map {
            it.toDomain()
        }
    }

    override fun findByName(name: String): Role? {
        return jpaRepository.findByName(name)?.toDomain()
    }
}