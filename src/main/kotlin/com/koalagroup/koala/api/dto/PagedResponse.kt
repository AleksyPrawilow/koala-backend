package com.koalagroup.koala.api.dto

import com.koalagroup.koala.domain.queries.PagedResult
import io.swagger.v3.oas.annotations.media.Schema

data class PagedResponse<T>(
    @field:Schema(description = "Entries of the requested page.")
    val items: List<T>,
    @field:Schema(description = "Zero-based index of this page.")
    val page: Int,
    @field:Schema(description = "Requested page size.")
    val size: Int,
    @field:Schema(description = "Number of entries matching the filters across all pages.")
    val totalElements: Long,
    @field:Schema(description = "Number of pages for the requested page size.")
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
