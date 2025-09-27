package com.hightemplar.vipo.service

import com.hightemplar.vipo.entity.*
import com.hightemplar.vipo.entity.Thread
import com.hightemplar.vipo.repository.ActivityLogRepository
import com.hightemplar.vipo.repository.ConversationRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Service
@Transactional
class ConversationService(
    private val conversationRepository: ConversationRepository,
    private val threadService: ThreadService,
    private val aiService: AIService,
    private val activityLogRepository: ActivityLogRepository
) {
    
    private val logger = LoggerFactory.getLogger(ConversationService::class.java)
    
    /**
     * Creates a new conversation with AI response.
     * This method handles thread management, AI response generation, and activity logging.
     * 
     * @param user the user creating the conversation
     * @param question the user's question
     * @param model the AI model to use (optional, defaults to gpt-3.5-turbo)
     * @param temperature the AI response temperature (optional, defaults to 0.7)
     * @param isStreaming whether to use streaming response (optional, defaults to false)
     * @return the created conversation with AI response
     */
    fun createConversation(
        user: User,
        question: String,
        model: String = AIService.DEFAULT_MODEL,
        temperature: Double = AIService.DEFAULT_TEMPERATURE.toDouble(),
        isStreaming: Boolean = false
    ): Mono<Conversation> {
        logger.debug("Creating conversation for user: {} with question: '{}'", user.email, question)
        
        // Validate inputs first (throws exceptions immediately)
        try {
            validateConversationInput(question, model, temperature)
        } catch (exception: ConversationValidationException) {
            return Mono.error(exception)
        }
        
        return try {
            // Get or create thread based on 30-minute rule
            val thread = threadService.getOrCreateThreadForUser(user)
            logger.debug("Using thread ID: {} for conversation", thread.id)
            
            // Generate AI response
            val aiResponseMono = if (isStreaming) {
                // For streaming, we collect all chunks into a single response
                // In a real implementation, you might want to handle streaming differently
                aiService.generateStreamingResponse(question, model, temperature)
                    .collectList()
                    .map { chunks -> chunks.joinToString("") }
            } else {
                aiService.generateResponse(question, model, temperature, isStreaming)
            }
            
            aiResponseMono.map { aiResponse ->
                // Create and save conversation
                val conversation = Conversation(
                    thread = thread,
                    question = question.trim(),
                    answer = aiResponse,
                    model = model,
                    isStreaming = isStreaming
                )
                
                val savedConversation = conversationRepository.save(conversation)
                logger.debug("Saved conversation with ID: {}", savedConversation.id)
                
                // Update thread activity timestamp
                threadService.updateThreadActivity(thread)
                
                // Log activity for analytics
                logConversationActivity(user)
                
                savedConversation
            }.onErrorMap { exception ->
                logger.error("Error creating conversation for user: {}", user.email, exception)
                ConversationCreationException("Failed to create conversation: ${exception.message}", exception)
            }
            
        } catch (exception: Exception) {
            logger.error("Error creating conversation for user: {}", user.email, exception)
            Mono.error(ConversationCreationException("Failed to create conversation: ${exception.message}", exception))
        }
    }
    
    /**
     * Creates a streaming conversation with real-time AI response.
     * 
     * @param user the user creating the conversation
     * @param question the user's question
     * @param model the AI model to use
     * @param temperature the AI response temperature
     * @return flux of response chunks
     */
    fun createStreamingConversation(
        user: User,
        question: String,
        model: String = AIService.DEFAULT_MODEL,
        temperature: Double = AIService.DEFAULT_TEMPERATURE.toDouble()
    ): Flux<String> {
        logger.debug("Creating streaming conversation for user: {} with question: '{}'", user.email, question)
        
        return try {
            // Validate inputs
            validateConversationInput(question, model, temperature)
            
            // Get or create thread
            val thread = threadService.getOrCreateThreadForUser(user)
            logger.debug("Using thread ID: {} for streaming conversation", thread.id)
            
            // Generate streaming AI response
            aiService.generateStreamingResponse(question, model, temperature)
                .doOnComplete {
                    // Save conversation after streaming is complete
                    // Note: In a real implementation, you might want to collect the response
                    // and save it to the database
                    try {
                        threadService.updateThreadActivity(thread)
                        logConversationActivity(user)
                        logger.debug("Completed streaming conversation for user: {}", user.email)
                    } catch (e: Exception) {
                        logger.warn("Error updating thread activity after streaming: {}", e.message)
                    }
                }
                .doOnError { exception ->
                    logger.error("Error in streaming conversation for user: {}", user.email, exception)
                }
        } catch (exception: Exception) {
            logger.error("Error creating streaming conversation for user: {}", user.email, exception)
            Flux.error(ConversationCreationException("Failed to create streaming conversation: ${exception.message}", exception))
        }
    }
    
    /**
     * Retrieves conversations for a specific thread with pagination.
     * 
     * @param thread the thread to get conversations for
     * @param pageable pagination information
     * @return page of conversations ordered by creation time
     */
    @Transactional(readOnly = true)
    fun getConversationsForThread(thread: Thread, pageable: Pageable): Page<Conversation> {
        logger.debug("Retrieving conversations for thread ID: {}", thread.id)
        return conversationRepository.findByThreadOrderByCreatedAtAsc(thread, pageable)
    }
    
    /**
     * Retrieves all conversations for a user across all their threads.
     * 
     * @param user the user to get conversations for
     * @param pageable pagination information
     * @return page of conversations for the user
     */
    @Transactional(readOnly = true)
    fun getConversationsForUser(user: User, pageable: Pageable): Page<Conversation> {
        logger.debug("Retrieving conversations for user: {}", user.email)
        return conversationRepository.findByThreadUser(user, pageable)
    }
    
    /**
     * Finds a conversation by ID that belongs to the specified user.
     * This method ensures users can only access their own conversations.
     * 
     * @param conversationId the ID of the conversation to find
     * @param user the user who should own the conversation
     * @return the conversation if found and owned by the user
     * @throws ConversationNotFoundException if conversation not found or not owned by user
     */
    @Transactional(readOnly = true)
    fun findConversationByIdAndUser(conversationId: Long, user: User): Conversation {
        logger.debug("Finding conversation ID: {} for user: {}", conversationId, user.email)
        return conversationRepository.findByIdAndThreadUser(conversationId, user)
            ?: throw ConversationNotFoundException("Conversation with ID $conversationId not found for user ${user.email}")
    }
    
    /**
     * Counts the total number of conversations for a user.
     * 
     * @param user the user to count conversations for
     * @return the number of conversations for the user
     */
    @Transactional(readOnly = true)
    fun countConversationsForUser(user: User): Long {
        logger.debug("Counting conversations for user: {}", user.email)
        val threads = threadService.getThreadsForUser(user, Pageable.unpaged())
        return threads.content.sumOf { thread ->
            conversationRepository.countByThread(thread)
        }
    }
    
    /**
     * Retrieves conversations created within a specific time range.
     * This is used for analytics and reporting purposes.
     * 
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @return list of conversations within the time range
     */
    @Transactional(readOnly = true)
    fun getConversationsInTimeRange(startTime: LocalDateTime, endTime: LocalDateTime): List<Conversation> {
        logger.debug("Retrieving conversations between {} and {}", startTime, endTime)
        return conversationRepository.findByCreatedAtBetweenWithUser(startTime, endTime)
    }
    
    /**
     * Counts conversations created within a specific time range.
     * 
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @return count of conversations within the time range
     */
    @Transactional(readOnly = true)
    fun countConversationsInTimeRange(startTime: LocalDateTime, endTime: LocalDateTime): Long {
        logger.debug("Counting conversations between {} and {}", startTime, endTime)
        return conversationRepository.countByCreatedAtBetween(startTime, endTime)
    }
    
    /**
     * Retrieves conversations for the last 24 hours for reporting.
     * 
     * @return list of conversations from the last 24 hours
     */
    @Transactional(readOnly = true)
    fun getConversationsLast24Hours(): List<Conversation> {
        val now = LocalDateTime.now()
        val yesterday = now.minusHours(24)
        return getConversationsInTimeRange(yesterday, now)
    }
    
    /**
     * Counts conversations created in the last 24 hours.
     * 
     * @return count of conversations from the last 24 hours
     */
    @Transactional(readOnly = true)
    fun countConversationsLast24Hours(): Long {
        val now = LocalDateTime.now()
        val yesterday = now.minusHours(24)
        return countConversationsInTimeRange(yesterday, now)
    }
    
    /**
     * Validates conversation input parameters.
     * 
     * @param question the user's question
     * @param model the AI model to use
     * @param temperature the AI response temperature
     * @throws ConversationValidationException if validation fails
     */
    private fun validateConversationInput(question: String, model: String, temperature: Double) {
        when {
            question.isBlank() -> throw ConversationValidationException("Question cannot be empty")
            question.length > 10000 -> throw ConversationValidationException("Question is too long (max 10000 characters)")
            !aiService.isModelSupported(model) -> throw ConversationValidationException("Unsupported AI model: $model")
            !aiService.isTemperatureValid(temperature) -> throw ConversationValidationException("Invalid temperature: $temperature (must be between 0.0 and 1.0)")
        }
    }
    
    /**
     * Logs conversation creation activity for analytics.
     * 
     * @param user the user who created the conversation
     */
    private fun logConversationActivity(user: User) {
        try {
            val activityLog = ActivityLog(
                user = user,
                activityType = ActivityType.CONVERSATION_CREATED
            )
            activityLogRepository.save(activityLog)
            logger.debug("Logged conversation activity for user: {}", user.email)
        } catch (exception: Exception) {
            logger.warn("Failed to log conversation activity for user: {}", user.email, exception)
            // Don't fail the conversation creation if activity logging fails
        }
    }
}

/**
 * Exception thrown when conversation creation fails.
 */
class ConversationCreationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Exception thrown when conversation validation fails.
 */
class ConversationValidationException(message: String) : RuntimeException(message)

/**
 * Exception thrown when a conversation is not found or not accessible by the user.
 */
class ConversationNotFoundException(message: String) : RuntimeException(message)