package com.hightemplar.vipo.exception

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.support.WebExchangeBindException
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GlobalExceptionHandlerTest {

    private val globalExceptionHandler = GlobalExceptionHandler()

    @Test
    fun `should handle MethodArgumentNotValidException`() {
        // Given
        val bindingResult = mockk<BindingResult>()
        val fieldError = FieldError("user", "email", "Email is required")
        every { bindingResult.fieldErrors } returns listOf(fieldError)
        
        val exception = MethodArgumentNotValidException(mockk(), bindingResult)

        // When
        val response = globalExceptionHandler.handleValidationException(exception)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals(400, response.body!!.status)
        assertEquals("Validation Failed", response.body!!.error)
        assertEquals("Request validation failed", response.body!!.message)
        assertNotNull(response.body!!.details)
        assertEquals("Email is required", response.body!!.details!!["email"])
    }

    @Test
    fun `should handle WebExchangeBindException`() {
        // Given
        val exception = mockk<WebExchangeBindException>()
        val fieldError = FieldError("user", "password", "Password is required")
        every { exception.fieldErrors } returns listOf(fieldError)

        // When
        val response = globalExceptionHandler.handleWebExchangeBindException(exception)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals(400, response.body!!.status)
        assertEquals("Validation Failed", response.body!!.error)
        assertEquals("Request validation failed", response.body!!.message)
        assertNotNull(response.body!!.details)
        assertEquals("Password is required", response.body!!.details!!["password"])
    }

    @Test
    fun `should handle AuthenticationException`() {
        // Given
        val exception = object : AuthenticationException("Authentication failed") {}

        // When
        val response = globalExceptionHandler.handleAuthenticationException(exception)

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertNotNull(response.body)
        assertEquals(401, response.body!!.status)
        assertEquals("Authentication Failed", response.body!!.error)
        assertEquals("Authentication failed", response.body!!.message)
    }

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
    fun `should handle AccessDeniedException`() {
        // Given
        val exception = AccessDeniedException("Access denied")

        // When
        val response = globalExceptionHandler.handleAccessDeniedException(exception)

        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        assertNotNull(response.body)
        assertEquals(403, response.body!!.status)
        assertEquals("Access Denied", response.body!!.error)
        assertEquals("Access denied", response.body!!.message)
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
    fun `should handle FeedbackNotFoundException`() {
        // Given
        val exception = FeedbackNotFoundException("Feedback not found")

        // When
        val response = globalExceptionHandler.handleFeedbackNotFoundException(exception)

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertNotNull(response.body)
        assertEquals(404, response.body!!.status)
        assertEquals("Feedback Not Found", response.body!!.error)
        assertEquals("Feedback not found", response.body!!.message)
    }

    @Test
    fun `should handle DuplicateFeedbackException`() {
        // Given
        val exception = DuplicateFeedbackException("Feedback already exists for this conversation")

        // When
        val response = globalExceptionHandler.handleDuplicateFeedbackException(exception)

        // Then
        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertNotNull(response.body)
        assertEquals(409, response.body!!.status)
        assertEquals("Duplicate Feedback", response.body!!.error)
        assertEquals("Feedback already exists for this conversation", response.body!!.message)
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

    @Test
    fun `should handle exceptions with null messages`() {
        // Given
        val exception = mockk<InvalidCredentialsException>()
        every { exception.message } returns null

        // When
        val response = globalExceptionHandler.handleInvalidCredentialsException(exception)

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertNotNull(response.body)
        assertEquals("Invalid email or password", response.body!!.message)
    }
}