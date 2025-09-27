package com.hightemplar.vipo.service

import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class AIService(
    private val chatClient: ChatClient
) {
    
    private val logger = LoggerFactory.getLogger(AIService::class.java)
    
    companion object {
        const val DEFAULT_MODEL = "gpt-3.5-turbo"
        const val DEFAULT_TEMPERATURE = 0.7f
        const val FALLBACK_RESPONSE = "I apologize, but I'm currently unable to process your request. Please try again later."
    }
    
    /**
     * Generates an AI response for the given question using the specified model and options.
     * 
     * @param question the user's question
     * @param model the AI model to use (defaults to gpt-3.5-turbo)
     * @param temperature the creativity/randomness of the response (0.0 to 1.0)
     * @param isStreaming whether to use streaming response (not implemented in this version)
     * @return the AI-generated response
     */
    fun generateResponse(
        question: String,
        model: String = DEFAULT_MODEL,
        temperature: Double = DEFAULT_TEMPERATURE.toDouble(),
        isStreaming: Boolean = false
    ): Mono<String> {
        return try {
            logger.debug("Generating AI response for question: '{}' using model: '{}'", question, model)
            
            val options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .build()
            
            val response = chatClient.prompt()
                .user(question)
                .options(options)
                .call()
                .content()
            
            logger.debug("Successfully generated AI response")
            Mono.just(response ?: FALLBACK_RESPONSE)
            
        } catch (exception: Exception) {
            logger.error("Error generating AI response for question: '{}'", question, exception)
            handleAIServiceError(exception)
        }
    }
    
    /**
     * Generates a streaming AI response for the given question.
     * This method returns a Flux of response chunks for real-time streaming.
     * 
     * @param question the user's question
     * @param model the AI model to use
     * @param temperature the creativity/randomness of the response
     * @return a Flux of response chunks
     */
    fun generateStreamingResponse(
        question: String,
        model: String = DEFAULT_MODEL,
        temperature: Double = DEFAULT_TEMPERATURE.toDouble()
    ): Flux<String> {
        return try {
            logger.debug("Generating streaming AI response for question: '{}' using model: '{}'", question, model)
            
            val options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .build()
            
            chatClient.prompt()
                .user(question)
                .options(options)
                .stream()
                .content()
                .doOnNext { chunk ->
                    logger.trace("Received streaming chunk: '{}'", chunk)
                }
                .doOnComplete {
                    logger.debug("Completed streaming AI response")
                }
                .onErrorResume { exception ->
                    logger.error("Error in streaming AI response for question: '{}'", question, exception)
                    Flux.just(FALLBACK_RESPONSE)
                }
            
        } catch (exception: Exception) {
            logger.error("Error setting up streaming AI response for question: '{}'", question, exception)
            Flux.just(FALLBACK_RESPONSE)
        }
    }
    
    /**
     * Validates if the specified model is supported.
     * 
     * @param model the model name to validate
     * @return true if the model is supported, false otherwise
     */
    fun isModelSupported(model: String): Boolean {
        val supportedModels = setOf(
            "gpt-3.5-turbo",
            "gpt-3.5-turbo-16k",
            "gpt-4",
            "gpt-4-turbo",
            "gpt-4o",
            "gpt-4o-mini"
        )
        return supportedModels.contains(model)
    }
    
    /**
     * Validates the temperature parameter.
     * 
     * @param temperature the temperature value to validate
     * @return true if valid (between 0.0 and 1.0), false otherwise
     */
    fun isTemperatureValid(temperature: Double): Boolean {
        return temperature in 0.0..1.0
    }
    
    /**
     * Handles AI service errors and returns appropriate fallback responses.
     * 
     * @param exception the exception that occurred
     * @return a Mono with fallback response
     */
    private fun handleAIServiceError(exception: Exception): Mono<String> {
        return when (exception) {
            is org.springframework.web.reactive.function.client.WebClientResponseException -> {
                logger.error("AI service HTTP error: {} - {}", exception.statusCode, exception.responseBodyAsString)
                Mono.just(FALLBACK_RESPONSE)
            }
            is java.net.ConnectException -> {
                logger.error("AI service connection error", exception)
                Mono.just(FALLBACK_RESPONSE)
            }
            is java.util.concurrent.TimeoutException -> {
                logger.warn("AI service timeout, using fallback response", exception)
                Mono.just(FALLBACK_RESPONSE)
            }
            else -> {
                logger.error("Unexpected AI service error", exception)
                Mono.just(FALLBACK_RESPONSE)
            }
        }
    }
    
    /**
     * Creates a prompt with system context for better AI responses.
     * This can be used for more sophisticated prompt engineering.
     * 
     * @param question the user's question
     * @param systemContext optional system context to guide the AI
     * @return formatted prompt
     */
    fun createPromptWithContext(question: String, systemContext: String? = null): String {
        return if (systemContext != null) {
            "System: $systemContext\n\nUser: $question"
        } else {
            question
        }
    }
    
    /**
     * Generates a response with custom system context.
     * 
     * @param question the user's question
     * @param systemContext system context to guide the AI response
     * @param model the AI model to use
     * @param temperature the creativity/randomness of the response
     * @return the AI-generated response
     */
    fun generateResponseWithContext(
        question: String,
        systemContext: String,
        model: String = DEFAULT_MODEL,
        temperature: Double = DEFAULT_TEMPERATURE.toDouble()
    ): Mono<String> {
        val promptWithContext = createPromptWithContext(question, systemContext)
        return generateResponse(promptWithContext, model, temperature, false)
    }
}