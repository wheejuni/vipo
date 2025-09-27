package com.hightemplar.vipo.repository

import com.hightemplar.vipo.entity.Conversation
import com.hightemplar.vipo.entity.User
import com.hightemplar.vipo.entity.UserRole
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
class ConversationRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var conversationRepository: ConversationRepository

    private lateinit var user1: User
    private lateinit var user2: User
    private lateinit var thread1: Thread
    private lateinit var thread2: Thread

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
    }

    @Test
    fun `findByThread should return conversations for specific thread only`() {
        // Given
        val conversation1 = Conversation(
            thread = thread1,
            question = "Question 1",
            answer = "Answer 1",
            model = "gpt-3.5-turbo",
            isStreaming = false
        )
        val conversation2 = Conversation(
            thread = thread1,
            question = "Question 2",
            answer = "Answer 2",
            model = "gpt-3.5-turbo",
            isStreaming = true
        )
        val conversation3 = Conversation(
            thread = thread2,
            question = "Question 3",
            answer = "Answer 3",
            model = "gpt-4",
            isStreaming = false
        )
        
        entityManager.persistAndFlush(conversation1)
        entityManager.persistAndFlush(conversation2)
        entityManager.persistAndFlush(conversation3)

        // When
        val thread1Conversations = conversationRepository.findByThread(thread1)

        // Then
        assertThat(thread1Conversations).hasSize(2)
        assertThat(thread1Conversations).allMatch { it.thread.id == thread1.id }
    }

    @Test
    fun `findByThreadOrderByCreatedAtAsc should return conversations ordered by creation time`() {
        // Given
        val conversation1 = Conversation(
            thread = thread1,
            question = "First Question",
            answer = "First Answer",
            model = "gpt-3.5-turbo",
            isStreaming = false
        )
        val conversation2 = Conversation(
            thread = thread1,
            question = "Second Question",
            answer = "Second Answer",
            model = "gpt-3.5-turbo",
            isStreaming = false
        )
        
        entityManager.persistAndFlush(conversation1)
        entityManager.persistAndFlush(conversation2)

        // When
        val pageable = PageRequest.of(0, 10)
        val conversations = conversationRepository.findByThreadOrderByCreatedAtAsc(thread1, pageable)

        // Then
        assertThat(conversations.content).hasSize(2)
        // Since we're ordering by creation time ascending, first should be earlier or equal
        assertThat(conversations.content[0].createdAt).isBeforeOrEqualTo(conversations.content[1].createdAt)
    }

    @Test
    fun `countByThread should return correct count for thread`() {
        // Given
        repeat(3) { index ->
            val conversation = Conversation(
                thread = thread1,
                question = "Question $index",
                answer = "Answer $index",
                model = "gpt-3.5-turbo",
                isStreaming = false
            )
            entityManager.persistAndFlush(conversation)
        }
        
        repeat(2) { index ->
            val conversation = Conversation(
                thread = thread2,
                question = "Question $index",
                answer = "Answer $index",
                model = "gpt-4",
                isStreaming = false
            )
            entityManager.persistAndFlush(conversation)
        }

        // When
        val thread1Count = conversationRepository.countByThread(thread1)
        val thread2Count = conversationRepository.countByThread(thread2)

        // Then
        assertThat(thread1Count).isEqualTo(3)
        assertThat(thread2Count).isEqualTo(2)
    }

    @Test
    fun `findByCreatedAtBetween should return conversations within time range`() {
        // Given
        val now = LocalDateTime.now()
        val startTime = now.minusHours(2)
        val endTime = now.plusHours(1)
        
        val conversation1 = Conversation(
            thread = thread1,
            question = "Recent Question",
            answer = "Recent Answer",
            model = "gpt-3.5-turbo",
            isStreaming = false
        )
        entityManager.persistAndFlush(conversation1)

        // When
        val conversations = conversationRepository.findByCreatedAtBetween(startTime, endTime)

        // Then
        assertThat(conversations).hasSize(1)
        assertThat(conversations[0].question).isEqualTo("Recent Question")
    }

    @Test
    fun `findByCreatedAtBetweenWithUser should return conversations with user information`() {
        // Given
        val now = LocalDateTime.now()
        val startTime = now.minusHours(2)
        val endTime = now.plusHours(1)
        
        val conversation = Conversation(
            thread = thread1,
            question = "Question with user",
            answer = "Answer with user",
            model = "gpt-3.5-turbo",
            isStreaming = false
        )
        entityManager.persistAndFlush(conversation)

        // When
        val conversations = conversationRepository.findByCreatedAtBetweenWithUser(startTime, endTime)

        // Then
        assertThat(conversations).hasSize(1)
        assertThat(conversations[0].thread.user.email).isEqualTo("user1@example.com")
    }

    @Test
    fun `countByCreatedAtBetween should return correct count within time range`() {
        // Given
        val now = LocalDateTime.now()
        val startTime = now.minusHours(2)
        val endTime = now.plusHours(1)
        
        repeat(3) { index ->
            val conversation = Conversation(
                thread = thread1,
                question = "Question $index",
                answer = "Answer $index",
                model = "gpt-3.5-turbo",
                isStreaming = false
            )
            entityManager.persistAndFlush(conversation)
        }

        // When
        val count = conversationRepository.countByCreatedAtBetween(startTime, endTime)

        // Then
        assertThat(count).isEqualTo(3)
    }

    @Test
    fun `findByThreadUser should return conversations for specific user`() {
        // Given
        val conversation1 = Conversation(
            thread = thread1,
            question = "User1 Question",
            answer = "User1 Answer",
            model = "gpt-3.5-turbo",
            isStreaming = false
        )
        val conversation2 = Conversation(
            thread = thread2,
            question = "User2 Question",
            answer = "User2 Answer",
            model = "gpt-4",
            isStreaming = false
        )
        
        entityManager.persistAndFlush(conversation1)
        entityManager.persistAndFlush(conversation2)

        // When
        val pageable = PageRequest.of(0, 10)
        val user1Conversations = conversationRepository.findByThreadUser(user1, pageable)

        // Then
        assertThat(user1Conversations.content).hasSize(1)
        assertThat(user1Conversations.content[0].question).isEqualTo("User1 Question")
    }

    @Test
    fun `findByIdAndThreadUser should return conversation when user owns it`() {
        // Given
        val conversation = Conversation(
            thread = thread1,
            question = "Owned Question",
            answer = "Owned Answer",
            model = "gpt-3.5-turbo",
            isStreaming = false
        )
        entityManager.persistAndFlush(conversation)

        // When
        val foundConversation = conversationRepository.findByIdAndThreadUser(conversation.id, user1)

        // Then
        assertThat(foundConversation).isNotNull
        assertThat(foundConversation?.question).isEqualTo("Owned Question")
    }

    @Test
    fun `findByIdAndThreadUser should return null when user does not own conversation`() {
        // Given
        val conversation = Conversation(
            thread = thread1,
            question = "Not Owned Question",
            answer = "Not Owned Answer",
            model = "gpt-3.5-turbo",
            isStreaming = false
        )
        entityManager.persistAndFlush(conversation)

        // When
        val foundConversation = conversationRepository.findByIdAndThreadUser(conversation.id, user2)

        // Then
        assertThat(foundConversation).isNull()
    }
}