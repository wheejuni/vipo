package com.hightemplar.vipo.service

import com.hightemplar.vipo.entity.Thread
import com.hightemplar.vipo.entity.User
import com.hightemplar.vipo.repository.ThreadRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class ThreadService(
    private val threadRepository: ThreadRepository
) {
    
    companion object {
        const val THREAD_TIMEOUT_MINUTES = 30L
    }
    
    /**
     * Determines if a new thread should be created for the user based on the 30-minute rule.
     * A new thread is needed if:
     * - The user has no existing threads
     * - The last activity in the user's most recent thread was more than 30 minutes ago
     * 
     * @param user the user to check
     * @return true if a new thread should be created, false if the existing thread can be used
     */
    fun shouldCreateNewThread(user: User): Boolean {
        val latestThread = getLatestThreadForUser(user)
        
        return if (latestThread == null) {
            // No existing threads, create new one
            true
        } else {
            // Check if 30 minutes have passed since last activity
            val now = LocalDateTime.now()
            val timeSinceLastActivity = java.time.Duration.between(latestThread.lastActivityAt, now)
            timeSinceLastActivity.toMinutes() >= THREAD_TIMEOUT_MINUTES
        }
    }
    
    /**
     * Gets the most recent thread for a user, or null if no threads exist.
     * 
     * @param user the user to get the latest thread for
     * @return the latest thread or null
     */
    fun getLatestThreadForUser(user: User): Thread? {
        val threads = threadRepository.findByUserOrderByLastActivityAtDesc(
            user, 
            Pageable.ofSize(1)
        )
        return threads.content.firstOrNull()
    }
    
    /**
     * Creates a new thread for the specified user.
     * 
     * @param user the user to create the thread for
     * @return the newly created thread
     */
    fun createThread(user: User): Thread {
        val thread = Thread(
            user = user,
            lastActivityAt = LocalDateTime.now()
        )
        return threadRepository.save(thread)
    }
    
    /**
     * Gets or creates a thread for the user based on the 30-minute rule.
     * If a new thread is needed, creates one. Otherwise, returns the existing thread.
     * 
     * @param user the user to get or create a thread for
     * @return the thread to use for new conversations
     */
    fun getOrCreateThreadForUser(user: User): Thread {
        return if (shouldCreateNewThread(user)) {
            createThread(user)
        } else {
            getLatestThreadForUser(user)!!
        }
    }
    
    /**
     * Updates the last activity timestamp for a thread.
     * This should be called whenever a new conversation is added to the thread.
     * 
     * @param thread the thread to update
     */
    fun updateThreadActivity(thread: Thread) {
        thread.lastActivityAt = LocalDateTime.now()
        threadRepository.save(thread)
    }
    
    /**
     * Retrieves threads for a user with pagination and sorting by last activity.
     * 
     * @param user the user to get threads for
     * @param pageable pagination and sorting information
     * @return page of threads for the user
     */
    @Transactional(readOnly = true)
    fun getThreadsForUser(user: User, pageable: Pageable): Page<Thread> {
        return threadRepository.findByUserOrderByLastActivityAtDesc(user, pageable)
    }
    
    /**
     * Retrieves threads for a user with their conversations loaded.
     * 
     * @param user the user to get threads for
     * @param pageable pagination and sorting information
     * @return page of threads with conversations for the user
     */
    @Transactional(readOnly = true)
    fun getThreadsWithConversationsForUser(user: User, pageable: Pageable): Page<Thread> {
        return threadRepository.findByUserWithConversations(user, pageable)
    }
    
    /**
     * Finds a thread by ID that belongs to the specified user.
     * This method ensures users can only access their own threads.
     * 
     * @param threadId the ID of the thread to find
     * @param user the user who should own the thread
     * @return the thread if found and owned by the user
     * @throws ThreadNotFoundException if thread not found or not owned by user
     */
    @Transactional(readOnly = true)
    fun findThreadByIdAndUser(threadId: Long, user: User): Thread {
        return threadRepository.findByIdAndUser(threadId, user)
            ?: throw ThreadNotFoundException("Thread with ID $threadId not found for user ${user.email}")
    }
    
    /**
     * Deletes a thread and all its associated conversations.
     * This method ensures users can only delete their own threads.
     * 
     * @param threadId the ID of the thread to delete
     * @param user the user who should own the thread
     * @throws ThreadNotFoundException if thread not found or not owned by user
     */
    fun deleteThread(threadId: Long, user: User) {
        val thread = findThreadByIdAndUser(threadId, user)
        threadRepository.delete(thread)
    }
    
    /**
     * Counts the total number of threads for a user.
     * 
     * @param user the user to count threads for
     * @return the number of threads for the user
     */
    @Transactional(readOnly = true)
    fun countThreadsForUser(user: User): Long {
        return threadRepository.countByUser(user)
    }
}

/**
 * Exception thrown when a thread is not found or not accessible by the user.
 */
class ThreadNotFoundException(message: String) : RuntimeException(message)