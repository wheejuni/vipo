package com.hightemplar.vipo.validation

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidConversationQuestionTest {

    private lateinit var validator: Validator

    @BeforeEach
    fun setUp() {
        validator = Validation.buildDefaultValidatorFactory().validator
    }

    @Test
    fun `should validate good question`() {
        // Given
        val testObject = TestQuestionObject("What is artificial intelligence?")

        // When
        val violations = validator.validate(testObject)

        // Then
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should reject question that is too short`() {
        // Given
        val testObject = TestQuestionObject("Hi")

        // When
        val violations = validator.validate(testObject)

        // Then
        assertFalse(violations.isEmpty())
        assertTrue(violations.any { it.message.contains("at least 3 characters") })
    }

    @Test
    fun `should reject question with only whitespace`() {
        // Given
        val testObject = TestQuestionObject("   ")

        // When
        val violations = validator.validate(testObject)

        // Then
        assertFalse(violations.isEmpty())
    }

    @Test
    fun `should reject question with inappropriate content`() {
        // Given
        val testObject = TestQuestionObject("This is spam content")

        // When
        val violations = validator.validate(testObject)

        // Then
        assertFalse(violations.isEmpty())
        assertTrue(violations.any { it.message.contains("inappropriate content") })
    }

    @Test
    fun `should reject null or blank question`() {
        // Given
        val testObject1 = TestQuestionObject("")
        val testObject2 = TestQuestionObject("   ")

        // When
        val violations1 = validator.validate(testObject1)
        val violations2 = validator.validate(testObject2)

        // Then
        assertFalse(violations1.isEmpty())
        assertFalse(violations2.isEmpty())
    }

    @Test
    fun `should accept long valid question`() {
        // Given
        val longQuestion = "This is a very long question about artificial intelligence and machine learning " +
                "that should be accepted because it contains meaningful content and is not inappropriate."
        val testObject = TestQuestionObject(longQuestion)

        // When
        val violations = validator.validate(testObject)

        // Then
        if (violations.isNotEmpty()) {
            println("Violations: ${violations.map { it.message }}")
        }
        assertTrue(violations.isEmpty())
    }

    private data class TestQuestionObject(
        @field:ValidConversationQuestion
        val question: String
    )
}