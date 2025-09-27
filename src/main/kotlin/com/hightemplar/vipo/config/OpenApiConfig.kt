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
                        대화 관리, 사용자 인증, 분석, 피드백 시스템, 내부 문서용 RAG 기능을 갖춘 포괄적인 AI 기반 채팅 플랫폼입니다.
                        
                        ## 주요 기능
                        - **대화**: 스트리밍 지원을 통한 AI 대화 생성 및 관리
                        - **인증**: JWT 기반 사용자 인증 및 권한 관리
                        - **분석**: 포괄적인 분석 및 리포팅
                        - **피드백**: 사용자 피드백 수집 및 관리
                        - **RAG**: 내부 문서용 검색 증강 생성
                        - **실시간**: 스트리밍 응답을 위한 서버 전송 이벤트(SSE)
                        
                        ## 인증
                        대부분의 엔드포인트는 인증이 필요합니다. Authorization 헤더에 JWT 토큰을 포함하세요:
                        ```
                        Authorization: Bearer <your-jwt-token>
                        ```
                        
                        ## 요청 제한
                        API 요청은 공정한 사용과 시스템 안정성을 위해 제한됩니다.
                    """.trimIndent())
                    .version(appVersion)
                    .contact(
                        Contact()
                            .name("VIPO Development Team")
                            .email("dev@hightemplar.com")
                            .url("https://hightemplar.com")
                    )
                    .license(
                        License()
                            .name("Proprietary")
                            .url("https://hightemplar.com/license")
                    )
            )
            .servers(
                listOf(
                    Server()
                        .url("http://localhost:$serverPort")
                        .description("개발 서버"),
                    Server()
                        .url("https://api.vipo.hightemplar.com")
                        .description("운영 서버")
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
                            .description("인증을 위한 JWT 토큰")
                    )
            )
    }
}