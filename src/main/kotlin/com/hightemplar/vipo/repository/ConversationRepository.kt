package com.hightemplar.vipo.repository

import com.hightemplar.vipo.entity.Conversation
import com.hightemplar.vipo.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ConversationRepository : JpaRepository<Conversation, Long> {
    
    /**
     * Find all conversations for a specific thread
     * @param thread the thread to find conversations for
     * @return list of conversations in the thread
     */
    fun findByThread(thread: Thread): List<Conversation>
    
    /**
     * Find conversations for a thread with pagination, ordered by creation time
     * @param thread the thread to find conversations for
     * @param pageable pagination information
     * @return page of conversations ordered by creation time
     */
    fun findByThreadOrderByCreatedAtAsc(thread: Thread, pageable: Pageable): Page<Conversation>
    
    /**
     * Count conversations in a specific thread
     * @param thread the thread to count conversations for
     * @return number of conversations in the thread
     */
    fun countByThread(thread: Thread): Long
    
    /**
     * Find conversations created within a time range for reporting
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @return list of conversations within the time range
     */
    fun findByCreatedAtBetween(startTime: LocalDateTime, endTime: LocalDateTime): List<Conversation>
    
    /**
     * Find conversations created within a time range with user information for analytics
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @return list of conversations with user information
     */
    @Query("""
        SELECT c FROM Conversation c 
        JOIN FETCH c.thread t 
        JOIN FETCH t.user u 
        WHERE c.createdAt BETWEEN :startTime AND :endTime 
        ORDER BY c.createdAt DESC
    """)
    fun findByCreatedAtBetweenWithUser(
        @Param("startTime") startTime: LocalDateTime,
        @Param("endTime") endTime: LocalDateTime
    ): List<Conversation>
    
    /**
     * Count conversations created within a time range
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @return count of conversations within the time range
     */
    fun countByCreatedAtBetween(startTime: LocalDateTime, endTime: LocalDateTime): Long
    
    /**
     * Find conversations by thread user (for user-specific queries)
     * @param user the user whose conversations to find
     * @param pageable pagination information
     * @return page of conversations for the user
     */
    @Query("""
        SELECT c FROM Conversation c 
        JOIN c.thread t 
        WHERE t.user = :user 
        ORDER BY c.createdAt DESC
    """)
    fun findByThreadUser(@Param("user") user: User, pageable: Pageable): Page<Conversation>
    
    /**
     * Find conversation by ID and thread user (for authorization checks)
     * @param id the conversation ID
     * @param user the user who should own the conversation
     * @return the conversation if found and owned by user, null otherwise
     */
    @Query("""
        SELECT c FROM Conversation c 
        JOIN c.thread t 
        WHERE c.id = :id AND t.user = :user
    """)
    fun findByIdAndThreadUser(@Param("id") id: Long, @Param("user") user: User): Conversation?
    
    /**
     * Find conversations for a thread ordered by creation time (without pagination)
     * @param thread the thread to find conversations for
     * @return list of conversations ordered by creation time
     */
    fun findByThreadOrderByCreatedAtAsc(thread: Thread): List<Conversation>
}