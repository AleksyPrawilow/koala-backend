package com.koalagroup.koala.domain.repositories

import com.koalagroup.koala.domain.entities.User
import com.koalagroup.koala.domain.entities.UserStatus
import com.koalagroup.koala.domain.queries.PagedResult
import com.koalagroup.koala.domain.queries.UserQuery
import java.util.UUID

interface UserRepository {
    fun create(user: User): User
    fun findByEmail(email: String): User?
    fun search(query: UserQuery): PagedResult<User>
    fun findById(id: UUID): User?
    fun update(user: User): User
    fun countByRoleAndStatus(roleName: String, status: UserStatus): Long
}