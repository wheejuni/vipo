package com.hightemplar.vipo.service

import com.hightemplar.vipo.entity.ActivityLog
import com.hightemplar.vipo.entity.ActivityType
import com.hightemplar.vipo.entity.User
import com.hightemplar.vipo.entity.UserRole
import com.hightemplar.vipo.repository.ActivityLogRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class AnalyticsServiceTest {
    
    private lateinit var activityLogRepository: ActivityLogRepository
    private lateinit var analyticsService: AnalyticsService
    
    private lateinit var testUser: User
    
    @BeforeEach
    fun setUp() {
        activityLogRepository = mockk()
        analyticsService = AnalyticsService(activityLogRepository)
        
        testUser = User(
            email = "test@example.com",
            password = "hashedPassword",
            name = "Test User",
            role = UserRole.MEMBER
        )
    }
    
    @Test
    fun `get24HourActivityMetrics should return correct metrics`() {
        // Given
        val registrationCount = 5L
        val loginCount = 15L
        val conversationCount = 25L
        
        every { 
            activityLogRepository.countByActivityTypeAndCreatedAtAfter(
                ActivityType.USER_REGISTRATION, 
                any()
            ) 
        } returns registrationCount
        
        every { 
            activityLogRepository.countByActivityTypeAndCreatedAtAfter(
                ActivityType.USER_LOGIN, 
                any()
            ) 
        } returns loginCount
        
        every { 
            activityLogRepository.countByActivityTypeAndCreatedAtAfter(
                ActivityType.CONVERSATION_CREATED, 
                any()
            ) 
        } returns conversationCount
        
        // When
        val result = analyticsService.get24HourActivityMetrics()
        
        // Then
        assertEquals(registrationCount, result.registrationCount)
        assertEquals(loginCount, result.loginCount)
        assertEquals(conversationCount, result.conversationCount)
        assertEquals("Last 24 hours", result.timeRange)
        
        verify { 
            activityLogRepository.countByActivityTypeAndCreatedAtAfter(
                ActivityType.USER_REGISTRATION, 
                any()
            ) 
        }
        verify { 
            activityLogRepository.countByActivityTypeAndCreatedAtAfter(
                ActivityType.USER_LOGIN, 
                any()
            ) 
        }
        verify { 
            activityLogRepository.countByActivityTypeAndCreatedAtAfter(
                ActivityType.CONVERSATION_CREATED, 
                any()
            ) 
        }
    }
    
    @Test
    fun `getActivityMetrics should return correct metrics for custom time range`() {
        // Given
        val startTime = LocalDateTime.now().minusDays(7)
        val endTime = LocalDateTime.now()
        val registrationCount = 10L
        val loginCount = 30L
        val conversationCount = 50L
        
        every { 
            activityLogRepository.countByActivityTypeAndCreatedAtBetween(
                ActivityType.USER_REGISTRATION, 
                startTime, 
                endTime
            ) 
        } returns registrationCount
        
        every { 
            activityLogRepository.countByActivityTypeAndCreatedAtBetween(
                ActivityType.USER_LOGIN, 
                startTime, 
                endTime
            ) 
        } returns loginCount
        
        every { 
            activityLogRepository.countByActivityTypeAndCreatedAtBetween(
                ActivityType.CONVERSATION_CREATED, 
                startTime, 
                endTime
            ) 
        } returns conversationCount
        
        // When
        val result = analyticsService.getActivityMetrics(startTime, endTime)
        
        // Then
        assertEquals(registrationCount, result.registrationCount)
        assertEquals(loginCount, result.loginCount)
        assertEquals(conversationCount, result.conversationCount)
        assertTrue(result.timeRange.contains("Custom range"))
        
        verify { 
            activityLogRepository.countByActivityTypeAndCreatedAtBetween(
                ActivityType.USER_REGISTRATION, 
                startTime, 
                endTime
            ) 
        }
        verify { 
            activityLogRepository.countByActivityTypeAndCreatedAtBetween(
                ActivityType.USER_LOGIN, 
                startTime, 
                endTime
            ) 
        }
        verify { 
            activityLogRepository.countByActivityTypeAndCreatedAtBetween(
                ActivityType.CONVERSATION_CREATED, 
                startTime, 
                endTime
            ) 
        }
    }
    
    @Test
    fun `logUserRegistration should save activity log with correct type`() {
        // Given
        val activityLogSlot = slot<ActivityLog>()
        every { activityLogRepository.save(capture(activityLogSlot)) } returns mockk()
        
        // When
        analyticsService.logUserRegistration(testUser)
        
        // Then
        verify { activityLogRepository.save(any()) }
        val capturedLog = activityLogSlot.captured
        assertEquals(testUser, capturedLog.user)
        assertEquals(ActivityType.USER_REGISTRATION, capturedLog.activityType)
    }
    
    @Test
    fun `logUserLogin should save activity log with correct type`() {
        // Given
        val activityLogSlot = slot<ActivityLog>()
        every { activityLogRepository.save(capture(activityLogSlot)) } returns mockk()
        
        // When
        analyticsService.logUserLogin(testUser)
        
        // Then
        verify { activityLogRepository.save(any()) }
        val capturedLog = activityLogSlot.captured
        assertEquals(testUser, capturedLog.user)
        assertEquals(ActivityType.USER_LOGIN, capturedLog.activityType)
    }
    
    @Test
    fun `logConversationCreated should save activity log with correct type`() {
        // Given
        val activityLogSlot = slot<ActivityLog>()
        every { activityLogRepository.save(capture(activityLogSlot)) } returns mockk()
        
        // When
        analyticsService.logConversationCreated(testUser)
        
        // Then
        verify { activityLogRepository.save(any()) }
        val capturedLog = activityLogSlot.captured
        assertEquals(testUser, capturedLog.user)
        assertEquals(ActivityType.CONVERSATION_CREATED, capturedLog.activityType)
    }
    
    @Test
    fun `getDetailedActivityStatistics should return correct statistics map`() {
        // Given
        val statisticsData = listOf(
            arrayOf<Any>(ActivityType.USER_REGISTRATION, 5L),
            arrayOf<Any>(ActivityType.USER_LOGIN, 15L),
            arrayOf<Any>(ActivityType.CONVERSATION_CREATED, 25L)
        )
        
        every { activityLogRepository.getActivityStatistics(any()) } returns statisticsData
        
        // When
        val result = analyticsService.getDetailedActivityStatistics()
        
        // Then
        assertEquals(3, result.size)
        assertEquals(5L, result[ActivityType.USER_REGISTRATION])
        assertEquals(15L, result[ActivityType.USER_LOGIN])
        assertEquals(25L, result[ActivityType.CONVERSATION_CREATED])
        
        verify { activityLogRepository.getActivityStatistics(any()) }
    }
    
    @Test
    fun `countUniqueActiveUsers should return correct count`() {
        // Given
        val expectedCount = 42L
        every { 
            activityLogRepository.countUniqueUsersWithActivity(any(), any()) 
        } returns expectedCount
        
        // When
        val result = analyticsService.countUniqueActiveUsers()
        
        // Then
        assertEquals(expectedCount, result)
        verify { activityLogRepository.countUniqueUsersWithActivity(any(), any()) }
    }
    
    @Test
    fun `getRecentActivities should return activities with default limit`() {
        // Given
        val expectedActivities = listOf(
            ActivityLog(user = testUser, activityType = ActivityType.USER_LOGIN),
            ActivityLog(user = testUser, activityType = ActivityType.CONVERSATION_CREATED)
        )
        every { activityLogRepository.findRecentActivities(50) } returns expectedActivities
        
        // When
        val result = analyticsService.getRecentActivities()
        
        // Then
        assertEquals(expectedActivities, result)
        verify { activityLogRepository.findRecentActivities(50) }
    }
    
    @Test
    fun `getRecentActivities should return activities with custom limit`() {
        // Given
        val customLimit = 10
        val expectedActivities = listOf(
            ActivityLog(user = testUser, activityType = ActivityType.USER_LOGIN)
        )
        every { activityLogRepository.findRecentActivities(customLimit) } returns expectedActivities
        
        // When
        val result = analyticsService.getRecentActivities(customLimit)
        
        // Then
        assertEquals(expectedActivities, result)
        verify { activityLogRepository.findRecentActivities(customLimit) }
    }
    
    @Test
    fun `getDetailedActivityStatistics should handle empty statistics`() {
        // Given
        every { activityLogRepository.getActivityStatistics(any()) } returns emptyList()
        
        // When
        val result = analyticsService.getDetailedActivityStatistics()
        
        // Then
        assertTrue(result.isEmpty())
        verify { activityLogRepository.getActivityStatistics(any()) }
    }
}