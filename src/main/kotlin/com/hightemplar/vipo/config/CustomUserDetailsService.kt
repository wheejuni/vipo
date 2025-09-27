package com.hightemplar.vipo.config

import com.hightemplar.vipo.repository.UserRepository

import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService, ReactiveUserDetailsService {

    override fun loadUserByUsername(email: String): UserDetails {
        val user = userRepository.findByEmail(email)
            .orElseThrow { UsernameNotFoundException("User not found with email: $email") }
        
        return UserPrincipal(user)
    }

    override fun findByUsername(email: String): Mono<UserDetails> {
        return Mono.fromCallable {
            val user = userRepository.findByEmail(email)
                .orElseThrow { UsernameNotFoundException("User not found with email: $email") }
            UserPrincipal(user)
        }
    }
}