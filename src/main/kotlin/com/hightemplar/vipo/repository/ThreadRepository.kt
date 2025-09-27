package com.hightemplar.vipo.repository

import com.hightemplar.vipo.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ThreadRepository : JpaRepository<Thread, Long> {
    
    /**
     * Find all threads for a specific user with pagination
     * @param user the user to find threads for
     * @param pageable pagination information
     * @return page of threads for the user
     */
    fun findByUser(user: User, pageable: Pageable): Page<Thread>
    
    /**
     * Find all threads for a specific user ordered by last activity (most recent first)
     * @param user the user to find threads for
     * @param pageable pagination information
     * @return page of threads ordered by last activity
     */
    fun findByUserOrderByLastActivityAtDesc(user: User, pageable: Pageable): Page<Thread>
    
    /**
     * Find threads for a user with conversation count
     * @param user the user to find threads for
     * @param pageable pagination information
     * @return page of threads with conversation count
     */
    @Query("""
        SELECT t FROM Thread t 
        LEFT JOIN FETCH t.conversations 
        WHERE t.user = :user 
        ORDER BY t.lastActivityAt DESC
    """)
    fun findByUserWithConversations(@Param("user") user: User, pageable: Pageable): Page<Thread>
    
    /**
     * Count threads for a specific user
     * @param user the user to count threads for
     * @return number of threads for the user
     */
    fun countByUser(user: User): Long
    
    /**
     * Find thread by ID and user (for authorization checks)
     * @param id the thread ID
     * @param user the user who should own the thread
     * @return the thread if found and owned by user, null otherwise
     */
    fun findByIdAndUser(id: Long, user: User): Thread?
    
    /**
     * Find all threads for a specific user ordered by last activity (without pagination)
     * @param user the user to find threads for
     * @return list of threads ordered by last activity
     */
    fun findByUserOrderByLastActivityAtDesc(user: User): List<Thread>
}