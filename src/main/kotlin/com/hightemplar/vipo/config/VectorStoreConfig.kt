package com.hightemplar.vipo.config

import com.hightemplar.vipo.dto.SimpleDocument
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Configuration
class VectorStoreConfig {
    
    @Bean
    fun vectorStore(): SimpleVectorStore {
        return SimpleVectorStore()
    }
}

/**
 * Simple in-memory vector store implementation for RAG functionality
 * This is a basic implementation that uses simple text matching instead of embeddings
 */
@Component
class SimpleVectorStore {
    
    private val documents = ConcurrentHashMap<String, SimpleDocument>()
    
    fun add(documents: List<SimpleDocument>) {
        documents.forEach { doc ->
            val id = doc.id ?: generateId()
            doc.id = id
            this.documents[id] = doc
        }
    }
    
    fun delete(idList: List<String>): Boolean {
        return try {
            idList.forEach { id ->
                documents.remove(id)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun similaritySearch(query: String, maxResults: Int = 4, threshold: Double = 0.0): List<SimpleDocument> {
        if (documents.isEmpty()) {
            return emptyList()
        }
        
        val queryWords = query.lowercase().split("\\s+".toRegex()).toSet()
        
        return documents.values
            .map { doc ->
                val docWords = doc.content.lowercase().split("\\s+".toRegex()).toSet()
                val similarity = calculateTextSimilarity(queryWords, docWords)
                doc to similarity
            }
            .filter { (_, similarity) -> similarity >= threshold }
            .sortedByDescending { (_, similarity) -> similarity }
            .take(maxResults)
            .map { (doc, similarity) ->
                doc.apply {
                    metadata["distance"] = (1.0 - similarity).toString()
                }
            }
    }
    
    private fun generateId(): String {
        return "doc_${System.currentTimeMillis()}_${(Math.random() * 1000).toInt()}"
    }
    
    private fun calculateTextSimilarity(queryWords: Set<String>, docWords: Set<String>): Double {
        if (queryWords.isEmpty() || docWords.isEmpty()) return 0.0
        
        val intersection = queryWords.intersect(docWords).size
        val union = queryWords.union(docWords).size
        
        return if (union == 0) 0.0 else intersection.toDouble() / union.toDouble()
    }
}