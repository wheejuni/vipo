package com.hightemplar.vipo.controller

import com.hightemplar.vipo.config.UserPrincipal
import com.hightemplar.vipo.dto.ConversationRequest
import com.hightemplar.vipo.entity.*
import com.hightemplar.vipo.service.ConversationService
import com.hightemplar.vipo.service.ThreadService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.LocalDateTime

/**
 * Unit tests for ConversationController demonstrating WebFlux compliance.
 * These tests verify that all controller methods return proper reactive types.
 */
class ConversationControllerUnitTest {
    
    private lateinit var conversationService: ConversationService
    private lateinit var threadService: ThreadService
    private lateinit var conversationController: ConversationController
    
    private lateinit var testUser: User
    private lateinit var testUserPrincipal: UserPrincipal
    private lateinit var testThread: Thread
    private lateinit var testConversation: Conversation
    
    @BeforeEach
    fun setUp() {
        conversationService = mockk()
        threadService = mockk()
        conversationController = ConversationController(conversationService, threadService)
        
        testUser = User(
            email = "test@example.com",
            password = "password123",
            name = "Test User",
            role = UserRole.MEMBER
        )
        
        testUserPrincipal = UserPrincipal(testUser)
        
        testThread = Thread(
            user = testUser,
            lastActivityAt = LocalDateTime.now()
        )
        
        testConversation = Conversation(
            thread = testThread,
            question = "What is AI?",
            answer = "AI stands for Artificial Intelligence.",
            model = "gpt-3.5-turbo",
            isStreaming = false
        )
    }
    
    @Test
    fun `createConversation returns Mono of ResponseEntity - WebFlux compliant`() {
        // Given
        val request = ConversationRequest(
            question = "What is AI?",
            model = "gpt-3.5-turbo",
            temperature = 0.7,
            isStreaming = false
        )
        
        every { conversationService.createConversation(any(), any(), any(), any(), any()) } returns Mono.just(testConversation)
        
        // When
        val result = conversationController.createConversation(request, testUserPrincipal)
        
        // Then - Verify it returns Mono<ResponseEntity<Any>>
        assertNotNull(result)
        assertTrue(result is Mono<*>, "Method should return Mono for WebFlux compliance")
        
        StepVerifier.create(result)
            .assertNext { response ->
                assertEquals(HttpStatus.CREATED, response.statusCode)
                assertNotNull(response.body)
            }
            .verifyComplete()
        
        verify { conversationService.createConversation(testUser, "What is AI?", "gpt-3.5-turbo", 0.7, false) }
    }
    
    @Test
    fun `createConversation with streaming returns proper response - WebFlux compliant`() {
        // Given
        val request = ConversationRequest(
            question = "What is AI?",
            model = "gpt-3.5-turbo",
            temperature = 0.7,
            isStreaming = true
        )
        
        every { conversationService.createStreamingConversation(any(), any(), any(), any()) } returns 
            Flux.just("AI", " is", " artificial", " intelligence")
        
        // When
        val result = conversationController.createConversation(request, testUserPrincipal)
        
        // Then - Verify it returns Mono<ResponseEntity<Any>>
        assertNotNull(result)
        assertTrue(result is Mono<*>, "Method should return Mono for WebFlux compliance")
        
        StepVerifier.create(result)
            .assertNext { response ->
                assertEquals(HttpStatus.OK, response.statusCode)
                assertEquals(MediaType.TEXT_EVENT_STREAM, response.headers.contentType)
                assertNotNull(response.body)
            }
            .verifyComplete()
    }
    
    @Test
    fun `getThreads returns Mono of ResponseEntity - WebFlux compliant`() {
        // Given
        val threads = listOf(testThread)
        val threadsPage = PageImpl(threads, PageRequest.of(0, 20), 1)
        
        every { threadService.getThreadsForUser(testUser, any()) } returns threadsPage
        
        // When
        val result = conversationController.getThreads(
            page = 0,
            size = 20,
            sortBy = "lastActivityAt",
            sortDirection = "desc",
            includeConversations = false,
            userPrincipal = testUserPrincipal
        )
        
        // Then - Verify it returns Mono<ResponseEntity<PageResponse<ThreadResponse>>>
        assertNotNull(result)
        assertTrue(result is Mono<*>, "Method should return Mono for WebFlux compliance")
        
        StepVerifier.create(result)
            .assertNext { response ->
                assertEquals(HttpStatus.OK, response.statusCode)
                assertNotNull(response.body)
                assertEquals(1, response.body!!.content.size)
            }
            .verifyComplete()
        
        verify { threadService.getThreadsForUser(testUser, any()) }
    }
    
    @Test
    fun `deleteThread returns Mono of ResponseEntity - WebFlux compliant`() {
        // Given
        val threadId = 1L
        
        every { threadService.deleteThread(threadId, testUser) } returns Unit
        
        // When
        val result = conversationController.deleteThread(threadId, testUserPrincipal)
        
        // Then - Verify it returns Mono<ResponseEntity<Map<String, String>>>
        assertNotNull(result)
        assertTrue(result is Mono<*>, "Method should return Mono for WebFlux compliance")
        
        StepVerifier.create(result)
            .assertNext { response ->
                assertEquals(HttpStatus.OK, response.statusCode)
                assertNotNull(response.body)
                assertEquals("Thread deleted successfully", response.body!!["message"])
            }
            .verifyComplete()
        
        verify { threadService.deleteThread(threadId, testUser) }
    }
    
    @Test
    fun `getConversation returns Mono of ResponseEntity - WebFlux compliant`() {
        // Given
        val conversationId = 1L
        
        every { conversationService.findConversationByIdAndUser(conversationId, testUser) } returns testConversation
        
        // When
        val result = conversationController.getConversation(conversationId, testUserPrincipal)
        
        // Then - Verify it returns Mono<ResponseEntity<ConversationResponse>>
        assertNotNull(result)
        assertTrue(result is Mono<*>, "Method should return Mono for WebFlux compliance")
        
        StepVerifier.create(result)
            .assertNext { response ->
                assertEquals(HttpStatus.OK, response.statusCode)
                assertNotNull(response.body)
                assertEquals("What is AI?", response.body!!.question)
            }
            .verifyComplete()
        
        verify { conversationService.findConversationByIdAndUser(conversationId, testUser) }
    }
    
    @Test
    fun `all controller methods handle errors reactively - WebFlux compliant`() {
        // Given
        val request = ConversationRequest(question = "Test question")
        
        every { conversationService.createConversation(any(), any(), any(), any(), any()) } returns 
            Mono.error(RuntimeException("Service error"))
        
        // When
        val result = conversationController.createConversation(request, testUserPrincipal)
        
        // Then - Verify error handling is reactive
        StepVerifier.create(result)
            .assertNext { response ->
                assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            }
            .verifyComplete()
    }
}