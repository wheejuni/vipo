package com.hightemplar.vipo.service

import com.hightemplar.vipo.config.JwtTokenProvider
import com.hightemplar.vipo.dto.AuthResponse
import com.hightemplar.vipo.dto.UserLoginRequest
import com.hightemplar.vipo.dto.UserRegistrationRequest
import com.hightemplar.vipo.entity.ActivityLog
import com.hightemplar.vipo.entity.ActivityType
import com.hightemplar.vipo.entity.User
import com.hightemplar.vipo.entity.UserRole
import com.hightemplar.vipo.exception.InvalidCredentialsException
import com.hightemplar.vipo.exception.UserAlreadyExistsException
import com.hightemplar.vipo.repository.ActivityLogRepository
import com.hightemplar.vipo.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val activityLogRepository: ActivityLogRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider
) {

    /**
     * Register a new user with email, password, and name
     * @param request the registration request containing user details
     * @return AuthResponse with JWT token and user information
     * @throws UserAlreadyExistsException if email already exists
     */
    fun registerUser(request: UserRegistrationRequest): AuthResponse {
        // Check if user already exists
        if (userRepository.existsByEmail(request.email)) {
            throw UserAlreadyExistsException("User with email ${request.email} already exists")
        }

        // Create new user with encrypted password
        val user = User(
            email = request.email,
            password = passwordEncoder.encode(request.password),
            name = request.name,
            role = UserRole.MEMBER
        )

        // Save user to database
        val savedUser = userRepository.save(user)

        // Log registration activity
        logActivity(savedUser, ActivityType.USER_REGISTRATION)

        // Generate JWT token
        val token = jwtTokenProvider.generateToken(savedUser.email, savedUser.role.name)

        return AuthResponse(
            token = token,
            email = savedUser.email,
            name = savedUser.name,
            role = savedUser.role.name
        )
    }

    /**
     * Authenticate user with email and password
     * @param request the login request containing credentials
     * @return AuthResponse with JWT token and user information
     * @throws InvalidCredentialsException if credentials are invalid
     */
    fun loginUser(request: UserLoginRequest): AuthResponse {
        // Find user by email
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { InvalidCredentialsException("Invalid email or password") }

        // Verify password
        if (!passwordEncoder.matches(request.password, user.password)) {
            throw InvalidCredentialsException("Invalid email or password")
        }

        // Log login activity
        logActivity(user, ActivityType.USER_LOGIN)

        // Generate JWT token
        val token = jwtTokenProvider.generateToken(user.email, user.role.name)

        return AuthResponse(
            token = token,
            email = user.email,
            name = user.name,
            role = user.role.name
        )
    }

    /**
     * Find user by email
     * @param email the email to search for
     * @return User if found, null otherwise
     */
    @Transactional(readOnly = true)
    fun findByEmail(email: String): User? {
        return userRepository.findByEmail(email).orElse(null)
    }

    /**
     * Check if user exists by email
     * @param email the email to check
     * @return true if user exists, false otherwise
     */
    @Transactional(readOnly = true)
    fun existsByEmail(email: String): Boolean {
        return userRepository.existsByEmail(email)
    }

    /**
     * Log user activity
     * @param user the user performing the activity
     * @param activityType the type of activity
     */
    private fun logActivity(user: User, activityType: ActivityType) {
        val activityLog = ActivityLog(
            user = user,
            activityType = activityType
        )
        activityLogRepository.save(activityLog)
    }
}