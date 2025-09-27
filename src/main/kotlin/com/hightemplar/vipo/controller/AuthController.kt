package com.hightemplar.vipo.controller

import com.hightemplar.vipo.dto.AuthResponse
import com.hightemplar.vipo.dto.UserLoginRequest
import com.hightemplar.vipo.dto.UserRegistrationRequest
import com.hightemplar.vipo.service.UserService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userService: UserService
) {
    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    /**
     * Register a new user
     * POST /api/auth/register
     */
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: UserRegistrationRequest): ResponseEntity<AuthResponse> {
        logger.debug("Registration request for email: {}", request.email)
        
        val response = userService.registerUser(request)
        
        logger.info("User registered successfully: {}", response.email)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * Login user
     * POST /api/auth/login
     */
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: UserLoginRequest): ResponseEntity<AuthResponse> {
        logger.debug("Login request for email: {}", request.email)
        
        val response = userService.loginUser(request)
        
        logger.info("User logged in successfully: {}", response.email)
        return ResponseEntity.ok(response)
    }
}