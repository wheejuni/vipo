package com.hightemplar.vipo.config

import com.hightemplar.vipo.entity.User
import com.hightemplar.vipo.entity.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.test.util.ReflectionTestUtils
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JwtTokenProviderTest {

    private lateinit var jwtTokenProvider: JwtTokenProvider
    private val jwtSecret = "mySecretKeyForTestingPurposesOnlyAndShouldBeLongEnough"
    private val jwtExpirationInMs = 86400000L // 24 hours

    @BeforeEach
    fun setUp() {
        jwtTokenProvider = JwtTokenProvider()
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", jwtSecret)
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationInMs", jwtExpirationInMs)
    }

    @Test
    fun `should generate token from authentication`() {
        // Given
        val user = User(
            email = "test@example.com",
            password = "password123",
            name = "Test User",
            role = UserRole.MEMBER
        )
        val userPrincipal = UserPrincipal(user)
        val authentication: Authentication = UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.authorities)

        // When
        val token = jwtTokenProvider.generateToken(authentication)

        // Then
        assertNotNull(token)
        assertTrue(token.isNotEmpty())
    }

    @Test
    fun `should generate token from email and role`() {
        // Given
        val email = "test@example.com"
        val role = "MEMBER"

        // When
        val token = jwtTokenProvider.generateToken(email, role)

        // Then
        assertNotNull(token)
        assertTrue(token.isNotEmpty())
    }

    @Test
    fun `should extract username from token`() {
        // Given
        val email = "test@example.com"
        val role = "MEMBER"
        val token = jwtTokenProvider.generateToken(email, role)

        // When
        val extractedEmail = jwtTokenProvider.getUsernameFromToken(token)

        // Then
        assertEquals(email, extractedEmail)
    }

    @Test
    fun `should extract role from token`() {
        // Given
        val email = "test@example.com"
        val role = "MEMBER"
        val token = jwtTokenProvider.generateToken(email, role)

        // When
        val extractedRole = jwtTokenProvider.getRoleFromToken(token)

        // Then
        assertEquals(role, extractedRole)
    }

    @Test
    fun `should validate valid token`() {
        // Given
        val email = "test@example.com"
        val role = "MEMBER"
        val token = jwtTokenProvider.generateToken(email, role)

        // When
        val isValid = jwtTokenProvider.validateToken(token)

        // Then
        assertTrue(isValid)
    }

    @Test
    fun `should reject invalid token`() {
        // Given
        val invalidToken = "invalid.token.here"

        // When
        val isValid = jwtTokenProvider.validateToken(invalidToken)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `should reject malformed token`() {
        // Given
        val malformedToken = "malformed-token"

        // When
        val isValid = jwtTokenProvider.validateToken(malformedToken)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `should get expiration date from token`() {
        // Given
        val email = "test@example.com"
        val role = "MEMBER"
        val token = jwtTokenProvider.generateToken(email, role)

        // When
        val expirationDate = jwtTokenProvider.getExpirationDateFromToken(token)

        // Then
        assertNotNull(expirationDate)
        assertTrue(expirationDate.after(Date()))
    }

    @Test
    fun `should check if token is not expired`() {
        // Given
        val email = "test@example.com"
        val role = "MEMBER"
        val token = jwtTokenProvider.generateToken(email, role)

        // When
        val isExpired = jwtTokenProvider.isTokenExpired(token)

        // Then
        assertFalse(isExpired)
    }

    @Test
    fun `should handle expired token validation`() {
        // Given - Create a token provider with very short expiration
        val shortExpirationProvider = JwtTokenProvider()
        ReflectionTestUtils.setField(shortExpirationProvider, "jwtSecret", jwtSecret)
        ReflectionTestUtils.setField(shortExpirationProvider, "jwtExpirationInMs", 1L) // 1ms
        
        val email = "test@example.com"
        val role = "MEMBER"
        val token = shortExpirationProvider.generateToken(email, role)
        
        // Wait for token to expire
        Thread.sleep(10)

        // When
        val isValid = shortExpirationProvider.validateToken(token)

        // Then
        assertFalse(isValid)
    }
}