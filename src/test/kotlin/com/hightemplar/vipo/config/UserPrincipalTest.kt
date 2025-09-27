package com.hightemplar.vipo.config

import com.hightemplar.vipo.entity.User
import com.hightemplar.vipo.entity.UserRole
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserPrincipalTest {

    @Test
    fun `should create UserPrincipal with member role`() {
        // Given
        val user = User(
            email = "member@example.com",
            password = "password123",
            name = "Member User",
            role = UserRole.MEMBER
        )

        // When
        val userPrincipal = UserPrincipal(user)

        // Then
        assertEquals("member@example.com", userPrincipal.username)
        assertEquals("password123", userPrincipal.password)
        assertEquals("Member User", userPrincipal.getName())
        assertEquals("MEMBER", userPrincipal.getRole())
        assertTrue(userPrincipal.isAccountNonExpired)
        assertTrue(userPrincipal.isAccountNonLocked)
        assertTrue(userPrincipal.isCredentialsNonExpired)
        assertTrue(userPrincipal.isEnabled)
        
        val authorities = userPrincipal.authorities
        assertEquals(1, authorities.size)
        assertTrue(authorities.contains(SimpleGrantedAuthority("ROLE_MEMBER")))
    }

    @Test
    fun `should create UserPrincipal with admin role`() {
        // Given
        val user = User(
            email = "admin@example.com",
            password = "password123",
            name = "Admin User",
            role = UserRole.ADMIN
        )

        // When
        val userPrincipal = UserPrincipal(user)

        // Then
        assertEquals("admin@example.com", userPrincipal.username)
        assertEquals("password123", userPrincipal.password)
        assertEquals("Admin User", userPrincipal.getName())
        assertEquals("ADMIN", userPrincipal.getRole())
        
        val authorities = userPrincipal.authorities
        assertEquals(1, authorities.size)
        assertTrue(authorities.contains(SimpleGrantedAuthority("ROLE_ADMIN")))
    }

    @Test
    fun `should return user object`() {
        // Given
        val user = User(
            email = "test@example.com",
            password = "password123",
            name = "Test User",
            role = UserRole.MEMBER
        )

        // When
        val userPrincipal = UserPrincipal(user)

        // Then
        assertEquals(user, userPrincipal.getUser())
    }
}