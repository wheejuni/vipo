package com.hightemplar.vipo.config

import com.hightemplar.vipo.entity.User
import com.hightemplar.vipo.entity.UserRole
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class JwtAuthenticationWebFilterTest {

    private val jwtTokenProvider = mockk<JwtTokenProvider>()
    private val userDetailsService = mockk<ReactiveUserDetailsService>()
    private val filterChain = mockk<WebFilterChain>()
    
    private lateinit var jwtAuthenticationWebFilter: JwtAuthenticationWebFilter

    @BeforeEach
    fun setUp() {
        jwtAuthenticationWebFilter = JwtAuthenticationWebFilter(jwtTokenProvider, userDetailsService)
    }

    @Test
    fun `should authenticate user with valid JWT token`() {
        // Given
        val token = "valid.jwt.token"
        val email = "test@example.com"
        val user = User(
            email = email,
            password = "password123",
            name = "Test User",
            role = UserRole.MEMBER
        )
        val userPrincipal = UserPrincipal(user)

        val request = MockServerHttpRequest.get("/api/test")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .build()
        val exchange = MockServerWebExchange.from(request)

        every { jwtTokenProvider.validateToken(token) } returns true
        every { jwtTokenProvider.getUsernameFromToken(token) } returns email
        every { userDetailsService.findByUsername(email) } returns Mono.just(userPrincipal)
        every { filterChain.filter(any()) } returns Mono.empty()

        // When
        val result = jwtAuthenticationWebFilter.filter(exchange, filterChain)

        // Then
        StepVerifier.create(result)
            .verifyComplete()

        verify { filterChain.filter(any()) }
    }

    @Test
    fun `should not authenticate with invalid JWT token`() {
        // Given
        val token = "invalid.jwt.token"

        val request = MockServerHttpRequest.get("/api/test")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .build()
        val exchange = MockServerWebExchange.from(request)

        every { jwtTokenProvider.validateToken(token) } returns false
        every { filterChain.filter(exchange) } returns Mono.empty()

        // When
        val result = jwtAuthenticationWebFilter.filter(exchange, filterChain)

        // Then
        StepVerifier.create(result)
            .verifyComplete()

        verify { filterChain.filter(exchange) }
    }

    @Test
    fun `should not authenticate without Authorization header`() {
        // Given
        val request = MockServerHttpRequest.get("/api/test").build()
        val exchange = MockServerWebExchange.from(request)

        every { filterChain.filter(exchange) } returns Mono.empty()

        // When
        val result = jwtAuthenticationWebFilter.filter(exchange, filterChain)

        // Then
        StepVerifier.create(result)
            .verifyComplete()

        verify { filterChain.filter(exchange) }
    }

    @Test
    fun `should not authenticate with malformed Authorization header`() {
        // Given
        val request = MockServerHttpRequest.get("/api/test")
            .header(HttpHeaders.AUTHORIZATION, "InvalidHeader token")
            .build()
        val exchange = MockServerWebExchange.from(request)

        every { filterChain.filter(exchange) } returns Mono.empty()

        // When
        val result = jwtAuthenticationWebFilter.filter(exchange, filterChain)

        // Then
        StepVerifier.create(result)
            .verifyComplete()

        verify { filterChain.filter(exchange) }
    }

    @Test
    fun `should not authenticate with empty Bearer token`() {
        // Given
        val request = MockServerHttpRequest.get("/api/test")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ")
            .build()
        val exchange = MockServerWebExchange.from(request)

        every { filterChain.filter(exchange) } returns Mono.empty()

        // When
        val result = jwtAuthenticationWebFilter.filter(exchange, filterChain)

        // Then
        StepVerifier.create(result)
            .verifyComplete()

        verify { filterChain.filter(exchange) }
    }

    @Test
    fun `should handle user details service error gracefully`() {
        // Given
        val token = "valid.jwt.token"
        val email = "test@example.com"

        val request = MockServerHttpRequest.get("/api/test")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .build()
        val exchange = MockServerWebExchange.from(request)

        every { jwtTokenProvider.validateToken(token) } returns true
        every { jwtTokenProvider.getUsernameFromToken(token) } returns email
        every { userDetailsService.findByUsername(email) } returns Mono.error(RuntimeException("User service error"))
        every { filterChain.filter(exchange) } returns Mono.empty()

        // When
        val result = jwtAuthenticationWebFilter.filter(exchange, filterChain)

        // Then
        StepVerifier.create(result)
            .verifyComplete()

        verify { filterChain.filter(exchange) }
    }

    @Test
    fun `should authenticate admin user with valid JWT token`() {
        // Given
        val token = "valid.admin.token"
        val email = "admin@example.com"
        val user = User(
            email = email,
            password = "password123",
            name = "Admin User",
            role = UserRole.ADMIN
        )
        val userPrincipal = UserPrincipal(user)

        val request = MockServerHttpRequest.get("/api/test")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .build()
        val exchange = MockServerWebExchange.from(request)

        every { jwtTokenProvider.validateToken(token) } returns true
        every { jwtTokenProvider.getUsernameFromToken(token) } returns email
        every { userDetailsService.findByUsername(email) } returns Mono.just(userPrincipal)
        every { filterChain.filter(any()) } returns Mono.empty()

        // When
        val result = jwtAuthenticationWebFilter.filter(exchange, filterChain)

        // Then
        StepVerifier.create(result)
            .verifyComplete()

        verify { filterChain.filter(any()) }
    }
}