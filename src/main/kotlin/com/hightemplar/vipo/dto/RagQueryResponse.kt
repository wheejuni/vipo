package com.hightemplar.vipo.dto

import java.time.LocalDateTime

data class RagQueryResponse(
    val answer: String,
    val sources: List<DocumentSource>,
    val confidence: Double,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class DocumentSource(
    val documentId: Long,
    val title: String,
    val excerpt: String,
    val relevanceScore: Double,
    val filename: String? = null
)