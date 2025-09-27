package com.hightemplar.vipo.dto

data class AuthResponse(
    val token: String,
    val email: String,
    val name: String,
    val role: String
)