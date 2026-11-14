package com.koalagroup.koala.domain.entities

data class Role(
    val id: Long,
    val name: String
) {
    companion object {
        const val ADMIN = "ADMIN"
    }
}