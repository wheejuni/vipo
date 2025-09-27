package com.hightemplar.vipo.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.hightemplar.vipo.service.*
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@WebMvcTest(AnalyticsController::class)
class AnalyticsControllerTest {
    
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    @MockBean
    private lateinit var analyticsService: AnalyticsService
    
    @MockBean
    private lateinit var reportService: ReportService
    
    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `getActivityMetrics should return activity metrics for admin`() {
        // Given
        val metrics = ActivityMetrics(
            registrationCount = 5L,
            loginCount = 15L,
            conversationCount = 25L,
            timeRange = "Last 24 hours"
        )
        val uniqueActiveUsers = 10L
        
        given(analyticsService.get24HourActivityMetrics()).willReturn(metrics)
        given(analyticsService.countUniqueActiveUsers()).willReturn(uniqueActiveUsers)
        
        // When & Then
        mockMvc.perform(get("/api/analytics/activity"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.registrationCount").value(5))
            .andExpect(jsonPath("$.loginCount").value(15))
            .andExpect(jsonPath("$.conversationCount").value(25))
            .andExpect(jsonPath("$.uniqueActiveUsers").value(10))
            .andExpect(jsonPath("$.timeRange").value("Last 24 hours"))
    }
    
    @Test
    @WithMockUser(roles = ["MEMBER"])
    fun `getActivityMetrics should return 403 for non-admin user`() {
        // When & Then
        mockMvc.perform(get("/api/analytics/activity"))
            .andExpect(status().isForbidden)
    }
    
    @Test
    fun `getActivityMetrics should return 401 for unauthenticated user`() {
        // When & Then
        mockMvc.perform(get("/api/analytics/activity"))
            .andExpect(status().isUnauthorized)
    }
    
    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `getCustomActivityMetrics should return metrics for valid time range`() {
        // Given
        val startTime = "2024-01-15T00:00:00"
        val endTime = "2024-01-16T00:00:00"
        val metrics = ActivityMetrics(
            registrationCount = 3L,
            loginCount = 8L,
            conversationCount = 12L,
            timeRange = "Custom range"
        )
        val uniqueActiveUsers = 5L
        
        given(analyticsService.getActivityMetrics(any(), any())).willReturn(metrics)
        given(analyticsService.countUniqueActiveUsers()).willReturn(uniqueActiveUsers)
        
        // When & Then
        mockMvc.perform(get("/api/analytics/activity/custom")
            .param("startTime", startTime)
            .param("endTime", endTime))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.registrationCount").value(3))
            .andExpect(jsonPath("$.loginCount").value(8))
            .andExpect(jsonPath("$.conversationCount").value(12))
            .andExpect(jsonPath("$.uniqueActiveUsers").value(5))
    }
    
    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `getCustomActivityMetrics should return 400 for invalid time range`() {
        // Given
        val startTime = "2024-01-16T00:00:00"
        val endTime = "2024-01-15T00:00:00" // End before start
        
        // When & Then
        mockMvc.perform(get("/api/analytics/activity/custom")
            .param("startTime", startTime)
            .param("endTime", endTime))
            .andExpect(status().isBadRequest)
    }
    
    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `getCustomActivityMetrics should return 400 for invalid date format`() {
        // Given
        val startTime = "invalid-date"
        val endTime = "2024-01-16T00:00:00"
        
        // When & Then
        mockMvc.perform(get("/api/analytics/activity/custom")
            .param("startTime", startTime)
            .param("endTime", endTime))
            .andExpect(status().isBadRequest)
    }
    
    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `getConversationReportStatistics should return report statistics`() {
        // Given
        val statistics = ConversationReportStatistics(
            totalConversations = 50,
            uniqueUsers = 10,
            uniqueThreads = 25,
            modelUsage = mapOf("gpt-4" to 30, "gpt-3.5-turbo" to 20),
            streamingConversations = 15,
            nonStreamingConversations = 35,
            timeRange = "Last 24 hours"
        )
        val report = ConversationReport(
            csvContent = "csv content",
            statistics = statistics,
            generatedAt = LocalDateTime.now()
        )
        
        given(reportService.generate24HourConversationReportWithMetadata()).willReturn(report)
        
        // When & Then
        mockMvc.perform(get("/api/analytics/reports/conversations/statistics"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.statistics.totalConversations").value(50))
            .andExpect(jsonPath("$.statistics.uniqueUsers").value(10))
            .andExpect(jsonPath("$.statistics.uniqueThreads").value(25))
            .andExpect(jsonPath("$.statistics.modelUsage.gpt-4").value(30))
            .andExpect(jsonPath("$.statistics.modelUsage.gpt-3\\.5-turbo").value(20))
            .andExpect(jsonPath("$.statistics.streamingConversations").value(15))
            .andExpect(jsonPath("$.statistics.nonStreamingConversations").value(35))
            .andExpect(jsonPath("$.csvDownloadUrl").value("/api/analytics/reports/conversations/csv"))
    }
    
    @Test
    @WithMockUser(roles = ["MEMBER"])
    fun `getConversationReportStatistics should return 403 for non-admin user`() {
        // When & Then
        mockMvc.perform(get("/api/analytics/reports/conversations/statistics"))
            .andExpect(status().isForbidden)
    }
    
    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `downloadConversationReportCsv should return CSV content`() {
        // Given
        val csvContent = "ID,Question,Answer,Model,Streaming,Author Name,Author Email,Thread ID,Created At\n1,Test,Response,gpt-4,false,User,user@test.com,1,2024-01-15 10:00:00"
        
        given(reportService.generateConversationReport24Hours()).willReturn(csvContent)
        
        // When & Then
        mockMvc.perform(get("/api/analytics/reports/conversations/csv"))
            .andExpect(status().isOk)
            .andExpect(content().contentType("text/csv"))
            .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"conversations_report_24h.csv\""))
            .andExpect(content().string(csvContent))
    }
    
    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `downloadCustomConversationReportCsv should return CSV content for valid time range`() {
        // Given
        val startTime = "2024-01-15T00:00:00"
        val endTime = "2024-01-16T00:00:00"
        val csvContent = "ID,Question,Answer,Model,Streaming,Author Name,Author Email,Thread ID,Created At\n1,Test,Response,gpt-4,false,User,user@test.com,1,2024-01-15 10:00:00"
        
        given(reportService.generateConversationReport(any(), any())).willReturn(csvContent)
        
        // When & Then
        mockMvc.perform(get("/api/analytics/reports/conversations/csv/custom")
            .param("startTime", startTime)
            .param("endTime", endTime))
            .andExpect(status().isOk)
            .andExpect(content().contentType("text/csv"))
            .andExpect(content().string(csvContent))
    }
    
    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `downloadCustomConversationReportCsv should return 400 for invalid time range`() {
        // Given
        val startTime = "2024-01-16T00:00:00"
        val endTime = "2024-01-15T00:00:00" // End before start
        
        // When & Then
        mockMvc.perform(get("/api/analytics/reports/conversations/csv/custom")
            .param("startTime", startTime)
            .param("endTime", endTime))
            .andExpect(status().isBadRequest)
    }
    
    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `getCustomConversationReportStatistics should return statistics for valid time range`() {
        // Given
        val startTime = "2024-01-15T00:00:00"
        val endTime = "2024-01-16T00:00:00"
        val statistics = ConversationReportStatistics(
            totalConversations = 25,
            uniqueUsers = 5,
            uniqueThreads = 12,
            modelUsage = mapOf("gpt-4" to 15, "gpt-3.5-turbo" to 10),
            streamingConversations = 8,
            nonStreamingConversations = 17,
            timeRange = "Custom range"
        )
        val report = ConversationReport(
            csvContent = "csv content",
            statistics = statistics,
            generatedAt = LocalDateTime.now()
        )
        
        given(reportService.generateConversationReportWithMetadata(any(), any())).willReturn(report)
        
        // When & Then
        mockMvc.perform(get("/api/analytics/reports/conversations/statistics/custom")
            .param("startTime", startTime)
            .param("endTime", endTime))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.statistics.totalConversations").value(25))
            .andExpect(jsonPath("$.statistics.uniqueUsers").value(5))
            .andExpect(jsonPath("$.statistics.uniqueThreads").value(12))
    }
    
    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `getCustomConversationReportStatistics should return 400 for invalid time range`() {
        // Given
        val startTime = "2024-01-16T00:00:00"
        val endTime = "2024-01-15T00:00:00" // End before start
        
        // When & Then
        mockMvc.perform(get("/api/analytics/reports/conversations/statistics/custom")
            .param("startTime", startTime)
            .param("endTime", endTime))
            .andExpect(status().isBadRequest)
    }
    
    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `healthCheck should return health status`() {
        // When & Then
        mockMvc.perform(get("/api/analytics/health"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("healthy"))
            .andExpect(jsonPath("$.service").value("analytics"))
            .andExpect(jsonPath("$.timestamp").exists())
    }
    
    @Test
    @WithMockUser(roles = ["MEMBER"])
    fun `healthCheck should return 403 for non-admin user`() {
        // When & Then
        mockMvc.perform(get("/api/analytics/health"))
            .andExpect(status().isForbidden)
    }
}