package com.hightemplar.vipo.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class UserLoginRequest(
    @field:Email(message = "Email must be valid")
    @field:NotBlank(message = "Email is required")
    val email: String,
    
    @field:NotBlank(message = "Password is required")
    val password: String
)