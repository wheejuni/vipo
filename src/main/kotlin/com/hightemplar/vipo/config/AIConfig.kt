package com.hightemplar.vipo.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AIConfig {
    
    @Bean
    fun chatClient(openAiChatModel: OpenAiChatModel): ChatClient {
        return ChatClient.builder(openAiChatModel).build()
    }
}