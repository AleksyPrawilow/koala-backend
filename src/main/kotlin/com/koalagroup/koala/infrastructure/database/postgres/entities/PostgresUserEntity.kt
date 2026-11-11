package com.koalagroup.koala.infrastructure.database.postgres.entities

import com.koalagroup.koala.domain.entities.Role
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

@Entity
@Table(name = "users")
class PostgresUserEntity(
    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = false)
    var firstName: String,

    @Column(nullable = false)
    var lastName: String,

    @Column(nullable = false)
    var passwordSalt: String,

    @Column(nullable = false)
    var passwordHash: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    var role: PostgresRoleEntity,

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID?
)

interface PostgresUserJpaRepository : JpaRepository<PostgresUserEntity, UUID> {
    fun save(user: PostgresUserEntity): PostgresUserEntity
}