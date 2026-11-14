package com.koalagroup.koala.domain.services

import com.koalagroup.koala.domain.entities.User
import com.koalagroup.koala.domain.entities.UserStatus
import com.koalagroup.koala.domain.exceptions.InvalidQueryException
import com.koalagroup.koala.domain.queries.PagedResult
import com.koalagroup.koala.domain.queries.SortDirection
import com.koalagroup.koala.domain.queries.UserQuery
import com.koalagroup.koala.domain.queries.UserSortField
import com.koalagroup.koala.domain.repositories.UserRepository
import org.springframework.stereotype.Service

@Service
class AdminUsersService(
    private val userRepository: UserRepository
) {
    fun getUsers(
        search: String?,
        role: String?,
        status: UserStatus?,
        page: Int,
        size: Int,
        sort: String
    ): PagedResult<User> {
        if (page < 0) throw InvalidQueryException("Page must be 0 or greater.")
        if (size !in 1..MAX_PAGE_SIZE) throw InvalidQueryException("Size must be between 1 and $MAX_PAGE_SIZE.")
        if ((search?.length ?: 0) > MAX_SEARCH_LENGTH) {
            throw InvalidQueryException("Search cannot exceed $MAX_SEARCH_LENGTH characters.")
        }

        val (sortBy, direction) = parseSort(sort)

        return userRepository.search(
            UserQuery(
                searchTokens = search?.trim()?.split(WHITESPACE)?.filter { it.isNotEmpty() } ?: emptyList(),
                role = role?.trim()?.uppercase()?.ifEmpty { null },
                status = status,
                page = page,
                size = size,
                sortBy = sortBy,
                direction = direction
            )
        )
    }

    /** Parses `field` or `field,asc|desc`, e.g. `lastName,desc`. */
    private fun parseSort(sort: String): Pair<UserSortField, SortDirection> {
        val parts = sort.split(",").map { it.trim() }
        if (parts.size > 2) throw InvalidQueryException("Sort must have the form 'field' or 'field,asc|desc'.")

        val field = UserSortField.fromApiName(parts[0]) ?: throw InvalidQueryException(
            "Cannot sort by '${parts[0]}'. Allowed fields: ${UserSortField.entries.joinToString { it.apiName }}."
        )
        val direction = when (parts.getOrNull(1)?.lowercase()) {
            null, "asc" -> SortDirection.ASC
            "desc" -> SortDirection.DESC
            else -> throw InvalidQueryException("Sort direction must be 'asc' or 'desc'.")
        }
        return field to direction
    }

    private companion object {
        const val MAX_PAGE_SIZE = 100
        const val MAX_SEARCH_LENGTH = 128
        val WHITESPACE = Regex("\\s+")
    }
}
