package com.hightemplar.vipo.repository

import com.hightemplar.vipo.entity.User
import com.hightemplar.vipo.entity.UserRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `findByEmail should return user when email exists`() {
        // Given
        val user = User(
            email = "test@example.com",
            password = "password123",
            name = "Test User",
            role = UserRole.MEMBER
        )
        entityManager.persistAndFlush(user)

        // When
        val foundUser = userRepository.findByEmail("test@example.com")

        // Then
        assertThat(foundUser).isPresent
        assertThat(foundUser.get().email).isEqualTo("test@example.com")
        assertThat(foundUser.get().name).isEqualTo("Test User")
        assertThat(foundUser.get().role).isEqualTo(UserRole.MEMBER)
    }

    @Test
    fun `findByEmail should return empty when email does not exist`() {
        // When
        val foundUser = userRepository.findByEmail("nonexistent@example.com")

        // Then
        assertThat(foundUser).isEmpty
    }

    @Test
    fun `existsByEmail should return true when email exists`() {
        // Given
        val user = User(
            email = "existing@example.com",
            password = "password123",
            name = "Existing User",
            role = UserRole.MEMBER
        )
        entityManager.persistAndFlush(user)

        // When
        val exists = userRepository.existsByEmail("existing@example.com")

        // Then
        assertThat(exists).isTrue
    }

    @Test
    fun `existsByEmail should return false when email does not exist`() {
        // When
        val exists = userRepository.existsByEmail("nonexistent@example.com")

        // Then
        assertThat(exists).isFalse
    }

    @Test
    fun `findByEmail should be case sensitive`() {
        // Given
        val user = User(
            email = "test@example.com",
            password = "password123",
            name = "Test User",
            role = UserRole.MEMBER
        )
        entityManager.persistAndFlush(user)

        // When
        val foundUser = userRepository.findByEmail("TEST@EXAMPLE.COM")

        // Then
        assertThat(foundUser).isEmpty
    }

    @Test
    fun `should save and retrieve user with admin role`() {
        // Given
        val adminUser = User(
            email = "admin@example.com",
            password = "adminpassword",
            name = "Admin User",
            role = UserRole.ADMIN
        )
        entityManager.persistAndFlush(adminUser)

        // When
        val foundUser = userRepository.findByEmail("admin@example.com")

        // Then
        assertThat(foundUser).isPresent
        assertThat(foundUser.get().role).isEqualTo(UserRole.ADMIN)
    }
}