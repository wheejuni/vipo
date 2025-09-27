package com.hightemplar.vipo.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    
    @Value("\${app.version:1.0.0}")
    private lateinit var appVersion: String
    
    @Value("\${server.port:8080}")
    private lateinit var serverPort: String
    
    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("VIPO AI Chat Platform API")
                    .description("""
                        A comprehensive AI-powered chat platform with conversation management, 
                        user authentication, analytics, feedback system, and RAG capabilities for internal documents.
                        
                        ## Features
                        - **Conversations**: Create and manage AI conversations with streaming support
                        - **Authentication**: JWT-based user authentication and authorization
                        - **Analytics**: Comprehensive analytics and reporting
                        - **Feedback**: User feedback collection and management
                        - **RAG**: Retrieval-Augmented Generation for internal documents
                        - **Real-time**: Server-Sent Events (SSE) for streaming responses
                        
                        ## Authentication
                        Most endpoints require authentication. Include the JWT token in the Authorization header:
                        ```
                        Authorization: Bearer <your-jwt-token>
                        ```
                        
                        ## Rate Limiting
                        API requests are rate-limited to ensure fair usage and system stability.
                    """.trimIndent())
                    .version(appVersion)
                    .contact(
                        Contact()
                            .name("VIPO Development Team")
                            .email("dev@sionicai.com")
                            .url("https://sionicai.com")
                    )
                    .license(
                        License()
                            .name("Proprietary")
                            .url("https://sionicai.com/license")
                    )
            )
            .servers(
                listOf(
                    Server()
                        .url("http://localhost:$serverPort")
                        .description("Development server"),
                    Server()
                        .url("https://api.vipo.sionicai.com")
                        .description("Production server")
                )
            )
            .addSecurityItem(
                SecurityRequirement().addList("bearerAuth")
            )
            .components(
                io.swagger.v3.oas.models.Components()
                    .addSecuritySchemes(
                        "bearerAuth",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("JWT token for authentication")
                    )
            )
    }
}