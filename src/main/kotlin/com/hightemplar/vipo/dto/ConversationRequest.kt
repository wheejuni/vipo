package com.hightemplar.vipo.dto

import com.hightemplar.vipo.validation.ValidConversationQuestion
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin

data class ConversationRequest(
    @field:NotBlank(message = "Question is required")
    @field:Size(max = 10000, message = "Question must not exceed 10000 characters")
    @field:ValidConversationQuestion
    val question: String,
    
    @field:Size(max = 50, message = "Model name must not exceed 50 characters")
    val model: String? = null,
    
    @field:DecimalMin(value = "0.0", message = "Temperature must be at least 0.0")
    @field:DecimalMax(value = "1.0", message = "Temperature must be at most 1.0")
    val temperature: Double? = null,
    
    val isStreaming: Boolean? = null
)