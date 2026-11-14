package com.koalagroup.koala.infrastructure.database.postgres.entities

import com.koalagroup.koala.domain.entities.AuditAction
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "audit_logs")
class PostgresAuditLogEntity(
    @Column(name = "actor_id")
    var actorId: UUID?,

    @Column(name = "target_user_id")
    var targetUserId: UUID?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var action: AuditAction,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_value", columnDefinition = "jsonb")
    var oldValue: Map<String, String?>?,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value", columnDefinition = "jsonb")
    var newValue: Map<String, String?>?,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
)

interface PostgresAuditLogJpaRepository : JpaRepository<PostgresAuditLogEntity, Long> {
    fun findByTargetUserIdOrderByCreatedAtDesc(targetUserId: UUID): List<PostgresAuditLogEntity>
}
