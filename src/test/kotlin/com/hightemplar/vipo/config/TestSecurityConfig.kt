package com.hightemplar.vipo.config

import com.hightemplar.vipo.entity.User
import com.hightemplar.vipo.entity.UserRole
import com.hightemplar.vipo.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import java.util.*

@TestConfiguration
@EnableWebFluxSecurity
class TestSecurityConfig {
    
    @Bean
    @Primary
    fun testSecurityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .authorizeExchange { exchanges ->
                exchanges.anyExchange().permitAll()
            }
            .build()
    }
    
    @Bean
    @Primary
    fun testJwtTokenProvider(): JwtTokenProvider {
        val mockProvider = mockk<JwtTokenProvider>()
        every { mockProvider.validateToken(any()) } returns true
        every { mockProvider.getUsernameFromToken(any()) } returns "test@example.com"
        every { mockProvider.getRoleFromToken(any()) } returns "MEMBER"
        every { mockProvider.generateToken(any<String>(), any()) } returns "mock-jwt-token"
        every { mockProvider.isTokenExpired(any()) } returns false
        return mockProvider
    }
    
    @Bean
    @Primary
    fun testUserRepository(): UserRepository {
        val mockRepository = mockk<UserRepository>()
        val testUser = User(
            email = "test@example.com",
            password = "password123",
            name = "Test User",
            role = UserRole.MEMBER
        )
        
        every { mockRepository.findByEmail("test@example.com") } returns Optional.of(testUser)
        every { mockRepository.findByEmail("admin@example.com") } returns Optional.of(
            testUser.copy(role = UserRole.ADMIN)
        )
        every { mockRepository.save(any()) } returns testUser
        every { mockRepository.findById(any()) } returns Optional.of(testUser)
        
        return mockRepository
    }
    
    @Bean
    @Primary
    fun testReactiveUserDetailsService(userRepository: UserRepository): ReactiveUserDetailsService {
        return CustomUserDetailsService(userRepository)
    }
    
    @Bean
    @Primary
    fun testPasswordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder(4) // Lower strength for faster tests
    }
    
    @Bean
    @Primary
    fun testJwtAuthenticationWebFilter(
        jwtTokenProvider: JwtTokenProvider,
        userDetailsService: ReactiveUserDetailsService
    ): JwtAuthenticationWebFilter {
        return JwtAuthenticationWebFilter(jwtTokenProvider, userDetailsService)
    }
}