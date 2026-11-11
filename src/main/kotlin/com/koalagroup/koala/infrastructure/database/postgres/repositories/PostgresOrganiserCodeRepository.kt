package com.koalagroup.koala.infrastructure.database.postgres.repositories

import com.koalagroup.koala.domain.entities.OrganiserCode
import com.koalagroup.koala.domain.repositories.OrganiserCodeRepository
import com.koalagroup.koala.infrastructure.database.postgres.entities.PostgresOrganiserCodeEntity
import com.koalagroup.koala.infrastructure.database.postgres.entities.PostgresOrganiserCodeJpaRepository
import com.koalagroup.koala.infrastructure.database.postgres.mappers.toDomain
import jakarta.transaction.Transactional
import org.springframework.stereotype.Repository

@Repository
class PostgresOrganiserCodeRepository(
    private val jpaRepository: PostgresOrganiserCodeJpaRepository
) : OrganiserCodeRepository {
    @Transactional
    override fun useCode(code: String): OrganiserCode? {
        val retrievedCode: PostgresOrganiserCodeEntity = jpaRepository.findByCodeAndUsedFalse(code) ?: return null
        retrievedCode.used = true
        return retrievedCode.toDomain()
    }
}