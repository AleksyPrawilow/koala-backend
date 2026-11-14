package com.koalagroup.koala.infrastructure.database.postgres.repositories

import com.koalagroup.koala.domain.entities.User
import com.koalagroup.koala.domain.queries.PagedResult
import com.koalagroup.koala.domain.queries.SortDirection
import com.koalagroup.koala.domain.queries.UserQuery
import com.koalagroup.koala.domain.queries.UserSortField
import com.koalagroup.koala.domain.repositories.UserRepository
import com.koalagroup.koala.infrastructure.database.postgres.entities.PostgresUserJpaRepository
import com.koalagroup.koala.infrastructure.database.postgres.mappers.toDomain
import com.koalagroup.koala.infrastructure.database.postgres.mappers.toEntity
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class PostgresUserRepository(
    private val jpaRepository: PostgresUserJpaRepository
) : UserRepository {
    override fun create(user: User): User {
        jpaRepository.save(user.toEntity())
        return user
    }

    @Transactional(readOnly = true)
    override fun search(query: UserQuery): PagedResult<User> {
        val direction = if (query.direction == SortDirection.DESC) Sort.Direction.DESC else Sort.Direction.ASC
        val order = Sort.Order(direction, query.sortBy.toProperty()).let {
            if (query.sortBy.isText()) it.ignoreCase() else it
        }
        // The id tie-breaker keeps the page boundaries stable when the sorted values repeat.
        val sort = Sort.by(order, Sort.Order.asc("id"))

        val result = jpaRepository.findAll(
            PostgresUserSpecifications.from(query),
            PageRequest.of(query.page, query.size, sort)
        )

        return PagedResult(
            items = result.content.map { it.toDomain() },
            page = query.page,
            size = query.size,
            totalElements = result.totalElements
        )
    }

    private fun UserSortField.toProperty(): String = when (this) {
        UserSortField.FIRST_NAME -> "firstName"
        UserSortField.LAST_NAME -> "lastName"
        UserSortField.EMAIL -> "email"
        UserSortField.ROLE -> "role.name"
        UserSortField.STATUS -> "status"
        UserSortField.CREATED_AT -> "createdAt"
    }

    private fun UserSortField.isText(): Boolean =
        this == UserSortField.FIRST_NAME || this == UserSortField.LAST_NAME ||
            this == UserSortField.EMAIL || this == UserSortField.ROLE
}
