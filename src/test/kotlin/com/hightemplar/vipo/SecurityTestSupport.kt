package com.hightemplar.vipo

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@TestConfiguration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityTestSupport {
    
    @Bean
    fun testSecurityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { 
                it.requestMatchers("/api/auth/**").permitAll()
                  .anyRequest().authenticated() 
            }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .build()
}