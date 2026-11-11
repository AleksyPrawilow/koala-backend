package com.koalagroup.koala.infrastructure.database.postgres.repositories

import com.koalagroup.koala.domain.entities.User
import com.koalagroup.koala.domain.repositories.UserRepository
import com.koalagroup.koala.infrastructure.database.postgres.entities.PostgresUserJpaRepository
import com.koalagroup.koala.infrastructure.database.postgres.mappers.toEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
class PostgresUserRepository(
    private val jpaRepository: PostgresUserJpaRepository
) : UserRepository {
    override fun create(user: User): User {
        jpaRepository.save(user.toEntity())
        return user
    }
}