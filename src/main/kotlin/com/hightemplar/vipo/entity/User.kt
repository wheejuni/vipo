package com.hightemplar.vipo.entity

import jakarta.persistence.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Entity
@Table(name = "users")
data class User(
    @Column(unique = true, nullable = false)
    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    val email: String,
    
    @Column(nullable = false)
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    val password: String,
    
    @Column(nullable = false)
    @NotBlank(message = "Name is required")
    val name: String,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: UserRole = UserRole.MEMBER
) : BaseEntity()