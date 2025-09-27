package com.hightemplar.vipo.config

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider {

    @Value("\${app.jwt.secret}")
    private lateinit var jwtSecret: String

    @Value("\${app.jwt.expiration}")
    private var jwtExpirationInMs: Long = 0

    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    }

    fun generateToken(authentication: Authentication): String {
        val userPrincipal = authentication.principal as UserDetails
        val expiryDate = Date(Date().time + jwtExpirationInMs)

        return Jwts.builder()
            .subject(userPrincipal.username)
            .issuedAt(Date())
            .expiration(expiryDate)
            .signWith(key)
            .compact()
    }

    fun generateToken(email: String, role: String): String {
        val expiryDate = Date(Date().time + jwtExpirationInMs)

        return Jwts.builder()
            .subject(email)
            .claim("role", role)
            .issuedAt(Date())
            .expiration(expiryDate)
            .signWith(key)
            .compact()
    }

    fun getUsernameFromToken(token: String): String {
        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload

        return claims.subject
    }

    fun getRoleFromToken(token: String): String? {
        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload

        return claims["role"] as String?
    }

    fun validateToken(authToken: String): Boolean {
        try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(authToken)
            return true
        } catch (ex: SecurityException) {
            // Invalid JWT signature
        } catch (ex: MalformedJwtException) {
            // Invalid JWT token
        } catch (ex: ExpiredJwtException) {
            // Expired JWT token
        } catch (ex: UnsupportedJwtException) {
            // Unsupported JWT token
        } catch (ex: IllegalArgumentException) {
            // JWT claims string is empty
        }
        return false
    }

    fun getExpirationDateFromToken(token: String): Date {
        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload

        return claims.expiration
    }

    fun isTokenExpired(token: String): Boolean {
        val expiration = getExpirationDateFromToken(token)
        return expiration.before(Date())
    }
}