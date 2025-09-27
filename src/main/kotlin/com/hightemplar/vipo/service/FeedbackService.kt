package com.hightemplar.vipo.service

import com.hightemplar.vipo.dto.FeedbackRequest
import com.hightemplar.vipo.dto.FeedbackResponse
import com.hightemplar.vipo.entity.*
import com.hightemplar.vipo.exception.DuplicateFeedbackException
import com.hightemplar.vipo.exception.FeedbackNotFoundException
import com.hightemplar.vipo.repository.ConversationRepository
import com.hightemplar.vipo.repository.FeedbackRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class FeedbackService(
    private val feedbackRepository: FeedbackRepository,
    private val conversationRepository: ConversationRepository
) {
    
    /**
     * Create feedback for a conversation
     * Members can only create feedback for their own conversations
     * Admins can create feedback for any conversation
     */
    fun createFeedback(request: FeedbackRequest, user: User): FeedbackResponse {
        // Find the conversation
        val conversation = conversationRepository.findById(request.conversationId)
            .orElseThrow { ConversationNotFoundException("Conversation not found with id: ${request.conversationId}") }
        
        // Check if user can create feedback for this conversation
        if (user.role == UserRole.MEMBER) {
            // Members can only create feedback for their own conversations
            val userConversation = conversationRepository.findByIdAndThreadUser(request.conversationId, user)
            if (userConversation == null) {
                throw AccessDeniedException("You can only create feedback for your own conversations")
            }
        }
        
        // Check if feedback already exists for this user and conversation
        if (feedbackRepository.existsByUserAndConversation(user, conversation)) {
            throw DuplicateFeedbackException("Feedback already exists for this conversation")
        }
        
        // Create and save feedback
        val feedback = Feedback(
            user = user,
            conversation = conversation,
            isPositive = request.isPositive
        )
        
        val savedFeedback = feedbackRepository.save(feedback)
        return FeedbackResponse.fromEntity(savedFeedback)
    }
    
    /**
     * Get feedback list with user-specific or admin filtering
     * Members see only their own feedback
     * Admins see all feedback
     */
    @Transactional(readOnly = true)
    fun getFeedback(user: User, status: FeedbackStatus?, pageable: Pageable): Page<FeedbackResponse> {
        val feedbackPage = when (user.role) {
            UserRole.MEMBER -> {
                if (status != null) {
                    feedbackRepository.findByUserAndStatus(user, status, pageable)
                } else {
                    feedbackRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                }
            }
            UserRole.ADMIN -> {
                if (status != null) {
                    feedbackRepository.findByStatus(status, pageable)
                } else {
                    feedbackRepository.findAllWithUserAndConversation(pageable)
                }
            }
        }
        
        return feedbackPage.map { FeedbackResponse.fromEntity(it) }
    }
    
    /**
     * Update feedback status (admin only)
     */
    fun updateFeedbackStatus(feedbackId: Long, newStatus: FeedbackStatus, user: User): FeedbackResponse {
        // Only admins can update feedback status
        if (user.role != UserRole.ADMIN) {
            throw AccessDeniedException("Only administrators can update feedback status")
        }
        
        val feedback = feedbackRepository.findById(feedbackId)
            .orElseThrow { FeedbackNotFoundException("Feedback not found with id: $feedbackId") }
        
        feedback.status = newStatus
        val updatedFeedback = feedbackRepository.save(feedback)
        
        return FeedbackResponse.fromEntity(updatedFeedback)
    }
    
    /**
     * Get feedback by ID with proper authorization
     * Members can only access their own feedback
     * Admins can access any feedback
     */
    @Transactional(readOnly = true)
    fun getFeedbackById(feedbackId: Long, user: User): FeedbackResponse {
        val feedback = when (user.role) {
            UserRole.MEMBER -> {
                feedbackRepository.findByIdAndUser(feedbackId, user)
                    ?: throw FeedbackNotFoundException("Feedback not found or access denied")
            }
            UserRole.ADMIN -> {
                feedbackRepository.findById(feedbackId)
                    .orElseThrow { FeedbackNotFoundException("Feedback not found with id: $feedbackId") }
            }
        }
        
        return FeedbackResponse.fromEntity(feedback)
    }
    
    /**
     * Check if user can create feedback for a conversation
     */
    @Transactional(readOnly = true)
    fun canCreateFeedback(conversationId: Long, user: User): Boolean {
        val conversation = conversationRepository.findById(conversationId)
            .orElse(null) ?: return false
        
        // Check if feedback already exists
        if (feedbackRepository.existsByUserAndConversation(user, conversation)) {
            return false
        }
        
        // Check access rights
        return when (user.role) {
            UserRole.ADMIN -> true
            UserRole.MEMBER -> conversationRepository.findByIdAndThreadUser(conversationId, user) != null
        }
    }
}