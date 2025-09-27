package com.hightemplar.vipo.dto

import com.hightemplar.vipo.entity.FeedbackStatus
import jakarta.validation.constraints.NotNull

data class FeedbackStatusUpdateRequest(
    @field:NotNull(message = "Status is required")
    val status: FeedbackStatus
)