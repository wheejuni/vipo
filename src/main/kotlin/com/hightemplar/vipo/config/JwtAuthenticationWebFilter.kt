package com.hightemplar.vipo.config

import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class JwtAuthenticationWebFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userDetailsService: ReactiveUserDetailsService
) : WebFilter {

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val token = getJwtFromRequest(exchange)
        
        return if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token!!)) {
            val username = jwtTokenProvider.getUsernameFromToken(token)
            
            userDetailsService.findByUsername(username)
                .cast(UserPrincipal::class.java)
                .map { userDetails ->
                    UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.authorities
                    )
                }
                .flatMap { authentication ->
                    chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                }
                .onErrorResume { 
                    // Log error and continue without authentication
                    chain.filter(exchange)
                }
        } else {
            chain.filter(exchange)
        }
    }

    private fun getJwtFromRequest(exchange: ServerWebExchange): String? {
        val bearerToken = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        return if (StringUtils.hasText(bearerToken) && bearerToken!!.startsWith(BEARER_PREFIX)) {
            bearerToken.substring(BEARER_PREFIX.length)
        } else {
            null
        }
    }
}