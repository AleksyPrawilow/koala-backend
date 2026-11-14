package com.koalagroup.koala.infrastructure.database.postgres.repositories

import com.koalagroup.koala.domain.entities.AuditAction
import com.koalagroup.koala.domain.queries.AuditLogQuery
import com.koalagroup.koala.infrastructure.database.postgres.entities.PostgresAuditLogEntity
import jakarta.persistence.criteria.Predicate
import org.springframework.data.jpa.domain.Specification
import java.time.Instant
import java.util.UUID

object PostgresAuditLogSpecifications {

    fun from(query: AuditLogQuery): Specification<PostgresAuditLogEntity> =
        Specification { root, _, cb ->
            val predicates = mutableListOf<Predicate>()

            query.actorId?.let { predicates += cb.equal(root.get<UUID>("actorId"), it) }
            query.targetUserId?.let { predicates += cb.equal(root.get<UUID>("targetUserId"), it) }
            query.action?.let { predicates += cb.equal(root.get<AuditAction>("action"), it) }
            query.from?.let { predicates += cb.greaterThanOrEqualTo(root.get<Instant>("createdAt"), it) }
            query.to?.let { predicates += cb.lessThanOrEqualTo(root.get<Instant>("createdAt"), it) }

            cb.and(*predicates.toTypedArray())
        }
}
