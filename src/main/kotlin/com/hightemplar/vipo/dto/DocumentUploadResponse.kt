package com.hightemplar.vipo.dto

import java.time.LocalDateTime

data class DocumentUploadResponse(
    val id: Long?,
    val filename: String,
    val status: String,
    val message: String,
    val uploadedAt: LocalDateTime = LocalDateTime.now()
)

data class DocumentResponse(
    val id: Long,
    val filename: String,
    val title: String?,
    val description: String?,
    val uploadedAt: LocalDateTime,
    val size: Long,
    val status: String
)