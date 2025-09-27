package com.hightemplar.vipo.dto

import com.hightemplar.vipo.entity.Feedback
import com.hightemplar.vipo.entity.FeedbackStatus
import java.time.LocalDateTime

data class FeedbackResponse(
    val id: Long,
    val conversationId: Long,
    val userId: Long,
    val userEmail: String,
    val isPositive: Boolean,
    val status: FeedbackStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun fromEntity(feedback: Feedback): FeedbackResponse {
            return FeedbackResponse(
                id = feedback.id,
                conversationId = feedback.conversation.id,
                userId = feedback.user.id,
                userEmail = feedback.user.email,
                isPositive = feedback.isPositive,
                status = feedback.status,
                createdAt = feedback.createdAt,
                updatedAt = feedback.updatedAt
            )
        }
    }
}