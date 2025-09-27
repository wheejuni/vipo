package com.hightemplar.vipo.service

import com.hightemplar.vipo.entity.*
import com.hightemplar.vipo.repository.ActivityLogRepository
import com.hightemplar.vipo.repository.ConversationRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.LocalDateTime

class ConversationServiceTest {
    
    private lateinit var conversationRepository: ConversationRepository
    private lateinit var threadService: ThreadService
    private lateinit var aiService: AIService
    private lateinit var activityLogRepository: ActivityLogRepository
    private lateinit var conversationService: ConversationService
    
    private lateinit var testUser: User
    private lateinit var testThread: Thread
    
    @BeforeEach
    fun setUp() {
        conversationRepository = mockk()
        threadService = mockk()
        aiService = mockk()
        activityLogRepository = mockk()
        conversationService = ConversationService(
            conversationRepository,
            threadService,
            aiService,
            activityLogRepository
        )
        
        testUser = User(
            email = "test@example.com",
            password = "password123",
            name = "Test User",
            role = UserRole.MEMBER
        )
        
        testThread = Thread(
            user = testUser,
            lastActivityAt = LocalDateTime.now()
        )
    }
    
    @Test
    fun `createConversation creates conversation successfully`() {
        // Given
        val question = "What is AI?"
        val aiResponse = "AI stands for Artificial Intelligence."
        val model = "gpt-3.5-turbo"
        val temperature = 0.7
        val isStreaming = false
        
        every { threadService.getOrCreateThreadForUser(testUser) } returns testThread
        every { aiService.isModelSupported(model) } returns true
        every { aiService.isTemperatureValid(temperature) } returns true
        every { aiService.generateResponse(question, model, temperature, isStreaming) } returns Mono.just(aiResponse)
        
        val conversationSlot = slot<Conversation>()
        every { conversationRepository.save(capture(conversationSlot)) } answers { conversationSlot.captured }
        every { threadService.updateThreadActivity(testThread) } just Runs
        
        val activityLogSlot = slot<ActivityLog>()
        every { activityLogRepository.save(capture(activityLogSlot)) } answers { activityLogSlot.captured }
        
        // When
        val result = conversationService.createConversation(testUser, question, model, temperature, isStreaming)
        
        // Then
        StepVerifier.create(result)
            .assertNext { conversation ->
                assertEquals(testThread, conversation.thread)
                assertEquals(question, conversation.question)
                assertEquals(aiResponse, conversation.answer)
                assertEquals(model, conversation.model)
                assertEquals(isStreaming, conversation.isStreaming)
            }
            .verifyComplete()
        
        verify { threadService.getOrCreateThreadForUser(testUser) }
        verify { aiService.generateResponse(question, model, temperature, isStreaming) }
        verify { conversationRepository.save(any()) }
        verify { threadService.updateThreadActivity(testThread) }
        verify { activityLogRepository.save(any()) }
        
        // Verify activity log
        assertEquals(testUser, activityLogSlot.captured.user)
        assertEquals(ActivityType.CONVERSATION_CREATED, activityLogSlot.captured.activityType)
    }
    
    @Test
    fun `createConversation with default parameters`() {
        // Given
        val question = "Hello"
        val aiResponse = "Hello! How can I help you?"
        
        every { threadService.getOrCreateThreadForUser(testUser) } returns testThread
        every { aiService.isModelSupported(AIService.DEFAULT_MODEL) } returns true
        every { aiService.isTemperatureValid(AIService.DEFAULT_TEMPERATURE.toDouble()) } returns true
        every { aiService.generateResponse(question, AIService.DEFAULT_MODEL, AIService.DEFAULT_TEMPERATURE.toDouble(), false) } returns Mono.just(aiResponse)
        
        val conversationSlot = slot<Conversation>()
        every { conversationRepository.save(capture(conversationSlot)) } answers { conversationSlot.captured }
        every { threadService.updateThreadActivity(testThread) } just Runs
        every { activityLogRepository.save(any()) } returns mockk()
        
        // When
        val result = conversationService.createConversation(testUser, question)
        
        // Then
        StepVerifier.create(result)
            .assertNext { conversation ->
                assertEquals(AIService.DEFAULT_MODEL, conversation.model)
                assertEquals(false, conversation.isStreaming)
            }
            .verifyComplete()
    }
    
    @Test
    fun `createConversation handles streaming response`() {
        // Given
        val question = "Tell me a story"
        val model = "gpt-4"
        val temperature = 0.8
        val isStreaming = true
        val chunks = listOf("Once", " upon", " a", " time")
        val expectedResponse = "Once upon a time"
        
        every { threadService.getOrCreateThreadForUser(testUser) } returns testThread
        every { aiService.isModelSupported(model) } returns true
        every { aiService.isTemperatureValid(temperature) } returns true
        every { aiService.generateStreamingResponse(question, model, temperature) } returns reactor.core.publisher.Flux.fromIterable(chunks)
        
        val conversationSlot = slot<Conversation>()
        every { conversationRepository.save(capture(conversationSlot)) } answers { conversationSlot.captured }
        every { threadService.updateThreadActivity(testThread) } just Runs
        every { activityLogRepository.save(any()) } returns mockk()
        
        // When
        val result = conversationService.createConversation(testUser, question, model, temperature, isStreaming)
        
        // Then
        StepVerifier.create(result)
            .assertNext { conversation ->
                assertEquals(expectedResponse, conversation.answer)
                assertEquals(isStreaming, conversation.isStreaming)
            }
            .verifyComplete()
    }
    
    @Test
    fun `createConversation validates empty question`() {
        // Given
        val emptyQuestion = ""
        
        // When & Then
        val result = conversationService.createConversation(testUser, emptyQuestion)
        
        StepVerifier.create(result)
            .expectError(ConversationValidationException::class.java)
            .verify()
    }
    
    @Test
    fun `createConversation validates question length`() {
        // Given
        val longQuestion = "a".repeat(10001)
        
        // When & Then
        val result = conversationService.createConversation(testUser, longQuestion)
        
        StepVerifier.create(result)
            .expectError(ConversationValidationException::class.java)
            .verify()
    }
    
    @Test
    fun `createConversation validates unsupported model`() {
        // Given
        val question = "Test question"
        val unsupportedModel = "unsupported-model"
        
        every { aiService.isModelSupported(unsupportedModel) } returns false
        
        // When & Then
        val result = conversationService.createConversation(testUser, question, unsupportedModel)
        
        StepVerifier.create(result)
            .expectError(ConversationValidationException::class.java)
            .verify()
    }
    
    @Test
    fun `createConversation validates invalid temperature`() {
        // Given
        val question = "Test question"
        val invalidTemperature = 1.5
        
        every { aiService.isModelSupported(AIService.DEFAULT_MODEL) } returns true
        every { aiService.isTemperatureValid(invalidTemperature) } returns false
        
        // When & Then
        val result = conversationService.createConversation(testUser, question, temperature = invalidTemperature)
        
        StepVerifier.create(result)
            .expectError(ConversationValidationException::class.java)
            .verify()
    }
    
    @Test
    fun `createConversation handles AI service error`() {
        // Given
        val question = "Test question"
        val aiError = RuntimeException("AI service error")
        
        every { threadService.getOrCreateThreadForUser(testUser) } returns testThread
        every { aiService.isModelSupported(AIService.DEFAULT_MODEL) } returns true
        every { aiService.isTemperatureValid(AIService.DEFAULT_TEMPERATURE.toDouble()) } returns true
        every { aiService.generateResponse(any(), any(), any(), any()) } returns Mono.error(aiError)
        
        // When & Then
        val result = conversationService.createConversation(testUser, question)
        
        StepVerifier.create(result)
            .expectError(ConversationCreationException::class.java)
            .verify()
    }
    
    @Test
    fun `getConversationsForThread returns paginated conversations`() {
        // Given
        val conversations = listOf(
            Conversation(testThread, "Q1", "A1", "gpt-3.5-turbo", false),
            Conversation(testThread, "Q2", "A2", "gpt-3.5-turbo", false)
        )
        val pageable = PageRequest.of(0, 10)
        val page = PageImpl(conversations)
        
        every { conversationRepository.findByThreadOrderByCreatedAtAsc(testThread, pageable) } returns page
        
        // When
        val result = conversationService.getConversationsForThread(testThread, pageable)
        
        // Then
        assertEquals(page, result)
        verify { conversationRepository.findByThreadOrderByCreatedAtAsc(testThread, pageable) }
    }
    
    @Test
    fun `getConversationsForUser returns paginated conversations`() {
        // Given
        val conversations = listOf(
            Conversation(testThread, "Q1", "A1", "gpt-3.5-turbo", false)
        )
        val pageable = PageRequest.of(0, 10)
        val page = PageImpl(conversations)
        
        every { conversationRepository.findByThreadUser(testUser, pageable) } returns page
        
        // When
        val result = conversationService.getConversationsForUser(testUser, pageable)
        
        // Then
        assertEquals(page, result)
        verify { conversationRepository.findByThreadUser(testUser, pageable) }
    }
    
    @Test
    fun `findConversationByIdAndUser returns conversation when found`() {
        // Given
        val conversationId = 1L
        val conversation = Conversation(testThread, "Q1", "A1", "gpt-3.5-turbo", false)
        
        every { conversationRepository.findByIdAndThreadUser(conversationId, testUser) } returns conversation
        
        // When
        val result = conversationService.findConversationByIdAndUser(conversationId, testUser)
        
        // Then
        assertEquals(conversation, result)
    }
    
    @Test
    fun `findConversationByIdAndUser throws exception when not found`() {
        // Given
        val conversationId = 1L
        
        every { conversationRepository.findByIdAndThreadUser(conversationId, testUser) } returns null
        
        // When & Then
        val exception = assertThrows<ConversationNotFoundException> {
            conversationService.findConversationByIdAndUser(conversationId, testUser)
        }
        assertEquals("Conversation with ID 1 not found for user test@example.com", exception.message)
    }
    
    @Test
    fun `countConversationsForUser returns total count`() {
        // Given
        val threads = listOf(testThread)
        val threadsPage = PageImpl(threads)
        val expectedCount = 5L
        
        every { threadService.getThreadsForUser(testUser, Pageable.unpaged()) } returns threadsPage
        every { conversationRepository.countByThread(testThread) } returns expectedCount
        
        // When
        val result = conversationService.countConversationsForUser(testUser)
        
        // Then
        assertEquals(expectedCount, result)
    }
    
    @Test
    fun `getConversationsInTimeRange returns conversations in range`() {
        // Given
        val startTime = LocalDateTime.now().minusHours(2)
        val endTime = LocalDateTime.now()
        val conversations = listOf(
            Conversation(testThread, "Q1", "A1", "gpt-3.5-turbo", false)
        )
        
        every { conversationRepository.findByCreatedAtBetweenWithUser(startTime, endTime) } returns conversations
        
        // When
        val result = conversationService.getConversationsInTimeRange(startTime, endTime)
        
        // Then
        assertEquals(conversations, result)
    }
    
    @Test
    fun `countConversationsInTimeRange returns count in range`() {
        // Given
        val startTime = LocalDateTime.now().minusHours(2)
        val endTime = LocalDateTime.now()
        val expectedCount = 3L
        
        every { conversationRepository.countByCreatedAtBetween(startTime, endTime) } returns expectedCount
        
        // When
        val result = conversationService.countConversationsInTimeRange(startTime, endTime)
        
        // Then
        assertEquals(expectedCount, result)
    }
    
    @Test
    fun `getConversationsLast24Hours returns recent conversations`() {
        // Given
        val conversations = listOf(
            Conversation(testThread, "Q1", "A1", "gpt-3.5-turbo", false)
        )
        
        every { conversationRepository.findByCreatedAtBetweenWithUser(any(), any()) } returns conversations
        
        // When
        val result = conversationService.getConversationsLast24Hours()
        
        // Then
        assertEquals(conversations, result)
        verify { conversationRepository.findByCreatedAtBetweenWithUser(any(), any()) }
    }
    
    @Test
    fun `countConversationsLast24Hours returns recent count`() {
        // Given
        val expectedCount = 10L
        
        every { conversationRepository.countByCreatedAtBetween(any(), any()) } returns expectedCount
        
        // When
        val result = conversationService.countConversationsLast24Hours()
        
        // Then
        assertEquals(expectedCount, result)
        verify { conversationRepository.countByCreatedAtBetween(any(), any()) }
    }
    
    @Test
    fun `createConversation continues when activity logging fails`() {
        // Given
        val question = "What is AI?"
        val aiResponse = "AI stands for Artificial Intelligence."
        
        every { threadService.getOrCreateThreadForUser(testUser) } returns testThread
        every { aiService.isModelSupported(AIService.DEFAULT_MODEL) } returns true
        every { aiService.isTemperatureValid(AIService.DEFAULT_TEMPERATURE.toDouble()) } returns true
        every { aiService.generateResponse(any(), any(), any(), any()) } returns Mono.just(aiResponse)
        
        val conversationSlot = slot<Conversation>()
        every { conversationRepository.save(capture(conversationSlot)) } answers { conversationSlot.captured }
        every { threadService.updateThreadActivity(testThread) } just Runs
        every { activityLogRepository.save(any()) } throws RuntimeException("Activity logging failed")
        
        // When
        val result = conversationService.createConversation(testUser, question)
        
        // Then - Should still complete successfully despite activity logging failure
        StepVerifier.create(result)
            .assertNext { conversation ->
                assertEquals(question, conversation.question)
                assertEquals(aiResponse, conversation.answer)
            }
            .verifyComplete()
    }
}