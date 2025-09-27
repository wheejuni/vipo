package com.hightemplar.vipo.service

import com.hightemplar.vipo.entity.ActivityLog
import com.hightemplar.vipo.entity.ActivityType
import com.hightemplar.vipo.entity.User
import com.hightemplar.vipo.repository.ActivityLogRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Service for analytics and activity tracking operations
 */
@Service
class AnalyticsService(
    private val activityLogRepository: ActivityLogRepository
) {
    
    /**
     * Get activity metrics for the last 24 hours
     * @return ActivityMetrics containing counts for different activity types
     */
    fun get24HourActivityMetrics(): ActivityMetrics {
        val since24HoursAgo = LocalDateTime.now().minusHours(24)
        
        val registrationCount = activityLogRepository.countByActivityTypeAndCreatedAtAfter(
            ActivityType.USER_REGISTRATION, since24HoursAgo
        )
        
        val loginCount = activityLogRepository.countByActivityTypeAndCreatedAtAfter(
            ActivityType.USER_LOGIN, since24HoursAgo
        )
        
        val conversationCount = activityLogRepository.countByActivityTypeAndCreatedAtAfter(
            ActivityType.CONVERSATION_CREATED, since24HoursAgo
        )
        
        return ActivityMetrics(
            registrationCount = registrationCount,
            loginCount = loginCount,
            conversationCount = conversationCount,
            timeRange = "Last 24 hours"
        )
    }
    
    /**
     * Get activity metrics for a custom time range
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @return ActivityMetrics containing counts for different activity types
     */
    fun getActivityMetrics(startTime: LocalDateTime, endTime: LocalDateTime): ActivityMetrics {
        val registrationCount = activityLogRepository.countByActivityTypeAndCreatedAtBetween(
            ActivityType.USER_REGISTRATION, startTime, endTime
        )
        
        val loginCount = activityLogRepository.countByActivityTypeAndCreatedAtBetween(
            ActivityType.USER_LOGIN, startTime, endTime
        )
        
        val conversationCount = activityLogRepository.countByActivityTypeAndCreatedAtBetween(
            ActivityType.CONVERSATION_CREATED, startTime, endTime
        )
        
        return ActivityMetrics(
            registrationCount = registrationCount,
            loginCount = loginCount,
            conversationCount = conversationCount,
            timeRange = "Custom range: $startTime to $endTime"
        )
    }
    
    /**
     * Log user registration activity
     * @param user the user who registered
     */
    fun logUserRegistration(user: User) {
        val activityLog = ActivityLog(
            user = user,
            activityType = ActivityType.USER_REGISTRATION
        )
        activityLogRepository.save(activityLog)
    }
    
    /**
     * Log user login activity
     * @param user the user who logged in
     */
    fun logUserLogin(user: User) {
        val activityLog = ActivityLog(
            user = user,
            activityType = ActivityType.USER_LOGIN
        )
        activityLogRepository.save(activityLog)
    }
    
    /**
     * Log conversation creation activity
     * @param user the user who created the conversation
     */
    fun logConversationCreated(user: User) {
        val activityLog = ActivityLog(
            user = user,
            activityType = ActivityType.CONVERSATION_CREATED
        )
        activityLogRepository.save(activityLog)
    }
    
    /**
     * Get detailed activity statistics for the last 24 hours
     * @return map of activity types to their counts
     */
    fun getDetailedActivityStatistics(): Map<ActivityType, Long> {
        val since24HoursAgo = LocalDateTime.now().minusHours(24)
        val statistics = activityLogRepository.getActivityStatistics(since24HoursAgo)
        
        return statistics.associate { row ->
            val activityType = row[0] as ActivityType
            val count = row[1] as Long
            activityType to count
        }
    }
    
    /**
     * Count unique active users in the last 24 hours
     * @return count of unique users who performed any activity
     */
    fun countUniqueActiveUsers(): Long {
        val since24HoursAgo = LocalDateTime.now().minusHours(24)
        val now = LocalDateTime.now()
        return activityLogRepository.countUniqueUsersWithActivity(since24HoursAgo, now)
    }
    
    /**
     * Get recent activities with limit
     * @param limit maximum number of activities to return
     * @return list of recent activities
     */
    fun getRecentActivities(limit: Int = 50): List<ActivityLog> {
        return activityLogRepository.findRecentActivities(limit)
    }
}

/**
 * Data class representing activity metrics
 */
data class ActivityMetrics(
    val registrationCount: Long,
    val loginCount: Long,
    val conversationCount: Long,
    val timeRange: String
)