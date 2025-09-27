package com.hightemplar.vipo.dto

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConversationRequestTest {

    private lateinit var validator: Validator

    @BeforeEach
    fun setUp() {
        validator = Validation.buildDefaultValidatorFactory().validator
    }

    @Test
    fun `should validate valid conversation request`() {
        // Given
        val request = ConversationRequest(
            question = "What is artificial intelligence?",
            model = "gpt-4",
            temperature = 0.7,
            isStreaming = false
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should validate minimal conversation request`() {
        // Given
        val request = ConversationRequest(
            question = "What is AI?"
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should reject blank question`() {
        // Given
        val request = ConversationRequest(
            question = ""
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertFalse(violations.isEmpty())
        assertTrue(violations.any { it.propertyPath.toString() == "question" })
    }

    @Test
    fun `should reject too short question`() {
        // Given
        val request = ConversationRequest(
            question = "Hi"
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertFalse(violations.isEmpty())
        assertTrue(violations.any { it.propertyPath.toString() == "question" })
    }

    @Test
    fun `should reject question with inappropriate content`() {
        // Given
        val request = ConversationRequest(
            question = "This is spam content"
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertFalse(violations.isEmpty())
        assertTrue(violations.any { it.propertyPath.toString() == "question" })
    }

    @Test
    fun `should reject too long question`() {
        // Given
        val longQuestion = "A".repeat(10001)
        val request = ConversationRequest(
            question = longQuestion
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertFalse(violations.isEmpty())
        assertTrue(violations.any { it.propertyPath.toString() == "question" })
    }

    @Test
    fun `should reject invalid temperature values`() {
        // Given
        val request1 = ConversationRequest(
            question = "What is AI?",
            temperature = -0.1
        )
        val request2 = ConversationRequest(
            question = "What is AI?",
            temperature = 1.1
        )

        // When
        val violations1 = validator.validate(request1)
        val violations2 = validator.validate(request2)

        // Then
        assertFalse(violations1.isEmpty())
        assertFalse(violations2.isEmpty())
        assertTrue(violations1.any { it.propertyPath.toString() == "temperature" })
        assertTrue(violations2.any { it.propertyPath.toString() == "temperature" })
    }

    @Test
    fun `should reject too long model name`() {
        // Given
        val longModel = "A".repeat(51)
        val request = ConversationRequest(
            question = "What is AI?",
            model = longModel
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertFalse(violations.isEmpty())
        assertTrue(violations.any { it.propertyPath.toString() == "model" })
    }
}