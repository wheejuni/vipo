package com.hightemplar.vipo.validation

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidPasswordTest {

    private lateinit var validator: Validator

    @BeforeEach
    fun setUp() {
        validator = Validation.buildDefaultValidatorFactory().validator
    }

    @Test
    fun `should validate strong password`() {
        // Given
        val testObject = TestPasswordObject("StrongPass123!")

        // When
        val violations = validator.validate(testObject)

        // Then
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should reject password without uppercase letter`() {
        // Given
        val testObject = TestPasswordObject("weakpass123!")

        // When
        val violations = validator.validate(testObject)

        // Then
        assertFalse(violations.isEmpty())
        assertTrue(violations.any { it.message.contains("uppercase") })
    }

    @Test
    fun `should reject password without lowercase letter`() {
        // Given
        val testObject = TestPasswordObject("WEAKPASS123!")

        // When
        val violations = validator.validate(testObject)

        // Then
        assertFalse(violations.isEmpty())
        assertTrue(violations.any { it.message.contains("lowercase") })
    }

    @Test
    fun `should reject password without digit`() {
        // Given
        val testObject = TestPasswordObject("WeakPass!")

        // When
        val violations = validator.validate(testObject)

        // Then
        assertFalse(violations.isEmpty())
        assertTrue(violations.any { it.message.contains("digit") })
    }

    @Test
    fun `should reject password without special character`() {
        // Given
        val testObject = TestPasswordObject("WeakPass123")

        // When
        val violations = validator.validate(testObject)

        // Then
        assertFalse(violations.isEmpty())
        assertTrue(violations.any { it.message.contains("special character") })
    }

    @Test
    fun `should reject null or blank password`() {
        // Given
        val testObject1 = TestPasswordObject("")
        val testObject2 = TestPasswordObject("   ")

        // When
        val violations1 = validator.validate(testObject1)
        val violations2 = validator.validate(testObject2)

        // Then
        assertFalse(violations1.isEmpty())
        assertFalse(violations2.isEmpty())
    }

    private data class TestPasswordObject(
        @field:ValidPassword
        val password: String
    )
}