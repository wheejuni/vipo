package com.hightemplar.vipo.dto

/**
 * Response DTO for activity metrics
 */
data class ActivityMetricsResponse(
    val registrationCount: Long,
    val loginCount: Long,
    val conversationCount: Long,
    val uniqueActiveUsers: Long,
    val timeRange: String
)