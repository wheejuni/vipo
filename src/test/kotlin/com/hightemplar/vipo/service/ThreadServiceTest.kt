package com.hightemplar.vipo.service

import com.hightemplar.vipo.entity.User
import com.hightemplar.vipo.entity.UserRole
import com.hightemplar.vipo.repository.ThreadRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

class ThreadServiceTest {
    
    private lateinit var threadRepository: ThreadRepository
    private lateinit var threadService: ThreadService
    private lateinit var testUser: User
    
    @BeforeEach
    fun setUp() {
        threadRepository = mockk()
        threadService = ThreadService(threadRepository)
        testUser = User(
            email = "test@example.com",
            password = "password123",
            name = "Test User",
            role = UserRole.MEMBER
        )
    }
    
    @Test
    fun `shouldCreateNewThread returns true when user has no threads`() {
        // Given
        val pageable = Pageable.ofSize(1)
        every { threadRepository.findByUserOrderByLastActivityAtDesc(testUser, pageable) } returns PageImpl(emptyList())
        
        // When
        val result = threadService.shouldCreateNewThread(testUser)
        
        // Then
        assertTrue(result)
        verify { threadRepository.findByUserOrderByLastActivityAtDesc(testUser, pageable) }
    }
    
    @Test
    fun `shouldCreateNewThread returns true when last activity was more than 30 minutes ago`() {
        // Given
        val oldThread = Thread(
            user = testUser,
            lastActivityAt = LocalDateTime.now().minusMinutes(31)
        )
        val pageable = Pageable.ofSize(1)
        every { threadRepository.findByUserOrderByLastActivityAtDesc(testUser, pageable) } returns PageImpl(listOf(oldThread))
        
        // When
        val result = threadService.shouldCreateNewThread(testUser)
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `shouldCreateNewThread returns false when last activity was within 30 minutes`() {
        // Given
        val recentThread = Thread(
            user = testUser,
            lastActivityAt = LocalDateTime.now().minusMinutes(15)
        )
        val pageable = Pageable.ofSize(1)
        every { threadRepository.findByUserOrderByLastActivityAtDesc(testUser, pageable) } returns PageImpl(listOf(recentThread))
        
        // When
        val result = threadService.shouldCreateNewThread(testUser)
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `shouldCreateNewThread returns true when last activity was exactly 30 minutes ago`() {
        // Given
        val exactThread = Thread(
            user = testUser,
            lastActivityAt = LocalDateTime.now().minusMinutes(30)
        )
        val pageable = Pageable.ofSize(1)
        every { threadRepository.findByUserOrderByLastActivityAtDesc(testUser, pageable) } returns PageImpl(listOf(exactThread))
        
        // When
        val result = threadService.shouldCreateNewThread(testUser)
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `getLatestThreadForUser returns null when no threads exist`() {
        // Given
        val pageable = Pageable.ofSize(1)
        every { threadRepository.findByUserOrderByLastActivityAtDesc(testUser, pageable) } returns PageImpl(emptyList())
        
        // When
        val result = threadService.getLatestThreadForUser(testUser)
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `getLatestThreadForUser returns most recent thread`() {
        // Given
        val thread = Thread(user = testUser)
        val pageable = Pageable.ofSize(1)
        every { threadRepository.findByUserOrderByLastActivityAtDesc(testUser, pageable) } returns PageImpl(listOf(thread))
        
        // When
        val result = threadService.getLatestThreadForUser(testUser)
        
        // Then
        assertEquals(thread, result)
    }
    
    @Test
    fun `createThread creates and saves new thread with current timestamp`() {
        // Given
        val threadSlot = slot<Thread>()
        every { threadRepository.save(capture(threadSlot)) } answers { threadSlot.captured }
        
        // When
        val result = threadService.createThread(testUser)
        
        // Then
        assertEquals(testUser, result.user)
        assertTrue(result.lastActivityAt.isAfter(LocalDateTime.now().minusSeconds(1)))
        assertTrue(result.lastActivityAt.isBefore(LocalDateTime.now().plusSeconds(1)))
        verify { threadRepository.save(any()) }
    }
    
    @Test
    fun `getOrCreateThreadForUser creates new thread when needed`() {
        // Given
        val pageable = Pageable.ofSize(1)
        every { threadRepository.findByUserOrderByLastActivityAtDesc(testUser, pageable) } returns PageImpl(emptyList())
        val threadSlot = slot<Thread>()
        every { threadRepository.save(capture(threadSlot)) } answers { threadSlot.captured }
        
        // When
        val result = threadService.getOrCreateThreadForUser(testUser)
        
        // Then
        assertEquals(testUser, result.user)
        verify { threadRepository.save(any()) }
    }
    
    @Test
    fun `getOrCreateThreadForUser returns existing thread when within 30 minutes`() {
        // Given
        val existingThread = Thread(
            user = testUser,
            lastActivityAt = LocalDateTime.now().minusMinutes(15)
        )
        val pageable = Pageable.ofSize(1)
        every { threadRepository.findByUserOrderByLastActivityAtDesc(testUser, pageable) } returns PageImpl(listOf(existingThread))
        
        // When
        val result = threadService.getOrCreateThreadForUser(testUser)
        
        // Then
        assertEquals(existingThread, result)
        verify(exactly = 0) { threadRepository.save(any()) }
    }
    
    @Test
    fun `updateThreadActivity updates timestamp and saves thread`() {
        // Given
        val thread = Thread(
            user = testUser,
            lastActivityAt = LocalDateTime.now().minusMinutes(10)
        )
        val originalTime = thread.lastActivityAt
        every { threadRepository.save(thread) } returns thread
        
        // When
        threadService.updateThreadActivity(thread)
        
        // Then
        assertTrue(thread.lastActivityAt.isAfter(originalTime))
        verify { threadRepository.save(thread) }
    }
    
    @Test
    fun `getThreadsForUser returns paginated threads`() {
        // Given
        val threads = listOf(Thread(user = testUser))
        val pageable = PageRequest.of(0, 10)
        val page = PageImpl(threads)
        every { threadRepository.findByUserOrderByLastActivityAtDesc(testUser, pageable) } returns page
        
        // When
        val result = threadService.getThreadsForUser(testUser, pageable)
        
        // Then
        assertEquals(page, result)
        verify { threadRepository.findByUserOrderByLastActivityAtDesc(testUser, pageable) }
    }
    
    @Test
    fun `getThreadsWithConversationsForUser returns paginated threads with conversations`() {
        // Given
        val threads = listOf(Thread(user = testUser))
        val pageable = PageRequest.of(0, 10)
        val page = PageImpl(threads)
        every { threadRepository.findByUserWithConversations(testUser, pageable) } returns page
        
        // When
        val result = threadService.getThreadsWithConversationsForUser(testUser, pageable)
        
        // Then
        assertEquals(page, result)
        verify { threadRepository.findByUserWithConversations(testUser, pageable) }
    }
    
    @Test
    fun `findThreadByIdAndUser returns thread when found`() {
        // Given
        val threadId = 1L
        val thread = Thread(user = testUser)
        every { threadRepository.findByIdAndUser(threadId, testUser) } returns thread
        
        // When
        val result = threadService.findThreadByIdAndUser(threadId, testUser)
        
        // Then
        assertEquals(thread, result)
    }
    
    @Test
    fun `findThreadByIdAndUser throws exception when thread not found`() {
        // Given
        val threadId = 1L
        every { threadRepository.findByIdAndUser(threadId, testUser) } returns null
        
        // When & Then
        val exception = assertThrows<ThreadNotFoundException> {
            threadService.findThreadByIdAndUser(threadId, testUser)
        }
        assertEquals("Thread with ID 1 not found for user test@example.com", exception.message)
    }
    
    @Test
    fun `deleteThread deletes thread when found`() {
        // Given
        val threadId = 1L
        val thread = Thread(user = testUser)
        every { threadRepository.findByIdAndUser(threadId, testUser) } returns thread
        every { threadRepository.delete(thread) } returns Unit
        
        // When
        threadService.deleteThread(threadId, testUser)
        
        // Then
        verify { threadRepository.delete(thread) }
    }
    
    @Test
    fun `deleteThread throws exception when thread not found`() {
        // Given
        val threadId = 1L
        every { threadRepository.findByIdAndUser(threadId, testUser) } returns null
        
        // When & Then
        assertThrows<ThreadNotFoundException> {
            threadService.deleteThread(threadId, testUser)
        }
        verify(exactly = 0) { threadRepository.delete(any()) }
    }
    
    @Test
    fun `countThreadsForUser returns thread count`() {
        // Given
        val expectedCount = 5L
        every { threadRepository.countByUser(testUser) } returns expectedCount
        
        // When
        val result = threadService.countThreadsForUser(testUser)
        
        // Then
        assertEquals(expectedCount, result)
        verify { threadRepository.countByUser(testUser) }
    }
}