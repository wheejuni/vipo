package com.hightemplar.vipo.dto

import com.hightemplar.vipo.entity.Conversation
import java.time.LocalDateTime

data class ConversationResponse(
    val id: Long,
    val threadId: Long,
    val question: String,
    val answer: String,
    val model: String,
    val isStreaming: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun fromEntity(conversation: Conversation): ConversationResponse {
            return ConversationResponse(
                id = conversation.id,
                threadId = conversation.thread.id,
                question = conversation.question,
                answer = conversation.answer,
                model = conversation.model,
                isStreaming = conversation.isStreaming,
                createdAt = conversation.createdAt,
                updatedAt = conversation.updatedAt
            )
        }
    }
}