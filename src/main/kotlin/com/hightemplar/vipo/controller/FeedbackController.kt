package com.hightemplar.vipo.controller

import com.hightemplar.vipo.config.UserPrincipal
import com.hightemplar.vipo.dto.*
import com.hightemplar.vipo.entity.FeedbackStatus
import com.hightemplar.vipo.service.FeedbackService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/feedback")
class FeedbackController(
    private val feedbackService: FeedbackService
) {
    
    private val logger = LoggerFactory.getLogger(FeedbackController::class.java)
    
    /**
     * Creates feedback for a conversation.
     * POST /api/feedback
     */
    @PostMapping
    fun createFeedback(
        @Valid @RequestBody request: FeedbackRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<FeedbackResponse> {
        logger.debug("Creating feedback for conversation: {} by user: {}", request.conversationId, userPrincipal.username)
        
        val user = userPrincipal.getUser()
        
        return try {
            val feedback = feedbackService.createFeedback(request, user)
            
            logger.debug("Successfully created feedback with ID: {} for conversation: {}", feedback.id, request.conversationId)
            ResponseEntity.status(HttpStatus.CREATED).body(feedback)
            
        } catch (exception: Exception) {
            logger.error("Error creating feedback for conversation: {} by user: {}", request.conversationId, userPrincipal.username, exception)
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null)
        }
    }
    
    /**
     * Retrieves feedback list with user-specific or admin filtering.
     * Members see only their own feedback, admins see all feedback.
     * GET /api/feedback
     */
    @GetMapping
    fun getFeedback(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDirection: String,
        @RequestParam(required = false) status: FeedbackStatus?,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<PageResponse<FeedbackResponse>> {
        logger.debug("Retrieving feedback for user: {} (page: {}, size: {}, status: {})", 
            userPrincipal.username, page, size, status)
        
        val user = userPrincipal.getUser()
        val direction = if (sortDirection.lowercase() == "asc") Sort.Direction.ASC else Sort.Direction.DESC
        val sort = Sort.by(direction, sortBy)
        val pageable = PageRequest.of(page, size, sort)
        
        return try {
            val feedbackPage = feedbackService.getFeedback(user, status, pageable)
            
            val response = PageResponse.fromPage(feedbackPage) { it }
            
            logger.debug("Successfully retrieved {} feedback items for user: {}", feedbackPage.totalElements, userPrincipal.username)
            ResponseEntity.ok(response)
            
        } catch (exception: Exception) {
            logger.error("Error retrieving feedback for user: {}", userPrincipal.username, exception)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null)
        }
    }
    
    /**
     * Retrieves a specific feedback by ID.
     * Members can only access their own feedback, admins can access any feedback.
     * GET /api/feedback/{feedbackId}
     */
    @GetMapping("/{feedbackId}")
    fun getFeedbackById(
        @PathVariable feedbackId: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<FeedbackResponse> {
        logger.debug("Retrieving feedback: {} by user: {}", feedbackId, userPrincipal.username)
        
        val user = userPrincipal.getUser()
        
        return try {
            val feedback = feedbackService.getFeedbackById(feedbackId, user)
            
            logger.debug("Successfully retrieved feedback: {}", feedbackId)
            ResponseEntity.ok(feedback)
            
        } catch (exception: Exception) {
            logger.error("Error retrieving feedback: {} by user: {}", feedbackId, userPrincipal.username, exception)
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
        }
    }
    
    /**
     * Updates feedback status (admin only).
     * PUT /api/feedback/{feedbackId}/status
     */
    @PutMapping("/{feedbackId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateFeedbackStatus(
        @PathVariable feedbackId: Long,
        @Valid @RequestBody request: FeedbackStatusUpdateRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<FeedbackResponse> {
        logger.debug("Updating feedback status: {} to {} by admin: {}", feedbackId, request.status, userPrincipal.username)
        
        val user = userPrincipal.getUser()
        
        return try {
            val updatedFeedback = feedbackService.updateFeedbackStatus(feedbackId, request.status, user)
            
            logger.debug("Successfully updated feedback: {} status to {}", feedbackId, request.status)
            ResponseEntity.ok(updatedFeedback)
            
        } catch (exception: Exception) {
            logger.error("Error updating feedback status: {} by admin: {}", feedbackId, userPrincipal.username, exception)
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null)
        }
    }
    
    /**
     * Checks if user can create feedback for a conversation.
     * GET /api/feedback/can-create/{conversationId}
     */
    @GetMapping("/can-create/{conversationId}")
    fun canCreateFeedback(
        @PathVariable conversationId: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<Map<String, Boolean>> {
        logger.debug("Checking if user: {} can create feedback for conversation: {}", userPrincipal.username, conversationId)
        
        val user = userPrincipal.getUser()
        
        return try {
            val canCreate = feedbackService.canCreateFeedback(conversationId, user)
            
            logger.debug("User: {} can create feedback for conversation: {} = {}", userPrincipal.username, conversationId, canCreate)
            ResponseEntity.ok(mapOf("canCreate" to canCreate))
            
        } catch (exception: Exception) {
            logger.error("Error checking feedback creation permission for conversation: {} by user: {}", 
                conversationId, userPrincipal.username, exception)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("canCreate" to false))
        }
    }
}