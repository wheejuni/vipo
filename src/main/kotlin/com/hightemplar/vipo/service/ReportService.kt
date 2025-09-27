package com.hightemplar.vipo.service

import com.hightemplar.vipo.entity.Conversation
import com.hightemplar.vipo.repository.ConversationRepository
import org.springframework.stereotype.Service
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Service for generating reports in various formats
 */
@Service
class ReportService(
    private val conversationRepository: ConversationRepository
) {
    
    companion object {
        private val CSV_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        private const val CSV_HEADER = "ID,Question,Answer,Model,Streaming,Author Name,Author Email,Thread ID,Created At"
        private const val CSV_DELIMITER = ","
        private const val CSV_QUOTE = "\""
        private const val CSV_NEWLINE = "\n"
    }
    
    /**
     * Generate CSV report of conversations from the last 24 hours
     * @return CSV content as string
     */
    fun generateConversationReport24Hours(): String {
        val since24HoursAgo = LocalDateTime.now().minusHours(24)
        val now = LocalDateTime.now()
        return generateConversationReport(since24HoursAgo, now)
    }
    
    /**
     * Generate CSV report of conversations within a custom time range
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @return CSV content as string
     */
    fun generateConversationReport(startTime: LocalDateTime, endTime: LocalDateTime): String {
        val conversations = conversationRepository.findByCreatedAtBetweenWithUser(startTime, endTime)
        return generateConversationCsv(conversations)
    }
    
    /**
     * Generate CSV content from a list of conversations
     * @param conversations list of conversations to include in the report
     * @return CSV content as string
     */
    private fun generateConversationCsv(conversations: List<Conversation>): String {
        val writer = StringWriter()
        
        // Write CSV header
        writer.append(CSV_HEADER)
        writer.append(CSV_NEWLINE)
        
        // Write conversation data
        conversations.forEach { conversation ->
            writer.append(formatConversationRow(conversation))
            writer.append(CSV_NEWLINE)
        }
        
        return writer.toString()
    }
    
    /**
     * Format a single conversation as a CSV row
     * @param conversation the conversation to format
     * @return formatted CSV row
     */
    private fun formatConversationRow(conversation: Conversation): String {
        val user = conversation.thread.user
        
        return listOf(
            conversation.id.toString(),
            escapeCsvField(conversation.question),
            escapeCsvField(conversation.answer),
            escapeCsvField(conversation.model),
            conversation.isStreaming.toString(),
            escapeCsvField(user.name),
            escapeCsvField(user.email),
            conversation.thread.id.toString(),
            conversation.createdAt.format(CSV_DATE_FORMATTER)
        ).joinToString(CSV_DELIMITER)
    }
    
    /**
     * Escape a field for CSV format
     * @param field the field to escape
     * @return escaped field
     */
    private fun escapeCsvField(field: String): String {
        // If field contains comma, quote, or newline, wrap in quotes and escape internal quotes
        return if (field.contains(CSV_DELIMITER) || field.contains(CSV_QUOTE) || field.contains("\n") || field.contains("\r")) {
            CSV_QUOTE + field.replace(CSV_QUOTE, CSV_QUOTE + CSV_QUOTE) + CSV_QUOTE
        } else {
            field
        }
    }
    
    /**
     * Get conversation report statistics
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @return report statistics
     */
    fun getConversationReportStatistics(startTime: LocalDateTime, endTime: LocalDateTime): ConversationReportStatistics {
        val conversations = conversationRepository.findByCreatedAtBetweenWithUser(startTime, endTime)
        
        val totalConversations = conversations.size
        val uniqueUsers = conversations.map { it.thread.user.id }.distinct().size
        val uniqueThreads = conversations.map { it.thread.id }.distinct().size
        val modelUsage = conversations.groupBy { it.model }.mapValues { it.value.size }
        val streamingCount = conversations.count { it.isStreaming }
        
        return ConversationReportStatistics(
            totalConversations = totalConversations,
            uniqueUsers = uniqueUsers,
            uniqueThreads = uniqueThreads,
            modelUsage = modelUsage,
            streamingConversations = streamingCount,
            nonStreamingConversations = totalConversations - streamingCount,
            timeRange = "From $startTime to $endTime"
        )
    }
    
    /**
     * Get 24-hour conversation report statistics
     * @return report statistics for the last 24 hours
     */
    fun get24HourConversationReportStatistics(): ConversationReportStatistics {
        val since24HoursAgo = LocalDateTime.now().minusHours(24)
        val now = LocalDateTime.now()
        return getConversationReportStatistics(since24HoursAgo, now)
    }
    
    /**
     * Generate conversation report with metadata
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @return report with CSV content and metadata
     */
    fun generateConversationReportWithMetadata(startTime: LocalDateTime, endTime: LocalDateTime): ConversationReport {
        val csvContent = generateConversationReport(startTime, endTime)
        val statistics = getConversationReportStatistics(startTime, endTime)
        
        return ConversationReport(
            csvContent = csvContent,
            statistics = statistics,
            generatedAt = LocalDateTime.now()
        )
    }
    
    /**
     * Generate 24-hour conversation report with metadata
     * @return report with CSV content and metadata for the last 24 hours
     */
    fun generate24HourConversationReportWithMetadata(): ConversationReport {
        val since24HoursAgo = LocalDateTime.now().minusHours(24)
        val now = LocalDateTime.now()
        return generateConversationReportWithMetadata(since24HoursAgo, now)
    }
}

/**
 * Data class representing conversation report statistics
 */
data class ConversationReportStatistics(
    val totalConversations: Int,
    val uniqueUsers: Int,
    val uniqueThreads: Int,
    val modelUsage: Map<String, Int>,
    val streamingConversations: Int,
    val nonStreamingConversations: Int,
    val timeRange: String
)

/**
 * Data class representing a complete conversation report
 */
data class ConversationReport(
    val csvContent: String,
    val statistics: ConversationReportStatistics,
    val generatedAt: LocalDateTime
)