package com.koalagroup.koala.domain.queries

import com.koalagroup.koala.domain.entities.UserStatus

enum class SortDirection { ASC, DESC }

/** Fields the user list can be sorted by. [apiName] is the value accepted by the `sort` query parameter. */
enum class UserSortField(val apiName: String) {
    FIRST_NAME("firstName"),
    LAST_NAME("lastName"),
    EMAIL("email"),
    ROLE("role"),
    STATUS("status"),
    CREATED_AT("createdAt");

    companion object {
        fun fromApiName(name: String): UserSortField? = entries.firstOrNull { it.apiName.equals(name, ignoreCase = true) }
    }
}

data class UserQuery(
    /** Free-text tokens; every token must match the first name, last name or e-mail (case-insensitive). */
    val searchTokens: List<String> = emptyList(),
    /** Exact role name, e.g. "ORGANISER". */
    val role: String? = null,
    val status: UserStatus? = null,
    val page: Int = 0,
    val size: Int = 20,
    val sortBy: UserSortField = UserSortField.LAST_NAME,
    val direction: SortDirection = SortDirection.ASC
)
