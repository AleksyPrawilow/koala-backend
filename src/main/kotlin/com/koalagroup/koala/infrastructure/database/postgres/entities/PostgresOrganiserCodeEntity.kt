package com.koalagroup.koala.infrastructure.database.postgres.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository

@Entity
@Table(name = "organiser_invitation_codes")
class PostgresOrganiserCodeEntity(
    @Column(nullable = false)
    var code: String,

    @Column(nullable = false)
    var used: Boolean,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long?
)

interface PostgresOrganiserCodeJpaRepository : JpaRepository<PostgresOrganiserCodeEntity, Long> {
    fun findByCodeAndUsedFalse(code: String): PostgresOrganiserCodeEntity?
}