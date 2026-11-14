package com.koalagroup.koala.api.dto

import com.koalagroup.koala.domain.queries.PagedResult

data class PagedResponse<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

fun <T, R> PagedResult<T>.toResponse(transform: (T) -> R): PagedResponse<R> {
    return PagedResponse(
        items = this.items.map(transform),
        page = this.page,
        size = this.size,
        totalElements = this.totalElements,
        totalPages = this.totalPages
    )
}
