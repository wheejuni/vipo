package com.hightemplar.vipo.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

/**
 * Validates critical application configuration on startup
 */
@Component
class ConfigurationValidator(
    @Value("\${app.jwt.secret}")
    private val jwtSecret: String,
    
    @Value("\${spring.ai.openai.api-key}")
    private val openaiApiKey: String,
    
    @Value("\${spring.datasource.url}")
    private val databaseUrl: String,
    
    @Value("\${app.cors.allowed-origins}")
    private val corsAllowedOrigins: String,
    
    @Value("\${spring.profiles.active:default}")
    private val activeProfiles: String
) {
    
    private val logger = LoggerFactory.getLogger(ConfigurationValidator::class.java)
    
    @EventListener(ApplicationReadyEvent::class)
    fun validateConfiguration() {
        logger.info("Validating application configuration...")
        
        val validationErrors = mutableListOf<String>()
        
        // Validate JWT secret
        validateJwtSecret(validationErrors)
        
        // Validate OpenAI API key
//        validateOpenAiApiKey(validationErrors)
        
        // Validate database configuration
        validateDatabaseConfiguration(validationErrors)
        
        // Validate CORS configuration for production
        validateCorsConfiguration(validationErrors)
        
        if (validationErrors.isNotEmpty()) {
            logger.error("Configuration validation failed:")
            validationErrors.forEach { error ->
                logger.error("  - $error")
            }
            throw IllegalStateException("Application configuration is invalid. See logs for details.")
        }
        
        logger.info("Configuration validation completed successfully")
        logConfigurationSummary()
    }
    
    private fun validateJwtSecret(errors: MutableList<String>) {
        when {
            jwtSecret.isBlank() -> {
                errors.add("JWT secret is not configured")
            }
            jwtSecret.toByteArray(StandardCharsets.UTF_8).size < 32 -> {
                errors.add("JWT secret must be at least 256 bits (32 characters) long")
            }
            jwtSecret.contains("mySecretKey") || jwtSecret.contains("dev-secret") -> {
                if (activeProfiles.contains("prod")) {
                    errors.add("Default JWT secret detected in production environment")
                } else {
                    logger.warn("Using default JWT secret - this is only acceptable in development")
                }
            }
        }
    }
    
    private fun validateOpenAiApiKey(errors: MutableList<String>) {
        when {
            openaiApiKey.isBlank() -> {
                errors.add("OpenAI API key is not configured")
            }
            openaiApiKey.contains("your-openai-api-key") -> {
                errors.add("Default OpenAI API key placeholder detected - please configure a real API key")
            }
            !openaiApiKey.startsWith("sk-") -> {
                errors.add("OpenAI API key format appears invalid (should start with 'sk-')")
            }
        }
    }
    
    private fun validateDatabaseConfiguration(errors: MutableList<String>) {
        when {
            databaseUrl.isBlank() -> {
                errors.add("Database URL is not configured")
            }
            !databaseUrl.startsWith("jdbc:") -> {
                errors.add("Database URL format appears invalid (should start with 'jdbc:')")
            }
            databaseUrl.contains("localhost") && activeProfiles.contains("prod") -> {
                logger.warn("Database URL contains 'localhost' in production environment")
            }
        }
    }
    
    private fun validateCorsConfiguration(errors: MutableList<String>) {
        if (activeProfiles.contains("prod")) {
            when {
                corsAllowedOrigins.contains("localhost") -> {
                    errors.add("CORS configuration allows localhost origins in production")
                }
                corsAllowedOrigins.contains("*") -> {
                    errors.add("CORS configuration allows all origins (*) in production")
                }
                corsAllowedOrigins.isBlank() -> {
                    errors.add("CORS allowed origins not configured for production")
                }
            }
        }
    }
    
    private fun logConfigurationSummary() {
        logger.info("Configuration Summary:")
        logger.info("  Active Profiles: $activeProfiles")
        logger.info("  Database URL: ${maskSensitiveUrl(databaseUrl)}")
        logger.info("  JWT Secret Length: ${jwtSecret.length} characters")
        logger.info("  OpenAI API Key: ${maskApiKey(openaiApiKey)}")
        logger.info("  CORS Allowed Origins: $corsAllowedOrigins")
    }
    
    private fun maskSensitiveUrl(url: String): String {
        return url.replace(Regex("://([^:]+):([^@]+)@"), "://***:***@")
    }
    
    private fun maskApiKey(apiKey: String): String {
        return if (apiKey.length > 8) {
            "${apiKey.take(4)}...${apiKey.takeLast(4)}"
        } else {
            "***"
        }
    }
}