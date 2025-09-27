package com.hightemplar.vipo.repository

import com.hightemplar.vipo.entity.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
class FeedbackRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var feedbackRepository: FeedbackRepository

    private lateinit var user1: User
    private lateinit var user2: User
    private lateinit var thread1: Thread
    private lateinit var thread2: Thread
    private lateinit var conversation1: Conversation
    private lateinit var conversation2: Conversation

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
            role = UserRole.MEMBER
        )
        
        entityManager.persistAndFlush(user1)
        entityManager.persistAndFlush(user2)
        
        thread1 = Thread(user = user1, lastActivityAt = LocalDateTime.now())
        thread2 = Thread(user = user2, lastActivityAt = LocalDateTime.now())
        
        entityManager.persistAndFlush(thread1)
        entityManager.persistAndFlush(thread2)
        
        conversation1 = Conversation(
            thread = thread1,
            question = "Question 1",
            answer = "Answer 1",
            model = "gpt-3.5-turbo",
            isStreaming = false
        )
        conversation2 = Conversation(
            thread = thread2,
            question = "Question 2",
            answer = "Answer 2",
            model = "gpt-4",
            isStreaming = false
        )
        
        entityManager.persistAndFlush(conversation1)
        entityManager.persistAndFlush(conversation2)
    }

    @Test
    fun `findByUser should return feedback for specific user only`() {
        // Given
        val feedback1 = Feedback(
            user = user1,
            conversation = conversation1,
            isPositive = true,
            status = FeedbackStatus.PENDING
        )
        val feedback2 = Feedback(
            user = user1,
            conversation = conversation1,
            isPositive = false,
            status = FeedbackStatus.RESOLVED
        )
        val feedback3 = Feedback(
            user = user2,
            conversation = conversation2,
            isPositive = true,
            status = FeedbackStatus.PENDING
        )
        
        entityManager.persistAndFlush(feedback1)
        entityManager.persistAndFlush(feedback2)
        entityManager.persistAndFlush(feedback3)

        // When
        val pageable = PageRequest.of(0, 10)
        val user1Feedback = feedbackRepository.findByUser(user1, pageable)

        // Then
        assertThat(user1Feedback.content).hasSize(2)
        assertThat(user1Feedback.content).allMatch { it.user.id == user1.id }
    }

    @Test
    fun `findByUserOrderByCreatedAtDesc should return feedback ordered by creation time`() {
        // Given
        val feedback1 = Feedback(
            user = user1,
            conversation = conversation1,
            isPositive = true,
            status = FeedbackStatus.PENDING
        )
        val feedback2 = Feedback(
            user = user1,
            conversation = conversation1,
            isPositive = false,
            status = FeedbackStatus.RESOLVED
        )
        
        entityManager.persistAndFlush(feedback1)
        entityManager.persistAndFlush(feedback2)

        // When
        val pageable = PageRequest.of(0, 10)
        val feedback = feedbackRepository.findByUserOrderByCreatedAtDesc(user1, pageable)

        // Then
        assertThat(feedback.content).hasSize(2)
        assertThat(feedback.content[0].createdAt).isAfterOrEqualTo(feedback.content[1].createdAt)
    }

    @Test
    fun `findByStatus should return feedback with specific status`() {
        // Given
        val feedback1 = Feedback(
            user = user1,
            conversation = conversation1,
            isPositive = true,
            status = FeedbackStatus.PENDING
        )
        val feedback2 = Feedback(
            user = user2,
            conversation = conversation2,
            isPositive = false,
            status = FeedbackStatus.PENDING
        )
        val feedback3 = Feedback(
            user = user1,
            conversation = conversation1,
            isPositive = true,
            status = FeedbackStatus.RESOLVED
        )
        
        entityManager.persistAndFlush(feedback1)
        entityManager.persistAndFlush(feedback2)
        entityManager.persistAndFlush(feedback3)

        // When
        val pageable = PageRequest.of(0, 10)
        val pendingFeedback = feedbackRepository.findByStatus(FeedbackStatus.PENDING, pageable)

        // Then
        assertThat(pendingFeedback.content).hasSize(2)
        assertThat(pendingFeedback.content).allMatch { it.status == FeedbackStatus.PENDING }
    }

    @Test
    fun `findAllByOrderByCreatedAtDesc should return all feedback ordered by creation time`() {
        // Given
        val feedback1 = Feedback(
            user = user1,
            conversation = conversation1,
            isPositive = true,
            status = FeedbackStatus.PENDING
        )
        val feedback2 = Feedback(
            user = user2,
            conversation = conversation2,
            isPositive = false,
            status = FeedbackStatus.RESOLVED
        )
        
        entityManager.persistAndFlush(feedback1)
        entityManager.persistAndFlush(feedback2)

        // When
        val pageable = PageRequest.of(0, 10)
        val allFeedback = feedbackRepository.findAllByOrderByCreatedAtDesc(pageable)

        // Then
        assertThat(allFeedback.content).hasSize(2)
        assertThat(allFeedback.content[0].createdAt).isAfterOrEqualTo(allFeedback.content[1].createdAt)
    }

    @Test
    fun `findByUserAndStatus should return feedback matching user and status`() {
        // Given
        val feedback1 = Feedback(
            user = user1,
            conversation = conversation1,
            isPositive = true,
            status = FeedbackStatus.PENDING
        )
        val feedback2 = Feedback(
            user = user1,
            conversation = conversation1,
            isPositive = false,
            status = FeedbackStatus.RESOLVED
        )
        val feedback3 = Feedback(
            user = user2,
            conversation = conversation2,
            isPositive = true,
            status = FeedbackStatus.PENDING
        )
        
        entityManager.persistAndFlush(feedback1)
        entityManager.persistAndFlush(feedback2)
        entityManager.persistAndFlush(feedback3)

        // When
        val pageable = PageRequest.of(0, 10)
        val user1PendingFeedback = feedbackRepository.findByUserAndStatus(user1, FeedbackStatus.PENDING, pageable)

        // Then
        assertThat(user1PendingFeedback.content).hasSize(1)
        assertThat(user1PendingFeedback.content[0].user.id).isEqualTo(user1.id)
        assertThat(user1PendingFeedback.content[0].status).isEqualTo(FeedbackStatus.PENDING)
    }

    @Test
    fun `findByConversation should return feedback for specific conversation`() {
        // Given
        val feedback1 = Feedback(
            user = user1,
            conversation = conversation1,
            isPositive = true,
            status = FeedbackStatus.PENDING
        )
        val feedback2 = Feedback(
            user = user2,
            conversation = conversation1,
            isPositive = false,
            status = FeedbackStatus.RESOLVED
        )
        val feedback3 = Feedback(
            user = user1,
            conversation = conversation2,
            isPositive = true,
            status = FeedbackStatus.PENDING
        )
        
        entityManager.persistAndFlush(feedback1)
        entityManager.persistAndFlush(feedback2)
        entityManager.persistAndFlush(feedback3)

        // When
        val conversation1Feedback = feedbackRepository.findByConversation(conversation1)

        // Then
        assertThat(conversation1Feedback).hasSize(2)
        assertThat(conversation1Feedback).allMatch { it.conversation.id == conversation1.id }
    }

    @Test
    fun `existsByUserAndConversation should return true when feedback exists`() {
        // Given
        val feedback = Feedback(
            user = user1,
            conversation = conversation1,
            isPositive = true,
            status = FeedbackStatus.PENDING
        )
        entityManager.persistAndFlush(feedback)

        // When
        val exists = feedbackRepository.existsByUserAndConversation(user1, conversation1)
        val notExists = feedbackRepository.existsByUserAndConversation(user2, conversation1)

        // Then
        assertThat(exists).isTrue
        assertThat(notExists).isFalse
    }

    @Test
    fun `findByIdAndUser should return feedback when user owns it`() {
        // Given
        val feedback = Feedback(
            user = user1,
            conversation = conversation1,
            isPositive = true,
            status = FeedbackStatus.PENDING
        )
        entityManager.persistAndFlush(feedback)

        // When
        val foundFeedback = feedbackRepository.findByIdAndUser(feedback.id, user1)
        val notFoundFeedback = feedbackRepository.findByIdAndUser(feedback.id, user2)

        // Then
        assertThat(foundFeedback).isNotNull
        assertThat(foundFeedback?.id).isEqualTo(feedback.id)
        assertThat(notFoundFeedback).isNull()
    }

    @Test
    fun `countByStatus should return correct count for status`() {
        // Given
        repeat(3) {
            val feedback = Feedback(
                user = user1,
                conversation = conversation1,
                isPositive = true,
                status = FeedbackStatus.PENDING
            )
            entityManager.persistAndFlush(feedback)
        }
        
        repeat(2) {
            val feedback = Feedback(
                user = user1,
                conversation = conversation1,
                isPositive = false,
                status = FeedbackStatus.RESOLVED
            )
            entityManager.persistAndFlush(feedback)
        }

        // When
        val pendingCount = feedbackRepository.countByStatus(FeedbackStatus.PENDING)
        val resolvedCount = feedbackRepository.countByStatus(FeedbackStatus.RESOLVED)

        // Then
        assertThat(pendingCount).isEqualTo(3)
        assertThat(resolvedCount).isEqualTo(2)
    }

    @Test
    fun `countByIsPositive should return correct count for sentiment`() {
        // Given
        repeat(4) {
            val feedback = Feedback(
                user = user1,
                conversation = conversation1,
                isPositive = true,
                status = FeedbackStatus.PENDING
            )
            entityManager.persistAndFlush(feedback)
        }
        
        repeat(2) {
            val feedback = Feedback(
                user = user1,
                conversation = conversation1,
                isPositive = false,
                status = FeedbackStatus.PENDING
            )
            entityManager.persistAndFlush(feedback)
        }

        // When
        val positiveCount = feedbackRepository.countByIsPositive(true)
        val negativeCount = feedbackRepository.countByIsPositive(false)

        // Then
        assertThat(positiveCount).isEqualTo(4)
        assertThat(negativeCount).isEqualTo(2)
    }
}