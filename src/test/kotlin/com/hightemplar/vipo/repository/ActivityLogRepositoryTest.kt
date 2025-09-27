package com.hightemplar.vipo.repository

import com.hightemplar.vipo.entity.ActivityLog
import com.hightemplar.vipo.entity.ActivityType
import com.hightemplar.vipo.entity.User
import com.hightemplar.vipo.entity.UserRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
class ActivityLogRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var activityLogRepository: ActivityLogRepository

    private lateinit var user1: User
    private lateinit var user2: User

    @BeforeEach
    fun setUp() {
        user1 = User(
            email = "user1@example.com",
            password = "password123",
            name = "User One",
            role = UserRole.MEMBER
        )
        user2 = User(
            email = "user2@example.com",
            password = "password123",
            name = "User Two",
            role = UserRole.ADMIN
        )
        
        entityManager.persistAndFlush(user1)
        entityManager.persistAndFlush(user2)
    }

    @Test
    fun `countByActivityTypeAndCreatedAtBetween should return correct count within time range`() {
        // Given
        val now = LocalDateTime.now()
        val startTime = now.minusHours(2)
        val endTime = now.plusHours(1)
        
        // Create activities within the time range
        repeat(3) {
            val activity = ActivityLog(
                user = user1,
                activityType = ActivityType.USER_LOGIN
            )
            entityManager.persistAndFlush(activity)
        }
        
        repeat(2) {
            val activity = ActivityLog(
                user = user1,
                activityType = ActivityType.CONVERSATION_CREATED
            )
            entityManager.persistAndFlush(activity)
        }

        // When
        val loginCount = activityLogRepository.countByActivityTypeAndCreatedAtBetween(
            ActivityType.USER_LOGIN, startTime, endTime
        )
        val conversationCount = activityLogRepository.countByActivityTypeAndCreatedAtBetween(
            ActivityType.CONVERSATION_CREATED, startTime, endTime
        )

        // Then
        assertThat(loginCount).isEqualTo(3)
        assertThat(conversationCount).isEqualTo(2)
    }

    @Test
    fun `countByActivityTypeAndCreatedAtAfter should return count after specific time`() {
        // Given
        val now = LocalDateTime.now()
        val since24HoursAgo = now.minusHours(24)
        
        repeat(4) {
            val activity = ActivityLog(
                user = user1,
                activityType = ActivityType.USER_REGISTRATION
            )
            entityManager.persistAndFlush(activity)
        }

        // When
        val count = activityLogRepository.countByActivityTypeAndCreatedAtAfter(
            ActivityType.USER_REGISTRATION, since24HoursAgo
        )

        // Then
        assertThat(count).isEqualTo(4)
    }

    @Test
    fun `findByCreatedAtBetween should return activities within time range`() {
        // Given
        val now = LocalDateTime.now()
        val startTime = now.minusHours(2)
        val endTime = now.plusHours(1)
        
        val activity1 = ActivityLog(user = user1, activityType = ActivityType.USER_LOGIN)
        val activity2 = ActivityLog(user = user2, activityType = ActivityType.CONVERSATION_CREATED)
        
        entityManager.persistAndFlush(activity1)
        entityManager.persistAndFlush(activity2)

        // When
        val activities = activityLogRepository.findByCreatedAtBetween(startTime, endTime)

        // Then
        assertThat(activities).hasSize(2)
        assertThat(activities).extracting("activityType").containsExactlyInAnyOrder(
            ActivityType.USER_LOGIN, ActivityType.CONVERSATION_CREATED
        )
    }

    @Test
    fun `findByActivityTypeAndCreatedAtBetween should return activities of specific type within time range`() {
        // Given
        val now = LocalDateTime.now()
        val startTime = now.minusHours(2)
        val endTime = now.plusHours(1)
        
        val loginActivity = ActivityLog(user = user1, activityType = ActivityType.USER_LOGIN)
        val conversationActivity = ActivityLog(user = user1, activityType = ActivityType.CONVERSATION_CREATED)
        val anotherLoginActivity = ActivityLog(user = user2, activityType = ActivityType.USER_LOGIN)
        
        entityManager.persistAndFlush(loginActivity)
        entityManager.persistAndFlush(conversationActivity)
        entityManager.persistAndFlush(anotherLoginActivity)

        // When
        val loginActivities = activityLogRepository.findByActivityTypeAndCreatedAtBetween(
            ActivityType.USER_LOGIN, startTime, endTime
        )

        // Then
        assertThat(loginActivities).hasSize(2)
        assertThat(loginActivities).allMatch { it.activityType == ActivityType.USER_LOGIN }
    }

    @Test
    fun `findByUserAndCreatedAtBetween should return activities for specific user within time range`() {
        // Given
        val now = LocalDateTime.now()
        val startTime = now.minusHours(2)
        val endTime = now.plusHours(1)
        
        val user1Activity1 = ActivityLog(user = user1, activityType = ActivityType.USER_LOGIN)
        val user1Activity2 = ActivityLog(user = user1, activityType = ActivityType.CONVERSATION_CREATED)
        val user2Activity = ActivityLog(user = user2, activityType = ActivityType.USER_LOGIN)
        
        entityManager.persistAndFlush(user1Activity1)
        entityManager.persistAndFlush(user1Activity2)
        entityManager.persistAndFlush(user2Activity)

        // When
        val user1Activities = activityLogRepository.findByUserAndCreatedAtBetween(user1, startTime, endTime)

        // Then
        assertThat(user1Activities).hasSize(2)
        assertThat(user1Activities).allMatch { it.user?.id == user1.id }
    }

    @Test
    fun `countByCreatedAtBetween should return total count within time range`() {
        // Given
        val now = LocalDateTime.now()
        val startTime = now.minusHours(2)
        val endTime = now.plusHours(1)
        
        repeat(5) { index ->
            val activity = ActivityLog(
                user = if (index % 2 == 0) user1 else user2,
                activityType = ActivityType.values()[index % ActivityType.values().size]
            )
            entityManager.persistAndFlush(activity)
        }

        // When
        val totalCount = activityLogRepository.countByCreatedAtBetween(startTime, endTime)

        // Then
        assertThat(totalCount).isEqualTo(5)
    }

    @Test
    fun `getActivityStatistics should return activity counts grouped by type`() {
        // Given
        val now = LocalDateTime.now()
        val since24HoursAgo = now.minusHours(24)
        
        repeat(3) {
            val activity = ActivityLog(user = user1, activityType = ActivityType.USER_LOGIN)
            entityManager.persistAndFlush(activity)
        }
        
        repeat(2) {
            val activity = ActivityLog(user = user1, activityType = ActivityType.CONVERSATION_CREATED)
            entityManager.persistAndFlush(activity)
        }
        
        repeat(1) {
            val activity = ActivityLog(user = user1, activityType = ActivityType.USER_REGISTRATION)
            entityManager.persistAndFlush(activity)
        }

        // When
        val statistics = activityLogRepository.getActivityStatistics(since24HoursAgo)

        // Then
        assertThat(statistics).hasSize(3)
        
        val statisticsMap = statistics.associate { 
            (it[0] as ActivityType) to (it[1] as Long) 
        }
        
        assertThat(statisticsMap[ActivityType.USER_LOGIN]).isEqualTo(3L)
        assertThat(statisticsMap[ActivityType.CONVERSATION_CREATED]).isEqualTo(2L)
        assertThat(statisticsMap[ActivityType.USER_REGISTRATION]).isEqualTo(1L)
    }

    @Test
    fun `countUniqueUsersWithActivity should return count of unique users with activities`() {
        // Given
        val now = LocalDateTime.now()
        val startTime = now.minusHours(2)
        val endTime = now.plusHours(1)
        
        // User1 has multiple activities
        repeat(3) {
            val activity = ActivityLog(user = user1, activityType = ActivityType.USER_LOGIN)
            entityManager.persistAndFlush(activity)
        }
        
        // User2 has one activity
        val activity = ActivityLog(user = user2, activityType = ActivityType.CONVERSATION_CREATED)
        entityManager.persistAndFlush(activity)
        
        // Activity without user (should not be counted)
        val anonymousActivity = ActivityLog(user = null, activityType = ActivityType.USER_REGISTRATION)
        entityManager.persistAndFlush(anonymousActivity)

        // When
        val uniqueUserCount = activityLogRepository.countUniqueUsersWithActivity(startTime, endTime)

        // Then
        assertThat(uniqueUserCount).isEqualTo(2L) // user1 and user2
    }

    @Test
    fun `should handle activities with null user`() {
        // Given
        val activityWithUser = ActivityLog(user = user1, activityType = ActivityType.USER_LOGIN)
        val activityWithoutUser = ActivityLog(user = null, activityType = ActivityType.USER_REGISTRATION)
        
        entityManager.persistAndFlush(activityWithUser)
        entityManager.persistAndFlush(activityWithoutUser)

        // When
        val now = LocalDateTime.now()
        val activities = activityLogRepository.findByCreatedAtBetween(
            now.minusHours(1), now.plusHours(1)
        )

        // Then
        assertThat(activities).hasSize(2)
        assertThat(activities).anyMatch { it.user != null }
        assertThat(activities).anyMatch { it.user == null }
    }
}