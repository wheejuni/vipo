package com.hightemplar.vipo.config

import com.hightemplar.vipo.entity.User
import com.hightemplar.vipo.entity.UserRole
import com.hightemplar.vipo.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.userdetails.UsernameNotFoundException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CustomUserDetailsServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val userDetailsService = CustomUserDetailsService(userRepository)

    @Test
    fun `should load user by username successfully`() {
        // Given
        val email = "test@example.com"
        val user = User(
            email = email,
            password = "password123",
            name = "Test User",
            role = UserRole.MEMBER
        )
        every { userRepository.findByEmail(email) } returns java.util.Optional.of(user)

        // When
        val userDetails = userDetailsService.loadUserByUsername(email)

        // Then
        assertNotNull(userDetails)
        assertEquals(email, userDetails.username)
        assertEquals("password123", userDetails.password)
        assert(userDetails is UserPrincipal)
        
        val userPrincipal = userDetails as UserPrincipal
        assertEquals("Test User", userPrincipal.getName())
        assertEquals("MEMBER", userPrincipal.getRole())
    }

    @Test
    fun `should throw UsernameNotFoundException when user not found`() {
        // Given
        val email = "nonexistent@example.com"
        every { userRepository.findByEmail(email) } returns java.util.Optional.empty()

        // When & Then
        val exception = assertThrows<UsernameNotFoundException> {
            userDetailsService.loadUserByUsername(email)
        }
        
        assertEquals("User not found with email: $email", exception.message)
    }

    @Test
    fun `should load admin user successfully`() {
        // Given
        val email = "admin@example.com"
        val user = User(
            email = email,
            password = "adminpass123",
            name = "Admin User",
            role = UserRole.ADMIN
        )
        every { userRepository.findByEmail(email) } returns java.util.Optional.of(user)

        // When
        val userDetails = userDetailsService.loadUserByUsername(email)

        // Then
        assertNotNull(userDetails)
        assertEquals(email, userDetails.username)
        assertEquals("adminpass123", userDetails.password)
        
        val userPrincipal = userDetails as UserPrincipal
        assertEquals("Admin User", userPrincipal.getName())
        assertEquals("ADMIN", userPrincipal.getRole())
    }

    @Test
    fun `should find user by username reactively`() {
        // Given
        val email = "test@example.com"
        val user = User(
            email = email,
            password = "password123",
            name = "Test User",
            role = UserRole.MEMBER
        )
        every { userRepository.findByEmail(email) } returns java.util.Optional.of(user)

        // When
        val userDetailsMono = userDetailsService.findByUsername(email)

        // Then
        val userDetails = userDetailsMono.block()
        assertNotNull(userDetails)
        assertEquals(email, userDetails!!.username)
        assertEquals("password123", userDetails.password)
        
        val userPrincipal = userDetails as UserPrincipal
        assertEquals("Test User", userPrincipal.getName())
        assertEquals("MEMBER", userPrincipal.getRole())
    }
}