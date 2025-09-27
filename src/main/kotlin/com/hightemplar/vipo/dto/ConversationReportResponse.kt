package com.hightemplar.vipo.dto

import java.time.LocalDateTime

/**
 * Response DTO for conversation report statistics
 */
data class ConversationReportStatisticsResponse(
    val totalConversations: Int,
    val uniqueUsers: Int,
    val uniqueThreads: Int,
    val modelUsage: Map<String, Int>,
    val streamingConversations: Int,
    val nonStreamingConversations: Int,
    val timeRange: String
)

/**
 * Response DTO for conversation report with metadata
 */
data class ConversationReportResponse(
    val statistics: ConversationReportStatisticsResponse,
    val generatedAt: LocalDateTime,
    val csvDownloadUrl: String? = null
)