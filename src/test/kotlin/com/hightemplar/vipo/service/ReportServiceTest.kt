package com.hightemplar.vipo.service

import com.hightemplar.vipo.entity.Conversation
import com.hightemplar.vipo.entity.User
import com.hightemplar.vipo.entity.UserRole
import com.hightemplar.vipo.repository.ConversationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class ReportServiceTest {
    
    private lateinit var conversationRepository: ConversationRepository
    private lateinit var reportService: ReportService
    
    private lateinit var testUser1: User
    private lateinit var testUser2: User
    private lateinit var testThread1: Thread
    private lateinit var testThread2: Thread
    private lateinit var testConversation1: Conversation
    private lateinit var testConversation2: Conversation
    private lateinit var testConversation3: Conversation
    
    @BeforeEach
    fun setUp() {
        conversationRepository = mockk()
        reportService = ReportService(conversationRepository)
        
        testUser1 = User(
            email = "user1@example.com",
            password = "hashedPassword1",
            name = "User One",
            role = UserRole.MEMBER
        )
        
        testUser2 = User(
            email = "user2@example.com",
            password = "hashedPassword2",
            name = "User Two",
            role = UserRole.ADMIN
        )
        
        testThread1 = Thread(
            user = testUser1,
            lastActivityAt = LocalDateTime.now()
        )
        
        testThread2 = Thread(
            user = testUser2,
            lastActivityAt = LocalDateTime.now()
        )
        
        testConversation1 = Conversation(
            thread = testThread1,
            question = "What is AI?",
            answer = "AI stands for Artificial Intelligence.",
            model = "gpt-4",
            isStreaming = false
        )
        
        testConversation2 = Conversation(
            thread = testThread1,
            question = "How does machine learning work?",
            answer = "Machine learning uses algorithms to learn from data.",
            model = "gpt-3.5-turbo",
            isStreaming = true
        )
        
        testConversation3 = Conversation(
            thread = testThread2,
            question = "What is \"deep learning\"?",
            answer = "Deep learning is a subset of machine learning that uses neural networks with multiple layers.",
            model = "gpt-4",
            isStreaming = false
        )
    }
    
    @Test
    fun `generateConversationReport24Hours should call repository with correct time range`() {
        // Given
        val conversations = listOf(testConversation1, testConversation2)
        every { 
            conversationRepository.findByCreatedAtBetweenWithUser(any(), any()) 
        } returns conversations
        
        // When
        val result = reportService.generateConversationReport24Hours()
        
        // Then
        assertNotNull(result)
        assertTrue(result.contains("ID,Question,Answer,Model,Streaming,Author Name,Author Email,Thread ID,Created At"))
        verify { conversationRepository.findByCreatedAtBetweenWithUser(any(), any()) }
    }
    
    @Test
    fun `generateConversationReport should generate correct CSV format`() {
        // Given
        val startTime = LocalDateTime.of(2024, 1, 15, 0, 0, 0)
        val endTime = LocalDateTime.of(2024, 1, 16, 0, 0, 0)
        val conversations = listOf(testConversation1, testConversation2, testConversation3)
        
        every { 
            conversationRepository.findByCreatedAtBetweenWithUser(startTime, endTime) 
        } returns conversations
        
        // When
        val result = reportService.generateConversationReport(startTime, endTime)
        
        // Then
        val lines = result.split("\n")
        assertEquals("ID,Question,Answer,Model,Streaming,Author Name,Author Email,Thread ID,Created At", lines[0])
        
        // Check first conversation row
        val firstRow = lines[1]
        assertTrue(firstRow.contains("1"))
        assertTrue(firstRow.contains("What is AI?"))
        assertTrue(firstRow.contains("AI stands for Artificial Intelligence."))
        assertTrue(firstRow.contains("gpt-4"))
        assertTrue(firstRow.contains("false"))
        assertTrue(firstRow.contains("User One"))
        assertTrue(firstRow.contains("user1@example.com"))
        assertTrue(firstRow.contains("2024-01-15 10:30:00"))
        
        verify { conversationRepository.findByCreatedAtBetweenWithUser(startTime, endTime) }
    }
    
    @Test
    fun `generateConversationReport should handle CSV escaping correctly`() {
        // Given
        val startTime = LocalDateTime.of(2024, 1, 15, 0, 0, 0)
        val endTime = LocalDateTime.of(2024, 1, 16, 0, 0, 0)
        val conversations = listOf(testConversation3) // Contains quotes in question
        
        every { 
            conversationRepository.findByCreatedAtBetweenWithUser(startTime, endTime) 
        } returns conversations
        
        // When
        val result = reportService.generateConversationReport(startTime, endTime)
        
        // Then
        val lines = result.split("\n")
        val dataRow = lines[1]
        
        // Should contain escaped quotes
        assertTrue(dataRow.contains("\"What is \"\"deep learning\"\"?\""))
        
        verify { conversationRepository.findByCreatedAtBetweenWithUser(startTime, endTime) }
    }
    
    @Test
    fun `getConversationReportStatistics should return correct statistics`() {
        // Given
        val startTime = LocalDateTime.of(2024, 1, 15, 0, 0, 0)
        val endTime = LocalDateTime.of(2024, 1, 16, 0, 0, 0)
        val conversations = listOf(testConversation1, testConversation2, testConversation3)
        
        every { 
            conversationRepository.findByCreatedAtBetweenWithUser(startTime, endTime) 
        } returns conversations
        
        // When
        val result = reportService.getConversationReportStatistics(startTime, endTime)
        
        // Then
        assertEquals(3, result.totalConversations)
        assertEquals(2, result.uniqueUsers) // testUser1 and testUser2
        assertEquals(2, result.uniqueThreads) // testThread1 and testThread2
        assertEquals(2, result.modelUsage["gpt-4"]) // testConversation1 and testConversation3
        assertEquals(1, result.modelUsage["gpt-3.5-turbo"]) // testConversation2
        assertEquals(1, result.streamingConversations) // only testConversation2
        assertEquals(2, result.nonStreamingConversations) // testConversation1 and testConversation3
        assertTrue(result.timeRange.contains("From $startTime to $endTime"))
        
        verify { conversationRepository.findByCreatedAtBetweenWithUser(startTime, endTime) }
    }
    
    @Test
    fun `get24HourConversationReportStatistics should call repository with correct time range`() {
        // Given
        val conversations = listOf(testConversation1)
        every { 
            conversationRepository.findByCreatedAtBetweenWithUser(any(), any()) 
        } returns conversations
        
        // When
        val result = reportService.get24HourConversationReportStatistics()
        
        // Then
        assertEquals(1, result.totalConversations)
        assertEquals(1, result.uniqueUsers)
        assertEquals(1, result.uniqueThreads)
        
        verify { conversationRepository.findByCreatedAtBetweenWithUser(any(), any()) }
    }
    
    @Test
    fun `generateConversationReportWithMetadata should return complete report`() {
        // Given
        val startTime = LocalDateTime.of(2024, 1, 15, 0, 0, 0)
        val endTime = LocalDateTime.of(2024, 1, 16, 0, 0, 0)
        val conversations = listOf(testConversation1, testConversation2)
        
        every { 
            conversationRepository.findByCreatedAtBetweenWithUser(startTime, endTime) 
        } returns conversations
        
        // When
        val result = reportService.generateConversationReportWithMetadata(startTime, endTime)
        
        // Then
        assertNotNull(result.csvContent)
        assertTrue(result.csvContent.contains("ID,Question,Answer,Model,Streaming,Author Name,Author Email,Thread ID,Created At"))
        assertEquals(2, result.statistics.totalConversations)
        assertEquals(1, result.statistics.uniqueUsers)
        assertEquals(1, result.statistics.uniqueThreads)
        assertNotNull(result.generatedAt)
        
        verify { conversationRepository.findByCreatedAtBetweenWithUser(startTime, endTime) }
    }
    
    @Test
    fun `generate24HourConversationReportWithMetadata should return complete report`() {
        // Given
        val conversations = listOf(testConversation1, testConversation2, testConversation3)
        every { 
            conversationRepository.findByCreatedAtBetweenWithUser(any(), any()) 
        } returns conversations
        
        // When
        val result = reportService.generate24HourConversationReportWithMetadata()
        
        // Then
        assertNotNull(result.csvContent)
        assertTrue(result.csvContent.contains("ID,Question,Answer,Model,Streaming,Author Name,Author Email,Thread ID,Created At"))
        assertEquals(3, result.statistics.totalConversations)
        assertEquals(2, result.statistics.uniqueUsers)
        assertEquals(2, result.statistics.uniqueThreads)
        assertNotNull(result.generatedAt)
        
        verify { conversationRepository.findByCreatedAtBetweenWithUser(any(), any()) }
    }
    
    @Test
    fun `generateConversationReport should handle empty conversation list`() {
        // Given
        val startTime = LocalDateTime.of(2024, 1, 15, 0, 0, 0)
        val endTime = LocalDateTime.of(2024, 1, 16, 0, 0, 0)
        val conversations = emptyList<Conversation>()
        
        every { 
            conversationRepository.findByCreatedAtBetweenWithUser(startTime, endTime) 
        } returns conversations
        
        // When
        val result = reportService.generateConversationReport(startTime, endTime)
        
        // Then
        val lines = result.split("\n")
        assertEquals("ID,Question,Answer,Model,Streaming,Author Name,Author Email,Thread ID,Created At", lines[0])
        assertEquals("", lines[1]) // Only header and empty line
        
        verify { conversationRepository.findByCreatedAtBetweenWithUser(startTime, endTime) }
    }
    
    @Test
    fun `getConversationReportStatistics should handle empty conversation list`() {
        // Given
        val startTime = LocalDateTime.of(2024, 1, 15, 0, 0, 0)
        val endTime = LocalDateTime.of(2024, 1, 16, 0, 0, 0)
        val conversations = emptyList<Conversation>()
        
        every { 
            conversationRepository.findByCreatedAtBetweenWithUser(startTime, endTime) 
        } returns conversations
        
        // When
        val result = reportService.getConversationReportStatistics(startTime, endTime)
        
        // Then
        assertEquals(0, result.totalConversations)
        assertEquals(0, result.uniqueUsers)
        assertEquals(0, result.uniqueThreads)
        assertTrue(result.modelUsage.isEmpty())
        assertEquals(0, result.streamingConversations)
        assertEquals(0, result.nonStreamingConversations)
        
        verify { conversationRepository.findByCreatedAtBetweenWithUser(startTime, endTime) }
    }
}