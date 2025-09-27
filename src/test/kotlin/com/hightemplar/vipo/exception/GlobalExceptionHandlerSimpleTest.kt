package com.hightemplar.vipo.exception

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GlobalExceptionHandlerSimpleTest {

    private val globalExceptionHandler = GlobalExceptionHandler()

    @Test
    fun `should handle InvalidCredentialsException`() {
        // Given
        val exception = InvalidCredentialsException("Invalid email or password")

        // When
        val response = globalExceptionHandler.handleInvalidCredentialsException(exception)

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertNotNull(response.body)
        assertEquals(401, response.body!!.status)
        assertEquals("Invalid Credentials", response.body!!.error)
        assertEquals("Invalid email or password", response.body!!.message)
    }

    @Test
    fun `should handle UserAlreadyExistsException`() {
        // Given
        val exception = UserAlreadyExistsException("User with this email already exists")

        // When
        val response = globalExceptionHandler.handleUserAlreadyExistsException(exception)

        // Then
        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertNotNull(response.body)
        assertEquals(409, response.body!!.status)
        assertEquals("User Already Exists", response.body!!.error)
        assertEquals("User with this email already exists", response.body!!.message)
    }

    @Test
    fun `should handle ConversationNotFoundException`() {
        // Given
        val exception = ConversationNotFoundException("Conversation not found")

        // When
        val response = globalExceptionHandler.handleConversationNotFoundException(exception)

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertNotNull(response.body)
        assertEquals(404, response.body!!.status)
        assertEquals("Conversation Not Found", response.body!!.error)
        assertEquals("Conversation not found", response.body!!.message)
    }

    @Test
    fun `should handle generic Exception`() {
        // Given
        val exception = RuntimeException("Something went wrong")

        // When
        val response = globalExceptionHandler.handleGenericException(exception)

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertNotNull(response.body)
        assertEquals(500, response.body!!.status)
        assertEquals("Internal Server Error", response.body!!.error)
        assertEquals("An unexpected error occurred", response.body!!.message)
    }
}