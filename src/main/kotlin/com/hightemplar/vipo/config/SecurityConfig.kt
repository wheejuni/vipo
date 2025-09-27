package com.hightemplar.vipo.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
    private val jwtAuthenticationWebFilter: JwtAuthenticationWebFilter
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder(12)
    }

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .authorizeExchange { exchanges ->
                exchanges
                    // Swagger UI 및 API 문서 관련 경로 허용
                    .pathMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**"
                    ).permitAll()
                    // H2 Console 허용 (dev 프로필용)
                    .pathMatchers("/h2-console/**").permitAll()
                    // Actuator health 엔드포인트 허용
                    .pathMatchers("/actuator/health").permitAll()
                    // 인증 관련 엔드포인트 허용
                    .pathMatchers("/api/auth/**").permitAll()
                    // 나머지 모든 요청은 인증 필요
                    .anyExchange().authenticated()
            }
            // JWT 인증 필터 추가
            .addFilterBefore(jwtAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build()
    }
}