package com.koalagroup.koala.infrastructure.database.postgres.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

@Entity
@Table(name = "roles")
class PostgresRoleEntity(
    @Column(nullable = false)
    var name: String,

    @OneToMany(mappedBy = "role")
    var users: MutableList<PostgresUserEntity> = mutableListOf(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long?
)

interface PostgresRoleJpaRepository : JpaRepository<PostgresRoleEntity, Long> {
}