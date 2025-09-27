package com.hightemplar.vipo.dto

import org.springframework.data.domain.Page

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean,
    val hasNext: Boolean,
    val hasPrevious: Boolean
) {
    companion object {
        fun <T, R> fromPage(page: Page<T>, mapper: (T) -> R): PageResponse<R> {
            return PageResponse(
                content = page.content.map(mapper),
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                first = page.isFirst,
                last = page.isLast,
                hasNext = page.hasNext(),
                hasPrevious = page.hasPrevious()
            )
        }
    }
}