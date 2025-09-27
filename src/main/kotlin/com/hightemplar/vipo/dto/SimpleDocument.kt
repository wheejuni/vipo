package com.hightemplar.vipo.dto

/**
 * Simple document class for vector store operations
 */
data class SimpleDocument(
    val content: String,
    val metadata: MutableMap<String, String> = mutableMapOf(),
    var id: String? = null
)