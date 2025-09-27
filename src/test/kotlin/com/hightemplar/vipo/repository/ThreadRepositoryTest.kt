package com.hightemplar.vipo.repository

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
class ThreadRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var threadRepository: ThreadRepository

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
            role = UserRole.MEMBER
        )
        
        entityManager.persistAndFlush(user1)
        entityManager.persistAndFlush(user2)
    }

    @Test
    fun `findByUser should return threads for specific user only`() {
        // Given
        val thread1 = Thread(user = user1, lastActivityAt = LocalDateTime.now().minusHours(1))
        val thread2 = Thread(user = user1, lastActivityAt = LocalDateTime.now().minusHours(2))
        val thread3 = Thread(user = user2, lastActivityAt = LocalDateTime.now().minusHours(3))
        
        entityManager.persistAndFlush(thread1)
        entityManager.persistAndFlush(thread2)
        entityManager.persistAndFlush(thread3)

        // When
        val pageable = PageRequest.of(0, 10)
        val user1Threads = threadRepository.findByUser(user1, pageable)

        // Then
        assertThat(user1Threads.content).hasSize(2)
        assertThat(user1Threads.content).allMatch { it.user.id == user1.id }
    }

    @Test
    fun `findByUserOrderByLastActivityAtDesc should return threads ordered by last activity`() {
        // Given
        val now = LocalDateTime.now()
        val thread1 = Thread(user = user1, lastActivityAt = now.minusHours(3))
        val thread2 = Thread(user = user1, lastActivityAt = now.minusHours(1))
        val thread3 = Thread(user = user1, lastActivityAt = now.minusHours(2))
        
        entityManager.persistAndFlush(thread1)
        entityManager.persistAndFlush(thread2)
        entityManager.persistAndFlush(thread3)

        // When
        val pageable = PageRequest.of(0, 10)
        val threads = threadRepository.findByUserOrderByLastActivityAtDesc(user1, pageable)

        // Then
        assertThat(threads.content).hasSize(3)
        assertThat(threads.content[0].lastActivityAt).isAfter(threads.content[1].lastActivityAt)
        assertThat(threads.content[1].lastActivityAt).isAfter(threads.content[2].lastActivityAt)
    }

    @Test
    fun `findByUser should support pagination`() {
        // Given
        repeat(5) { index ->
            val thread = Thread(
                user = user1, 
                lastActivityAt = LocalDateTime.now().minusHours(index.toLong())
            )
            entityManager.persistAndFlush(thread)
        }

        // When
        val firstPage = threadRepository.findByUser(user1, PageRequest.of(0, 2))
        val secondPage = threadRepository.findByUser(user1, PageRequest.of(1, 2))

        // Then
        assertThat(firstPage.content).hasSize(2)
        assertThat(secondPage.content).hasSize(2)
        assertThat(firstPage.totalElements).isEqualTo(5)
        assertThat(firstPage.totalPages).isEqualTo(3)
    }

    @Test
    fun `countByUser should return correct count for user`() {
        // Given
        repeat(3) {
            val thread = Thread(user = user1, lastActivityAt = LocalDateTime.now())
            entityManager.persistAndFlush(thread)
        }
        
        repeat(2) {
            val thread = Thread(user = user2, lastActivityAt = LocalDateTime.now())
            entityManager.persistAndFlush(thread)
        }

        // When
        val user1Count = threadRepository.countByUser(user1)
        val user2Count = threadRepository.countByUser(user2)

        // Then
        assertThat(user1Count).isEqualTo(3)
        assertThat(user2Count).isEqualTo(2)
    }

    @Test
    fun `findByIdAndUser should return thread when user owns it`() {
        // Given
        val thread = Thread(user = user1, lastActivityAt = LocalDateTime.now())
        entityManager.persistAndFlush(thread)

        // When
        val foundThread = threadRepository.findByIdAndUser(thread.id, user1)

        // Then
        assertThat(foundThread).isNotNull
        assertThat(foundThread?.id).isEqualTo(thread.id)
        assertThat(foundThread?.user?.id).isEqualTo(user1.id)
    }

    @Test
    fun `findByIdAndUser should return null when user does not own thread`() {
        // Given
        val thread = Thread(user = user1, lastActivityAt = LocalDateTime.now())
        entityManager.persistAndFlush(thread)

        // When
        val foundThread = threadRepository.findByIdAndUser(thread.id, user2)

        // Then
        assertThat(foundThread).isNull()
    }

    @Test
    fun `findByIdAndUser should return null when thread does not exist`() {
        // When
        val foundThread = threadRepository.findByIdAndUser(999L, user1)

        // Then
        assertThat(foundThread).isNull()
    }
}