package com.hightemplar.vipo.dto

import jakarta.validation.constraints.NotNull

data class FeedbackRequest(
    @field:NotNull(message = "Conversation ID is required")
    val conversationId: Long,
    
    @field:NotNull(message = "Feedback rating is required")
    val isPositive: Boolean
)