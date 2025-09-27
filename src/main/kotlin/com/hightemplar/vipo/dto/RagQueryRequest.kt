package com.hightemplar.vipo.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class RagQueryRequest(
    @field:NotBlank(message = "Query cannot be blank")
    val query: String,
    
    @field:Min(value = 1, message = "Max results must be at least 1")
    @field:Max(value = 20, message = "Max results cannot exceed 20")
    val maxResults: Int? = 5,
    
    @field:Min(value = 0, message = "Threshold must be between 0 and 1")
    @field:Max(value = 1, message = "Threshold must be between 0 and 1")
    val threshold: Double? = 0.7
)