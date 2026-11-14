package com.koalagroup.koala.domain.repositories

import com.koalagroup.koala.domain.entities.User
import com.koalagroup.koala.domain.queries.PagedResult
import com.koalagroup.koala.domain.queries.UserQuery

interface UserRepository {
    fun create(user: User): User
    fun search(query: UserQuery): PagedResult<User>
}