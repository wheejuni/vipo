package com.hightemplar.vipo.repository

import com.hightemplar.vipo.entity.ActivityLog
import com.hightemplar.vipo.entity.ActivityType
import com.hightemplar.vipo.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ActivityLogRepository : JpaRepository<ActivityLog, Long> {
    
    /**
     * Count activities by type within a specific time range
     * @param activityType the type of activity to count
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @return count of activities of the specified type within the time range
     */
    fun countByActivityTypeAndCreatedAtBetween(
        activityType: ActivityType,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Long
    
    /**
     * Count activities within the last 24 hours by type
     * @param activityType the type of activity to count
     * @param since24HoursAgo timestamp 24 hours ago
     * @return count of activities of the specified type in the last 24 hours
     */
    fun countByActivityTypeAndCreatedAtAfter(
        activityType: ActivityType,
        since24HoursAgo: LocalDateTime
    ): Long
    
    /**
     * Find all activities within a time range
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @return list of activities within the time range
     */
    fun findByCreatedAtBetween(startTime: LocalDateTime, endTime: LocalDateTime): List<ActivityLog>
    
    /**
     * Find activities by type within a time range
     * @param activityType the type of activity to find
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @return list of activities of the specified type within the time range
     */
    fun findByActivityTypeAndCreatedAtBetween(
        activityType: ActivityType,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<ActivityLog>
    
    /**
     * Find activities for a specific user within a time range
     * @param user the user to find activities for
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @return list of activities for the user within the time range
     */
    fun findByUserAndCreatedAtBetween(
        user: User,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<ActivityLog>
    
    /**
     * Count total activities within a time range
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @return total count of activities within the time range
     */
    fun countByCreatedAtBetween(startTime: LocalDateTime, endTime: LocalDateTime): Long
    
    /**
     * Get activity statistics for the last 24 hours
     * @param since24HoursAgo timestamp 24 hours ago
     * @return map of activity types to their counts
     */
    @Query("""
        SELECT a.activityType, COUNT(a) 
        FROM ActivityLog a 
        WHERE a.createdAt > :since24HoursAgo 
        GROUP BY a.activityType
    """)
    fun getActivityStatistics(@Param("since24HoursAgo") since24HoursAgo: LocalDateTime): List<Array<Any>>
    
    /**
     * Count unique users who performed activities within a time range
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @return count of unique users with activities in the time range
     */
    @Query("""
        SELECT COUNT(DISTINCT a.user) 
        FROM ActivityLog a 
        WHERE a.createdAt BETWEEN :startTime AND :endTime 
        AND a.user IS NOT NULL
    """)
    fun countUniqueUsersWithActivity(
        @Param("startTime") startTime: LocalDateTime,
        @Param("endTime") endTime: LocalDateTime
    ): Long
    
    /**
     * Find recent activities ordered by creation time
     * @param limit maximum number of activities to return
     * @return list of recent activities
     */
    @Query("""
        SELECT a FROM ActivityLog a 
        ORDER BY a.createdAt DESC 
        LIMIT :limit
    """)
    fun findRecentActivities(@Param("limit") limit: Int): List<ActivityLog>
    
    /**
     * Find activities by type
     * @param activityType the type of activity to find
     * @return list of activities of the specified type
     */
    fun findByActivityType(activityType: ActivityType): List<ActivityLog>
}