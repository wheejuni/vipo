package com.hightemplar.vipo.service

import com.hightemplar.vipo.dto.*
import com.hightemplar.vipo.entity.User
import com.hightemplar.vipo.entity.Document
import com.hightemplar.vipo.repository.DocumentRepository
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import com.hightemplar.vipo.dto.SimpleDocument
import com.hightemplar.vipo.config.SimpleVectorStore
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime

@Service
class RagService(
    private val documentRepository: DocumentRepository,
    private val vectorStore: SimpleVectorStore,
    private val chatClient: ChatClient
) {
    
    private val logger = LoggerFactory.getLogger(RagService::class.java)
    
    /**
     * Query documents using RAG
     */
    fun queryDocuments(
        query: String,
        user: User,
        maxResults: Int = 5,
        threshold: Double = 0.7
    ): Mono<RagQueryResponse> {
        logger.debug("Processing RAG query: {} for user: {}", query, user.email)
        
        return Mono.fromCallable {
            // Search for similar documents in vector store
            val similarDocuments = vectorStore.similaritySearch(query, maxResults, threshold)
            
            // Extract context from similar documents
            val context = similarDocuments.joinToString("\n\n") { doc ->
                "${doc.metadata["title"] ?: "Document"}: ${doc.content}"
            }
            
            // Generate response using AI with context
            val prompt = """
                Based on the following context from internal documents, answer the user's question.
                If the context doesn't contain relevant information, say so clearly.
                
                Context:
                $context
                
                Question: $query
                
                Answer:
            """.trimIndent()
            
            val aiResponse = chatClient.prompt(prompt).call().content()
            
            // Create document sources
            val sources = similarDocuments.mapIndexed { index, doc ->
                DocumentSource(
                    documentId = doc.metadata["documentId"]?.toString()?.toLongOrNull() ?: index.toLong(),
                    title = doc.metadata["title"]?.toString() ?: "Document ${index + 1}",
                    excerpt = doc.content.take(200) + if (doc.content.length > 200) "..." else "",
                    relevanceScore = doc.metadata["distance"]?.toString()?.toDoubleOrNull() ?: 0.0,
                    filename = doc.metadata["filename"]?.toString()
                )
            }
            
            RagQueryResponse(
                answer = aiResponse ?: "Unable to generate response",
                sources = sources,
                confidence = if (sources.isNotEmpty()) sources.maxOf { it.relevanceScore } else 0.0
            )
        }.doOnSuccess {
            logger.debug("Successfully processed RAG query for user: {}", user.email)
        }.doOnError { exception ->
            logger.error("Error processing RAG query for user: {}", user.email, exception)
        }
    }
    
    /**
     * Query documents using RAG with streaming response
     */
    fun queryDocumentsStream(
        query: String,
        user: User,
        maxResults: Int = 5,
        threshold: Double = 0.7
    ): Flux<String> {
        logger.debug("Processing streaming RAG query: {} for user: {}", query, user.email)
        
        return Mono.fromCallable {
            // Search for similar documents in vector store
            val similarDocuments = vectorStore.similaritySearch(query, maxResults, threshold)
            
            // Extract context from similar documents
            val context = similarDocuments.joinToString("\n\n") { doc ->
                "${doc.metadata["title"] ?: "Document"}: ${doc.content}"
            }
            
            // Generate prompt
            """
                Based on the following context from internal documents, answer the user's question.
                If the context doesn't contain relevant information, say so clearly.
                
                Context:
                $context
                
                Question: $query
                
                Answer:
            """.trimIndent()
        }.flatMapMany { prompt ->
            // Stream the AI response
            chatClient.prompt(prompt).stream().content()
        }.doOnComplete {
            logger.debug("Successfully completed streaming RAG query for user: {}", user.email)
        }.doOnError { exception ->
            logger.error("Error in streaming RAG query for user: {}", user.email, exception)
        }
    }
    
    /**
     * Upload and index a document
     */
    fun uploadDocument(
        filePart: FilePart,
        title: String?,
        description: String?,
        user: User
    ): Mono<DocumentUploadResponse> {
        logger.debug("Uploading document: {} for user: {}", filePart.filename(), user.email)
        
        return DataBufferUtils.join(filePart.content())
            .map { dataBuffer ->
                val bytes = ByteArray(dataBuffer.readableByteCount())
                dataBuffer.read(bytes)
                DataBufferUtils.release(dataBuffer)
                bytes
            }
            .flatMap { bytes ->
                Mono.fromCallable {
                    // Save document metadata to database
                    val document = Document(
                        filename = filePart.filename() ?: "unknown",
                        title = title ?: "Untitled",
                        description = description,
                        content = String(bytes, StandardCharsets.UTF_8),
                        size = bytes.size.toLong(),
                        uploadedBy = user,
                        uploadedAt = LocalDateTime.now(),
                        status = "processing"
                    )
                    
                    val savedDocument = documentRepository.save(document)
                    
                    // Process document for vector store
                    try {
                        val content = String(bytes, StandardCharsets.UTF_8)
                        
                        // Create simple document for vector store
                        val simpleDocument = SimpleDocument(
                            content = content,
                            metadata = mutableMapOf(
                                "documentId" to savedDocument.id.toString(),
                                "title" to savedDocument.title,
                                "filename" to savedDocument.filename,
                                "userId" to user.id.toString()
                            )
                        )
                        
                        val documents = listOf(simpleDocument)
                        
                        // Add to vector store
                        vectorStore.add(documents)
                        
                        // Update status to indexed
                        savedDocument.status = "indexed"
                        documentRepository.save(savedDocument)
                        
                        DocumentUploadResponse(
                            id = savedDocument.id,
                            filename = savedDocument.filename,
                            status = "success",
                            message = "Document uploaded and indexed successfully"
                        )
                    } catch (e: Exception) {
                        logger.error("Error indexing document: {}", e.message, e)
                        savedDocument.status = "error"
                        documentRepository.save(savedDocument)
                        
                        DocumentUploadResponse(
                            id = savedDocument.id,
                            filename = savedDocument.filename,
                            status = "error",
                            message = "Document uploaded but indexing failed: ${e.message}"
                        )
                    }
                }
            }
            .doOnSuccess { _ ->
                logger.debug("Successfully uploaded document for user: {}", user.email)
            }
            .doOnError { exception ->
                logger.error("Error uploading document for user: {}", user.email, exception)
            }
    }
    
    /**
     * Get user's documents
     */
    fun getUserDocuments(user: User, page: Int, size: Int): Mono<PageResponse<DocumentResponse>> {
        return Mono.fromCallable {
            val documents = documentRepository.findByUploadedByOrderByUploadedAtDesc(user)
            
            val documentResponses = documents.map { doc ->
                DocumentResponse(
                    id = doc.id!!,
                    filename = doc.filename,
                    title = doc.title,
                    description = doc.description,
                    uploadedAt = doc.uploadedAt,
                    size = doc.size,
                    status = doc.status
                )
            }
            
            // Simple pagination (in real implementation, use proper pagination)
            val startIndex = page * size
            val endIndex = minOf(startIndex + size, documentResponses.size)
            val pageContent = if (startIndex < documentResponses.size) {
                documentResponses.subList(startIndex, endIndex)
            } else {
                emptyList()
            }
            
            PageResponse(
                content = pageContent,
                page = page,
                size = size,
                totalElements = documentResponses.size.toLong(),
                totalPages = (documentResponses.size + size - 1) / size,
                first = page == 0,
                last = endIndex >= documentResponses.size,
                hasNext = endIndex < documentResponses.size,
                hasPrevious = page > 0
            )
        }
    }
    
    /**
     * Delete a document
     */
    fun deleteDocument(documentId: Long, user: User): Mono<Unit> {
        return Mono.fromCallable {
            val document = documentRepository.findByIdAndUploadedBy(documentId, user)
                ?: throw IllegalArgumentException("Document not found or access denied")
            
            // Remove from vector store (this is a simplified approach)
            // In a real implementation, you'd need to track and remove specific embeddings
            
            // Delete from database
            documentRepository.delete(document)
        }
    }
}