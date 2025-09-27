package com.hightemplar.vipo.repository

import com.hightemplar.vipo.entity.Conversation
import com.hightemplar.vipo.entity.Feedback
import com.hightemplar.vipo.entity.FeedbackStatus
import com.hightemplar.vipo.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface FeedbackRepository : JpaRepository<Feedback, Long> {
    
    /**
     * Find all feedback submitted by a specific user
     * @param user the user to find feedback for
     * @param pageable pagination information
     * @return page of feedback submitted by the user
     */
    fun findByUser(user: User, pageable: Pageable): Page<Feedback>
    
    /**
     * Find all feedback submitted by a specific user ordered by creation time
     * @param user the user to find feedback for
     * @param pageable pagination information
     * @return page of feedback ordered by creation time (most recent first)
     */
    fun findByUserOrderByCreatedAtDesc(user: User, pageable: Pageable): Page<Feedback>
    
    /**
     * Find all feedback with a specific status (for admin queries)
     * @param status the feedback status to filter by
     * @param pageable pagination information
     * @return page of feedback with the specified status
     */
    fun findByStatus(status: FeedbackStatus, pageable: Pageable): Page<Feedback>
    
    /**
     * Find all feedback ordered by creation time (for admin queries)
     * @param pageable pagination information
     * @return page of all feedback ordered by creation time (most recent first)
     */
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<Feedback>
    
    /**
     * Find feedback by user and status
     * @param user the user to find feedback for
     * @param status the feedback status to filter by
     * @param pageable pagination information
     * @return page of feedback matching user and status
     */
    fun findByUserAndStatus(user: User, status: FeedbackStatus, pageable: Pageable): Page<Feedback>
    
    /**
     * Find feedback for a specific conversation
     * @param conversation the conversation to find feedback for
     * @return list of feedback for the conversation
     */
    fun findByConversation(conversation: Conversation): List<Feedback>
    
    /**
     * Check if feedback exists for a specific user and conversation
     * @param user the user
     * @param conversation the conversation
     * @return true if feedback exists, false otherwise
     */
    fun existsByUserAndConversation(user: User, conversation: Conversation): Boolean
    
    /**
     * Find feedback by ID and user (for authorization checks)
     * @param id the feedback ID
     * @param user the user who should own the feedback
     * @return the feedback if found and owned by user, null otherwise
     */
    fun findByIdAndUser(id: Long, user: User): Feedback?
    
    /**
     * Count feedback by status
     * @param status the feedback status to count
     * @return count of feedback with the specified status
     */
    fun countByStatus(status: FeedbackStatus): Long
    
    /**
     * Count positive and negative feedback
     * @param isPositive whether to count positive (true) or negative (false) feedback
     * @return count of feedback with the specified sentiment
     */
    fun countByIsPositive(isPositive: Boolean): Long
    
    /**
     * Find feedback with user and conversation information for admin queries
     * @param pageable pagination information
     * @return page of feedback with user and conversation details
     */
    @Query("""
        SELECT f FROM Feedback f 
        JOIN FETCH f.user u 
        JOIN FETCH f.conversation c 
        JOIN FETCH c.thread t 
        ORDER BY f.createdAt DESC
    """)
    fun findAllWithUserAndConversation(pageable: Pageable): Page<Feedback>
    
    /**
     * Find feedback by user and conversation
     * @param user the user
     * @param conversation the conversation
     * @return the feedback if found, null otherwise
     */
    fun findByUserAndConversation(user: User, conversation: Conversation): Feedback?
}