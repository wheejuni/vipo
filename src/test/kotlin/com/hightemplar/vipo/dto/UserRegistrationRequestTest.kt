package com.hightemplar.vipo.dto

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserRegistrationRequestTest {

    private lateinit var validator: Validator

    @BeforeEach
    fun setUp() {
        validator = Validation.buildDefaultValidatorFactory().validator
    }

    @Test
    fun `should validate valid registration request`() {
        // Given
        val request = UserRegistrationRequest(
            email = "user@example.com",
            password = "StrongPass123!",
            name = "John Doe"
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should reject invalid email`() {
        // Given
        val request = UserRegistrationRequest(
            email = "invalid-email",
            password = "StrongPass123!",
            name = "John Doe"
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertFalse(violations.isEmpty())
        assertTrue(violations.any { it.propertyPath.toString() == "email" })
    }

    @Test
    fun `should reject weak password`() {
        // Given
        val request = UserRegistrationRequest(
            email = "user@example.com",
            password = "weak",
            name = "John Doe"
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertFalse(violations.isEmpty())
        assertTrue(violations.any { it.propertyPath.toString() == "password" })
    }

    @Test
    fun `should reject short name`() {
        // Given
        val request = UserRegistrationRequest(
            email = "user@example.com",
            password = "StrongPass123!",
            name = "A"
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertFalse(violations.isEmpty())
        assertTrue(violations.any { it.propertyPath.toString() == "name" })
    }

    @Test
    fun `should reject long name`() {
        // Given
        val longName = "A".repeat(101)
        val request = UserRegistrationRequest(
            email = "user@example.com",
            password = "StrongPass123!",
            name = longName
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertFalse(violations.isEmpty())
        assertTrue(violations.any { it.propertyPath.toString() == "name" })
    }

    @Test
    fun `should reject blank fields`() {
        // Given
        val request = UserRegistrationRequest(
            email = "",
            password = "",
            name = ""
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertFalse(violations.isEmpty())
        assertTrue(violations.size >= 3) // All fields should have violations
    }
}