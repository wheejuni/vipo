package com.hightemplar.vipo.controller

import com.hightemplar.vipo.dto.ActivityMetricsResponse
import com.hightemplar.vipo.dto.ConversationReportResponse
import com.hightemplar.vipo.dto.ConversationReportStatisticsResponse
import com.hightemplar.vipo.service.AnalyticsService
import com.hightemplar.vipo.service.ReportService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * REST controller for analytics and reporting endpoints
 * All endpoints require ADMIN role
 */
@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasRole('ADMIN')")
class AnalyticsController(
    private val analyticsService: AnalyticsService,
    private val reportService: ReportService
) {
    
    /**
     * Get activity metrics for the last 24 hours
     * @return activity metrics including registration, login, and conversation counts
     */
    @GetMapping("/activity")
    fun getActivityMetrics(): ResponseEntity<ActivityMetricsResponse> {
        val metrics = analyticsService.get24HourActivityMetrics()
        val uniqueActiveUsers = analyticsService.countUniqueActiveUsers()
        
        val response = ActivityMetricsResponse(
            registrationCount = metrics.registrationCount,
            loginCount = metrics.loginCount,
            conversationCount = metrics.conversationCount,
            uniqueActiveUsers = uniqueActiveUsers,
            timeRange = metrics.timeRange
        )
        
        return ResponseEntity.ok(response)
    }
    
    /**
     * Get activity metrics for a custom time range
     * @param startTime start of the time range (ISO format: yyyy-MM-ddTHH:mm:ss)
     * @param endTime end of the time range (ISO format: yyyy-MM-ddTHH:mm:ss)
     * @return activity metrics for the specified time range
     */
    @GetMapping("/activity/custom")
    fun getCustomActivityMetrics(
        @RequestParam startTime: String,
        @RequestParam endTime: String
    ): ResponseEntity<ActivityMetricsResponse> {
        try {
            val start = LocalDateTime.parse(startTime)
            val end = LocalDateTime.parse(endTime)
            
            if (start.isAfter(end)) {
                return ResponseEntity.badRequest().build()
            }
            
            val metrics = analyticsService.getActivityMetrics(start, end)
            val uniqueActiveUsers = analyticsService.countUniqueActiveUsers()
            
            val response = ActivityMetricsResponse(
                registrationCount = metrics.registrationCount,
                loginCount = metrics.loginCount,
                conversationCount = metrics.conversationCount,
                uniqueActiveUsers = uniqueActiveUsers,
                timeRange = metrics.timeRange
            )
            
            return ResponseEntity.ok(response)
        } catch (e: Exception) {
            return ResponseEntity.badRequest().build()
        }
    }
    
    /**
     * Get conversation report statistics for the last 24 hours
     * @return conversation report statistics
     */
    @GetMapping("/reports/conversations/statistics")
    fun getConversationReportStatistics(): ResponseEntity<ConversationReportResponse> {
        val report = reportService.generate24HourConversationReportWithMetadata()
        
        val statisticsResponse = ConversationReportStatisticsResponse(
            totalConversations = report.statistics.totalConversations,
            uniqueUsers = report.statistics.uniqueUsers,
            uniqueThreads = report.statistics.uniqueThreads,
            modelUsage = report.statistics.modelUsage,
            streamingConversations = report.statistics.streamingConversations,
            nonStreamingConversations = report.statistics.nonStreamingConversations,
            timeRange = report.statistics.timeRange
        )
        
        val response = ConversationReportResponse(
            statistics = statisticsResponse,
            generatedAt = report.generatedAt,
            csvDownloadUrl = "/api/analytics/reports/conversations/csv"
        )
        
        return ResponseEntity.ok(response)
    }
    
    /**
     * Download conversation report as CSV for the last 24 hours
     * @return CSV file content
     */
    @GetMapping("/reports/conversations/csv")
    fun downloadConversationReportCsv(): ResponseEntity<String> {
        val csvContent = reportService.generateConversationReport24Hours()
        
        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType("text/csv")
        headers.setContentDispositionFormData("attachment", "conversations_report_24h.csv")
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(csvContent)
    }
    
    /**
     * Download conversation report as CSV for a custom time range
     * @param startTime start of the time range (ISO format: yyyy-MM-ddTHH:mm:ss)
     * @param endTime end of the time range (ISO format: yyyy-MM-ddTHH:mm:ss)
     * @return CSV file content
     */
    @GetMapping("/reports/conversations/csv/custom")
    fun downloadCustomConversationReportCsv(
        @RequestParam startTime: String,
        @RequestParam endTime: String
    ): ResponseEntity<String> {
        try {
            val start = LocalDateTime.parse(startTime)
            val end = LocalDateTime.parse(endTime)
            
            if (start.isAfter(end)) {
                return ResponseEntity.badRequest().build()
            }
            
            val csvContent = reportService.generateConversationReport(start, end)
            
            val headers = HttpHeaders()
            headers.contentType = MediaType.parseMediaType("text/csv")
            val filename = "conversations_report_${start.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}_${end.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.csv"
            headers.setContentDispositionFormData("attachment", filename)
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(csvContent)
        } catch (e: Exception) {
            return ResponseEntity.badRequest().build()
        }
    }
    
    /**
     * Get conversation report statistics for a custom time range
     * @param startTime start of the time range (ISO format: yyyy-MM-ddTHH:mm:ss)
     * @param endTime end of the time range (ISO format: yyyy-MM-ddTHH:mm:ss)
     * @return conversation report statistics for the specified time range
     */
    @GetMapping("/reports/conversations/statistics/custom")
    fun getCustomConversationReportStatistics(
        @RequestParam startTime: String,
        @RequestParam endTime: String
    ): ResponseEntity<ConversationReportResponse> {
        try {
            val start = LocalDateTime.parse(startTime)
            val end = LocalDateTime.parse(endTime)
            
            if (start.isAfter(end)) {
                return ResponseEntity.badRequest().build()
            }
            
            val report = reportService.generateConversationReportWithMetadata(start, end)
            
            val statisticsResponse = ConversationReportStatisticsResponse(
                totalConversations = report.statistics.totalConversations,
                uniqueUsers = report.statistics.uniqueUsers,
                uniqueThreads = report.statistics.uniqueThreads,
                modelUsage = report.statistics.modelUsage,
                streamingConversations = report.statistics.streamingConversations,
                nonStreamingConversations = report.statistics.nonStreamingConversations,
                timeRange = report.statistics.timeRange
            )
            
            val response = ConversationReportResponse(
                statistics = statisticsResponse,
                generatedAt = report.generatedAt,
                csvDownloadUrl = "/api/analytics/reports/conversations/csv/custom?startTime=$startTime&endTime=$endTime"
            )
            
            return ResponseEntity.ok(response)
        } catch (e: Exception) {
            return ResponseEntity.badRequest().build()
        }
    }
    
    /**
     * Health check endpoint for analytics service
     * @return simple health status
     */
    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf(
            "status" to "healthy",
            "service" to "analytics",
            "timestamp" to LocalDateTime.now().toString()
        ))
    }
}