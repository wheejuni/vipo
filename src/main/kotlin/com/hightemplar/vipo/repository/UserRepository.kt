package com.hightemplar.vipo.repository

import com.hightemplar.vipo.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, Long> {
    
    /**
     * Find user by email address for authentication purposes
     * @param email the email address to search for
     * @return Optional containing the user if found, empty otherwise
     */
    fun findByEmail(email: String): Optional<User>
    
    /**
     * Check if a user exists with the given email
     * @param email the email address to check
     * @return true if user exists, false otherwise
     */
    fun existsByEmail(email: String): Boolean
}