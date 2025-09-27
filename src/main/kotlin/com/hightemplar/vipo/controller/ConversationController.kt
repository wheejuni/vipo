package com.hightemplar.vipo.controller

import com.hightemplar.vipo.config.UserPrincipal
import com.hightemplar.vipo.dto.*
import com.hightemplar.vipo.entity.Thread
import com.hightemplar.vipo.service.ConversationService
import com.hightemplar.vipo.service.ThreadService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/conversations")
@Tag(name = "Conversations", description = "AI conversation management endpoints")
class ConversationController(
    private val conversationService: ConversationService,
    private val threadService: ThreadService
) {
    
    private val logger = LoggerFactory.getLogger(ConversationController::class.java)
    
    /**
     * Creates a new conversation with AI response.
     * POST /api/conversations
     */
    @PostMapping
    @Operation(summary = "Create a new conversation", description = "Creates a new conversation with AI response")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Conversation created successfully"),
        ApiResponse(responseCode = "400", description = "Invalid request"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun createConversation(
        @Valid @RequestBody request: ConversationRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): Mono<ResponseEntity<Any>> {
        logger.debug("Creating conversation for user: {}", userPrincipal.username)
        
        val user = userPrincipal.getUser()
        val model = request.model ?: "gpt-3.5-turbo"
        val temperature = request.temperature ?: 0.7
        val isStreaming = request.isStreaming ?: false
        
        return if (isStreaming) {
            // Return SSE response for streaming
            Mono.just(
                ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(conversationService.createStreamingConversation(
                        user = user,
                        question = request.question,
                        model = model,
                        temperature = temperature,
                    ))
            )
        } else {
            conversationService.createConversation(
                user = user,
                question = request.question,
                model = model,
                temperature = temperature,
                isStreaming = isStreaming
            ).map { conversation ->
                logger.debug("Successfully created conversation with ID: {}", conversation.id)
                ResponseEntity.status(HttpStatus.CREATED)
                    .body(ConversationResponse.fromEntity(conversation) as Any)
            }.onErrorResume { exception ->
                logger.error("Error creating conversation for user: {}", userPrincipal.username, exception)
                Mono.just(
                    ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(null)
                )
            }
        }
    }

    /**
     * Retrieves threads for the authenticated user with pagination.
     * GET /api/conversations/threads
     */
    @GetMapping("/threads")
    @Operation(summary = "Get user threads", description = "Retrieves threads for the authenticated user with pagination")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Threads retrieved successfully"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    fun getThreads(
        @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "Sort field") @RequestParam(defaultValue = "lastActivityAt") sortBy: String,
        @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "desc") sortDirection: String,
        @Parameter(description = "Include conversations in response") @RequestParam(defaultValue = "false") includeConversations: Boolean,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): Mono<ResponseEntity<PageResponse<ThreadResponse>>> {
        logger.debug("Retrieving threads for user: {} (page: {}, size: {})", userPrincipal.username, page, size)
        
        val user = userPrincipal.getUser()
        val direction = if (sortDirection.lowercase() == "asc") Sort.Direction.ASC else Sort.Direction.DESC
        val sort = Sort.by(direction, sortBy)
        val pageable = PageRequest.of(page, size, sort)
        
        return Mono.fromCallable {
            if (includeConversations) {
                threadService.getThreadsWithConversationsForUser(user, pageable)
            } else {
                threadService.getThreadsForUser(user, pageable)
            }
        }.map { threadsPage ->
            val response = PageResponse.fromPage(threadsPage) { thread ->
                ThreadResponse.fromEntity(thread, includeConversations)
            }
            
            logger.debug("Successfully retrieved {} threads for user: {}", threadsPage.totalElements, userPrincipal.username)
            ResponseEntity.ok(response)
        }.onErrorResume { exception ->
            logger.error("Error retrieving threads for user: {}", userPrincipal.username, exception)
            Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null))
        }
    }
    
    /**
     * Retrieves conversations for a specific thread.
     * GET /api/conversations/threads/{threadId}
     */
    @GetMapping("/threads/{threadId}")
    @Operation(summary = "Get conversations for thread", description = "Retrieves conversations for a specific thread")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Conversations retrieved successfully"),
        ApiResponse(responseCode = "404", description = "Thread not found"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun getConversationsForThread(
        @Parameter(description = "Thread ID") @PathVariable threadId: Long,
        @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") sortBy: String,
        @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "asc") sortDirection: String,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): Mono<ResponseEntity<PageResponse<ConversationResponse>>> {
        logger.debug("Retrieving conversations for thread: {} by user: {}", threadId, userPrincipal.username)
        
        val user = userPrincipal.getUser()
        val direction = if (sortDirection.lowercase() == "asc") Sort.Direction.ASC else Sort.Direction.DESC
        val sort = Sort.by(direction, sortBy)
        val pageable = PageRequest.of(page, size, sort)
        
        return Mono.fromCallable {
            // First verify the thread belongs to the user
            val thread = threadService.findThreadByIdAndUser(threadId, user)
            conversationService.getConversationsForThread(thread, pageable)
        }.map { conversationsPage ->
            val response = PageResponse.fromPage(conversationsPage) { conversation ->
                ConversationResponse.fromEntity(conversation)
            }
            
            logger.debug("Successfully retrieved {} conversations for thread: {}", conversationsPage.totalElements, threadId)
            ResponseEntity.ok(response)
        }.onErrorResume { exception ->
            logger.error("Error retrieving conversations for thread: {} by user: {}", threadId, userPrincipal.username, exception)
            Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null))
        }
    }
    
    /**
     * Deletes a thread and all its conversations.
     * DELETE /api/conversations/threads/{threadId}
     */
    @DeleteMapping("/threads/{threadId}")
    @Operation(summary = "Delete thread", description = "Deletes a thread and all its conversations")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Thread deleted successfully"),
        ApiResponse(responseCode = "404", description = "Thread not found"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun deleteThread(
        @Parameter(description = "Thread ID") @PathVariable threadId: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): Mono<ResponseEntity<Map<String, String>>> {
        logger.debug("Deleting thread: {} by user: {}", threadId, userPrincipal.username)
        
        val user = userPrincipal.getUser()
        
        return Mono.fromCallable {
            threadService.deleteThread(threadId, user)
        }.map {
            logger.debug("Successfully deleted thread: {} by user: {}", threadId, userPrincipal.username)
            ResponseEntity.ok(mapOf("message" to "Thread deleted successfully"))
        }.onErrorResume { exception ->
            logger.error("Error deleting thread: {} by user: {}", threadId, userPrincipal.username, exception)
            Mono.just(
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(mapOf("error" to "Thread not found or access denied"))
            )
        }
    }
    
    /**
     * Retrieves all conversations for the authenticated user.
     * GET /api/conversations
     */
    @GetMapping
    @Operation(summary = "Get all conversations", description = "Retrieves all conversations for the authenticated user")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Conversations retrieved successfully"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    )
    fun getAllConversations(
        @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") sortBy: String,
        @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "desc") sortDirection: String,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): Mono<ResponseEntity<PageResponse<ConversationResponse>>> {
        logger.debug("Retrieving all conversations for user: {} (page: {}, size: {})", userPrincipal.username, page, size)
        
        val user = userPrincipal.getUser()
        val direction = if (sortDirection.lowercase() == "asc") Sort.Direction.ASC else Sort.Direction.DESC
        val sort = Sort.by(direction, sortBy)
        val pageable = PageRequest.of(page, size, sort)
        
        return Mono.fromCallable {
            conversationService.getConversationsForUser(user, pageable)
        }.map { conversationsPage ->
            val response = PageResponse.fromPage(conversationsPage) { conversation ->
                ConversationResponse.fromEntity(conversation)
            }
            
            logger.debug("Successfully retrieved {} conversations for user: {}", conversationsPage.totalElements, userPrincipal.username)
            ResponseEntity.ok(response)
        }.onErrorResume { exception ->
            logger.error("Error retrieving conversations for user: {}", userPrincipal.username, exception)
            Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null))
        }
    }
    
    /**
     * Retrieves a specific conversation by ID.
     * GET /api/conversations/{conversationId}
     */
    @GetMapping("/{conversationId}")
    @Operation(summary = "Get conversation by ID", description = "Retrieves a specific conversation by ID")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Conversation retrieved successfully"),
        ApiResponse(responseCode = "404", description = "Conversation not found"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun getConversation(
        @Parameter(description = "Conversation ID") @PathVariable conversationId: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): Mono<ResponseEntity<ConversationResponse>> {
        logger.debug("Retrieving conversation: {} by user: {}", conversationId, userPrincipal.username)
        
        val user = userPrincipal.getUser()
        
        return Mono.fromCallable {
            conversationService.findConversationByIdAndUser(conversationId, user)
        }.map { conversation ->
            logger.debug("Successfully retrieved conversation: {}", conversationId)
            ResponseEntity.ok(ConversationResponse.fromEntity(conversation))
        }.onErrorResume { exception ->
            logger.error("Error retrieving conversation: {} by user: {}", conversationId, userPrincipal.username, exception)
            Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null))
        }
    }
    
    /**
     * Admin endpoint: Retrieves all threads in the system with pagination.
     * GET /api/conversations/admin/threads
     */
    @GetMapping("/admin/threads")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all threads (Admin)", description = "Admin endpoint to retrieve all threads in the system")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Threads retrieved successfully"),
        ApiResponse(responseCode = "403", description = "Access denied - Admin role required"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun getAllThreadsAdmin(
        @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "Sort field") @RequestParam(defaultValue = "lastActivityAt") sortBy: String,
        @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "desc") sortDirection: String,
        @Parameter(description = "Include conversations in response") @RequestParam(defaultValue = "false") includeConversations: Boolean,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): Mono<ResponseEntity<PageResponse<ThreadResponse>>> {
        logger.debug("Admin retrieving all threads (page: {}, size: {})", page, size)
        
        return Mono.fromCallable {
            // For admin, we would need a method to get all threads regardless of user
            // This would require adding a method to ThreadService
            // For now, return empty response as this functionality needs to be added
            PageResponse(
                content = emptyList<ThreadResponse>(),
                page = page,
                size = size,
                totalElements = 0L,
                totalPages = 0,
                first = true,
                last = true,
                hasNext = false,
                hasPrevious = false
            )
        }.map { response ->
            logger.debug("Admin functionality for all threads not yet implemented")
            ResponseEntity.ok(response)
        }.onErrorResume { exception ->
            logger.error("Error retrieving all threads for admin", exception)
            Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null))
        }
    }
}