package com.hightemplar.vipo.service

import com.hightemplar.vipo.dto.FeedbackRequest
import com.hightemplar.vipo.entity.*
import com.hightemplar.vipo.exception.DuplicateFeedbackException
import com.hightemplar.vipo.exception.FeedbackNotFoundException
import com.hightemplar.vipo.repository.ConversationRepository
import com.hightemplar.vipo.repository.FeedbackRepository
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.AccessDeniedException
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeedbackServiceTest {
    
    private val feedbackRepository = mockk<FeedbackRepository>()
    private val conversationRepository = mockk<ConversationRepository>()
    private val feedbackService = FeedbackService(feedbackRepository, conversationRepository)
    
    private lateinit var memberUser: User
    private lateinit var adminUser: User
    private lateinit var otherUser: User
    private lateinit var thread: Thread
    private lateinit var conversation: Conversation
    private lateinit var feedback: Feedback
    
    @BeforeEach
    fun setUp() {
        clearAllMocks()
        
        memberUser = User(
            email = "member@test.com",
            password = "password",
            name = "Member User",
            role = UserRole.MEMBER
        ).apply {
            // Use reflection to set the id field
            val idField = User::class.java.superclass.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, 1L)
        }
        
        adminUser = User(
            email = "admin@test.com",
            password = "password",
            name = "Admin User",
            role = UserRole.ADMIN
        ).apply {
            val idField = User::class.java.superclass.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, 2L)
        }
        
        otherUser = User(
            email = "other@test.com",
            password = "password",
            name = "Other User",
            role = UserRole.MEMBER
        ).apply {
            val idField = User::class.java.superclass.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, 3L)
        }
        
        thread = Thread(user = memberUser).apply {
            val idField = Thread::class.java.superclass.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, 1L)
        }
        
        conversation = Conversation(
            thread = thread,
            question = "Test question",
            answer = "Test answer",
            model = "gpt-3.5-turbo",
            isStreaming = false
        ).apply {
            val idField = Conversation::class.java.superclass.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, 1L)
        }
        
        feedback = Feedback(
            user = memberUser,
            conversation = conversation,
            isPositive = true
        ).apply {
            val idField = Feedback::class.java.superclass.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, 1L)
        }
    }
    
    @Test
    fun `createFeedback should create feedback for member's own conversation`() {
        // Given
        val request = FeedbackRequest(conversationId = 1L, isPositive = true)
        
        every { conversationRepository.findById(1L) } returns Optional.of(conversation)
        every { conversationRepository.findByIdAndThreadUser(1L, memberUser) } returns conversation
        every { feedbackRepository.existsByUserAndConversation(memberUser, conversation) } returns false
        every { feedbackRepository.save(any<Feedback>()) } returns feedback
        
        // When
        val result = feedbackService.createFeedback(request, memberUser)
        
        // Then
        assertEquals(1L, result.id)
        assertEquals(1L, result.conversationId)
        assertEquals(1L, result.userId)
        assertEquals("member@test.com", result.userEmail)
        assertTrue(result.isPositive)
        assertEquals(FeedbackStatus.PENDING, result.status)
        
        verify { feedbackRepository.save(any<Feedback>()) }
    }
    
    @Test
    fun `createFeedback should create feedback for admin on any conversation`() {
        // Given
        val request = FeedbackRequest(conversationId = 1L, isPositive = false)
        
        every { conversationRepository.findById(1L) } returns Optional.of(conversation)
        every { feedbackRepository.existsByUserAndConversation(adminUser, conversation) } returns false
        every { feedbackRepository.save(any<Feedback>()) } returns feedback.copy(user = adminUser, isPositive = false)
        
        // When
        feedbackService.createFeedback(request, adminUser)
        
        // Then
        verify { feedbackRepository.save(any<Feedback>()) }
        verify(exactly = 0) { conversationRepository.findByIdAndThreadUser(any(), any()) }
    }
    
    @Test
    fun `createFeedback should throw ConversationNotFoundException when conversation not found`() {
        // Given
        val request = FeedbackRequest(conversationId = 999L, isPositive = true)
        
        every { conversationRepository.findById(999L) } returns Optional.empty()
        
        // When & Then
        val exception = assertThrows<ConversationNotFoundException> {
            feedbackService.createFeedback(request, memberUser)
        }
        assertEquals("Conversation not found with id: 999", exception.message)
    }
    
    @Test
    fun `createFeedback should throw AccessDeniedException when member tries to create feedback for other's conversation`() {
        // Given
        val request = FeedbackRequest(conversationId = 1L, isPositive = true)
        
        every { conversationRepository.findById(1L) } returns Optional.of(conversation)
        every { conversationRepository.findByIdAndThreadUser(1L, otherUser) } returns null
        
        // When & Then
        val exception = assertThrows<AccessDeniedException> {
            feedbackService.createFeedback(request, otherUser)
        }
        assertEquals("You can only create feedback for your own conversations", exception.message)
    }
    
    @Test
    fun `createFeedback should throw DuplicateFeedbackException when feedback already exists`() {
        // Given
        val request = FeedbackRequest(conversationId = 1L, isPositive = true)
        
        every { conversationRepository.findById(1L) } returns Optional.of(conversation)
        every { conversationRepository.findByIdAndThreadUser(1L, memberUser) } returns conversation
        every { feedbackRepository.existsByUserAndConversation(memberUser, conversation) } returns true
        
        // When & Then
        val exception = assertThrows<DuplicateFeedbackException> {
            feedbackService.createFeedback(request, memberUser)
        }
        assertEquals("Feedback already exists for this conversation", exception.message)
    }
    
    @Test
    fun `getFeedback should return member's own feedback only`() {
        // Given
        val pageable = PageRequest.of(0, 10)
        val feedbackPage = PageImpl(listOf(feedback))
        
        every { feedbackRepository.findByUserOrderByCreatedAtDesc(memberUser, pageable) } returns feedbackPage
        
        // When
        val result = feedbackService.getFeedback(memberUser, null, pageable)
        
        // Then
        assertEquals(1, result.content.size)
        assertEquals(1L, result.content[0].id)
        verify { feedbackRepository.findByUserOrderByCreatedAtDesc(memberUser, pageable) }
    }
    
    @Test
    fun `getFeedback should return member's feedback filtered by status`() {
        // Given
        val pageable = PageRequest.of(0, 10)
        val feedbackPage = PageImpl(listOf(feedback))
        
        every { feedbackRepository.findByUserAndStatus(memberUser, FeedbackStatus.PENDING, pageable) } returns feedbackPage
        
        // When
        val result = feedbackService.getFeedback(memberUser, FeedbackStatus.PENDING, pageable)
        
        // Then
        assertEquals(1, result.content.size)
        verify { feedbackRepository.findByUserAndStatus(memberUser, FeedbackStatus.PENDING, pageable) }
    }
    
    @Test
    fun `getFeedback should return all feedback for admin`() {
        // Given
        val pageable = PageRequest.of(0, 10)
        val feedbackPage = PageImpl(listOf(feedback))
        
        every { feedbackRepository.findAllWithUserAndConversation(pageable) } returns feedbackPage
        
        // When
        val result = feedbackService.getFeedback(adminUser, null, pageable)
        
        // Then
        assertEquals(1, result.content.size)
        verify { feedbackRepository.findAllWithUserAndConversation(pageable) }
    }
    
    @Test
    fun `getFeedback should return all feedback filtered by status for admin`() {
        // Given
        val pageable = PageRequest.of(0, 10)
        val feedbackPage = PageImpl(listOf(feedback))
        
        every { feedbackRepository.findByStatus(FeedbackStatus.RESOLVED, pageable) } returns feedbackPage
        
        // When
        val result = feedbackService.getFeedback(adminUser, FeedbackStatus.RESOLVED, pageable)
        
        // Then
        assertEquals(1, result.content.size)
        verify { feedbackRepository.findByStatus(FeedbackStatus.RESOLVED, pageable) }
    }
    
    @Test
    fun `updateFeedbackStatus should update status for admin`() {
        // Given
        val updatedFeedback = feedback.apply { status = FeedbackStatus.RESOLVED }
        
        every { feedbackRepository.findById(1L) } returns Optional.of(feedback)
        every { feedbackRepository.save(any<Feedback>()) } returns updatedFeedback
        
        // When
        val result = feedbackService.updateFeedbackStatus(1L, FeedbackStatus.RESOLVED, adminUser)
        
        // Then
        assertEquals(FeedbackStatus.RESOLVED, result.status)
        verify { feedbackRepository.save(feedback) }
    }
    
    @Test
    fun `updateFeedbackStatus should throw AccessDeniedException for member`() {
        // When & Then
        val exception = assertThrows<AccessDeniedException> {
            feedbackService.updateFeedbackStatus(1L, FeedbackStatus.RESOLVED, memberUser)
        }
        assertEquals("Only administrators can update feedback status", exception.message)
    }
    
    @Test
    fun `updateFeedbackStatus should throw FeedbackNotFoundException when feedback not found`() {
        // Given
        every { feedbackRepository.findById(999L) } returns Optional.empty()
        
        // When & Then
        val exception = assertThrows<FeedbackNotFoundException> {
            feedbackService.updateFeedbackStatus(999L, FeedbackStatus.RESOLVED, adminUser)
        }
        assertEquals("Feedback not found with id: 999", exception.message)
    }
    
    @Test
    fun `getFeedbackById should return feedback for member's own feedback`() {
        // Given
        every { feedbackRepository.findByIdAndUser(1L, memberUser) } returns feedback
        
        // When
        val result = feedbackService.getFeedbackById(1L, memberUser)
        
        // Then
        assertEquals(1L, result.id)
        verify { feedbackRepository.findByIdAndUser(1L, memberUser) }
    }
    
    @Test
    fun `getFeedbackById should return any feedback for admin`() {
        // Given
        every { feedbackRepository.findById(1L) } returns Optional.of(feedback)
        
        // When
        val result = feedbackService.getFeedbackById(1L, adminUser)
        
        // Then
        assertEquals(1L, result.id)
        verify { feedbackRepository.findById(1L) }
    }
    
    @Test
    fun `getFeedbackById should throw FeedbackNotFoundException for member accessing other's feedback`() {
        // Given
        every { feedbackRepository.findByIdAndUser(1L, otherUser) } returns null
        
        // When & Then
        val exception = assertThrows<FeedbackNotFoundException> {
            feedbackService.getFeedbackById(1L, otherUser)
        }
        assertEquals("Feedback not found or access denied", exception.message)
    }
    
    @Test
    fun `canCreateFeedback should return true for member's own conversation without existing feedback`() {
        // Given
        every { conversationRepository.findById(1L) } returns Optional.of(conversation)
        every { feedbackRepository.existsByUserAndConversation(memberUser, conversation) } returns false
        every { conversationRepository.findByIdAndThreadUser(1L, memberUser) } returns conversation
        
        // When
        val result = feedbackService.canCreateFeedback(1L, memberUser)
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `canCreateFeedback should return false when feedback already exists`() {
        // Given
        every { conversationRepository.findById(1L) } returns Optional.of(conversation)
        every { feedbackRepository.existsByUserAndConversation(memberUser, conversation) } returns true
        
        // When
        val result = feedbackService.canCreateFeedback(1L, memberUser)
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `canCreateFeedback should return false when conversation not found`() {
        // Given
        every { conversationRepository.findById(999L) } returns Optional.empty()
        
        // When
        val result = feedbackService.canCreateFeedback(999L, memberUser)
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `canCreateFeedback should return true for admin on any conversation`() {
        // Given
        every { conversationRepository.findById(1L) } returns Optional.of(conversation)
        every { feedbackRepository.existsByUserAndConversation(adminUser, conversation) } returns false
        
        // When
        val result = feedbackService.canCreateFeedback(1L, adminUser)
        
        // Then
        assertTrue(result)
        verify(exactly = 0) { conversationRepository.findByIdAndThreadUser(any(), any()) }
    }
    
    @Test
    fun `canCreateFeedback should return false for member accessing other's conversation`() {
        // Given
        every { conversationRepository.findById(1L) } returns Optional.of(conversation)
        every { feedbackRepository.existsByUserAndConversation(otherUser, conversation) } returns false
        every { conversationRepository.findByIdAndThreadUser(1L, otherUser) } returns null
        
        // When
        val result = feedbackService.canCreateFeedback(1L, otherUser)
        
        // Then
        assertFalse(result)
    }
}