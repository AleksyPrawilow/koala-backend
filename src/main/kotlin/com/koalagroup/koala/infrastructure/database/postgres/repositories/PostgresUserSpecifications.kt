package com.koalagroup.koala.infrastructure.database.postgres.repositories

import com.koalagroup.koala.domain.entities.UserStatus
import com.koalagroup.koala.domain.queries.UserQuery
import com.koalagroup.koala.infrastructure.database.postgres.entities.PostgresRoleEntity
import com.koalagroup.koala.infrastructure.database.postgres.entities.PostgresUserEntity
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import org.springframework.data.jpa.domain.Specification

object PostgresUserSpecifications {

    private const val LIKE_ESCAPE = '\\'

    fun from(query: UserQuery): Specification<PostgresUserEntity> =
        Specification { root, criteriaQuery, cb ->
            // Load the role together with the users (avoids N+1) - but not in the count query.
            val resultType = criteriaQuery.resultType
            if (resultType != Long::class.javaObjectType && resultType != Long::class.javaPrimitiveType) {
                root.fetch<PostgresUserEntity, PostgresRoleEntity>("role", JoinType.LEFT)
            }

            val predicates = mutableListOf<Predicate>()

            query.status?.let {
                predicates += cb.equal(root.get<UserStatus>("status"), it)
            }

            query.role?.let {
                predicates += cb.equal(root.get<PostgresRoleEntity>("role").get<String>("name"), it)
            }

            // Every token has to match at least one of the searchable fields.
            query.searchTokens.forEach { token ->
                val pattern = "%${escapeLike(token.lowercase())}%"
                predicates += cb.or(
                    cb.like(cb.lower(root.get("firstName")), pattern, LIKE_ESCAPE),
                    cb.like(cb.lower(root.get("lastName")), pattern, LIKE_ESCAPE),
                    cb.like(cb.lower(root.get("email")), pattern, LIKE_ESCAPE)
                )
            }

            cb.and(*predicates.toTypedArray())
        }

    /** Makes `%`, `_` and the escape character itself match literally inside a LIKE pattern. */
    private fun escapeLike(value: String): String =
        value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
