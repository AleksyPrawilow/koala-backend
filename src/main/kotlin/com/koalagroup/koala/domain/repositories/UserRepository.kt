package com.koalagroup.koala.domain.repositories

import com.koalagroup.koala.domain.entities.User

interface UserRepository {
    fun create(user: User): User
}