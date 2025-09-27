package com.hightemplar.vipo.dto

import com.hightemplar.vipo.entity.Thread
import java.time.LocalDateTime

data class ThreadResponse(
    val id: Long,
    val lastActivityAt: LocalDateTime,
    val conversationCount: Int,
    val conversations: List<ConversationResponse>? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun fromEntity(thread: Thread, includeConversations: Boolean = false): ThreadResponse {
            return ThreadResponse(
                id = thread.id,
                lastActivityAt = thread.lastActivityAt,
                conversationCount = thread.conversations.size,
                conversations = if (includeConversations) {
                    thread.conversations.map { ConversationResponse.fromEntity(it) }
                } else null,
                createdAt = thread.createdAt,
                updatedAt = thread.updatedAt
            )
        }
    }
}