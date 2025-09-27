package com.hightemplar.vipo.service

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient

class AIServiceTest {
    
    private lateinit var chatClient: ChatClient
    private lateinit var aiService: AIService
    
    @BeforeEach
    fun setUp() {
        chatClient = mockk(relaxed = true)
        aiService = AIService(chatClient)
    }
    
    @Test
    fun `isModelSupported returns true for supported models`() {
        // Test supported models
        assertTrue(aiService.isModelSupported("gpt-3.5-turbo"))
        assertTrue(aiService.isModelSupported("gpt-3.5-turbo-16k"))
        assertTrue(aiService.isModelSupported("gpt-4"))
        assertTrue(aiService.isModelSupported("gpt-4-turbo"))
        assertTrue(aiService.isModelSupported("gpt-4o"))
        assertTrue(aiService.isModelSupported("gpt-4o-mini"))
    }
    
    @Test
    fun `isModelSupported returns false for unsupported models`() {
        // Test unsupported models
        assertFalse(aiService.isModelSupported("unsupported-model"))
        assertFalse(aiService.isModelSupported("gpt-3"))
        assertFalse(aiService.isModelSupported(""))
    }
    
    @Test
    fun `isTemperatureValid returns true for valid temperatures`() {
        assertTrue(aiService.isTemperatureValid(0.0))
        assertTrue(aiService.isTemperatureValid(0.5))
        assertTrue(aiService.isTemperatureValid(1.0))
    }
    
    @Test
    fun `isTemperatureValid returns false for invalid temperatures`() {
        assertFalse(aiService.isTemperatureValid(-0.1))
        assertFalse(aiService.isTemperatureValid(1.1))
        assertFalse(aiService.isTemperatureValid(2.0))
    }
    
    @Test
    fun `createPromptWithContext creates prompt with system context`() {
        // Given
        val question = "What is AI?"
        val systemContext = "You are a helpful assistant."
        
        // When
        val result = aiService.createPromptWithContext(question, systemContext)
        
        // Then
        assertEquals("System: You are a helpful assistant.\n\nUser: What is AI?", result)
    }
    
    @Test
    fun `createPromptWithContext returns question when no context provided`() {
        // Given
        val question = "What is AI?"
        
        // When
        val result = aiService.createPromptWithContext(question, null)
        
        // Then
        assertEquals(question, result)
    }
    
    @Test
    fun `generateResponse returns Mono for valid input`() {
        // Given
        val question = "What is the capital of France?"
        
        // When
        val result = aiService.generateResponse(question)
        
        // Then
        assertNotNull(result)
        // The actual response would be tested in integration tests
    }
    
    @Test
    fun `generateStreamingResponse returns Flux for valid input`() {
        // Given
        val question = "Tell me a story"
        
        // When
        val result = aiService.generateStreamingResponse(question)
        
        // Then
        assertNotNull(result)
        // The actual streaming response would be tested in integration tests
    }
    
    @Test
    fun `generateResponseWithContext returns Mono for valid input`() {
        // Given
        val question = "What is AI?"
        val systemContext = "You are a helpful assistant."
        
        // When
        val result = aiService.generateResponseWithContext(question, systemContext)
        
        // Then
        assertNotNull(result)
        // The actual response would be tested in integration tests
    }
    
    @Test
    fun `constants have expected values`() {
        assertEquals("gpt-3.5-turbo", AIService.DEFAULT_MODEL)
        assertEquals(0.7f, AIService.DEFAULT_TEMPERATURE)
        assertEquals("I apologize, but I'm currently unable to process your request. Please try again later.", AIService.FALLBACK_RESPONSE)
    }
}