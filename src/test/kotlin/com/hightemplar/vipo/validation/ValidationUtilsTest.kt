package com.hightemplar.vipo.validation

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidationUtilsTest {

    private lateinit var validator: Validator
    private lateinit var validationUtils: ValidationUtils

    @BeforeEach
    fun setUp() {
        validator = Validation.buildDefaultValidatorFactory().validator
        validationUtils = ValidationUtils(validator)
    }

    @Test
    fun `should validate valid object successfully`() {
        // Given
        val validObject = TestObject("valid@example.com", "ValidName")

        // When & Then
        assertTrue(validationUtils.isValid(validObject))
        validationUtils.validateAndThrow(validObject) // Should not throw
    }

    @Test
    fun `should detect invalid object`() {
        // Given
        val invalidObject = TestObject("invalid-email", "")

        // When & Then
        assertFalse(validationUtils.isValid(invalidObject))
        assertThrows<ValidationException> {
            validationUtils.validateAndThrow(invalidObject)
        }
    }

    @Test
    fun `should return validation violations`() {
        // Given
        val invalidObject = TestObject("invalid-email", "")

        // When
        val violations = validationUtils.validate(invalidObject)

        // Then
        assertTrue(violations.isNotEmpty())
        assertTrue(violations.size >= 2) // At least email and name violations
    }

    private data class TestObject(
        @field:jakarta.validation.constraints.Email
        val email: String,
        
        @field:jakarta.validation.constraints.NotBlank
        val name: String
    )
}