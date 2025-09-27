package com.hightemplar.vipo.controller

import com.hightemplar.vipo.config.UserPrincipal
import com.hightemplar.vipo.dto.DocumentResponse
import com.hightemplar.vipo.dto.RagQueryRequest
import com.hightemplar.vipo.dto.RagQueryResponse
import com.hightemplar.vipo.dto.DocumentUploadResponse
import com.hightemplar.vipo.dto.PageResponse
import com.hightemplar.vipo.service.RagService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/rag")
@Tag(name = "RAG", description = "Retrieval-Augmented Generation for internal documents")
class RagController(
    private val ragService: RagService
) {
    
    private val logger = LoggerFactory.getLogger(RagController::class.java)
    
    /**
     * Query internal documents using RAG.
     * POST /api/rag/query
     */
    @PostMapping("/query")
    @Operation(summary = "Query documents", description = "Query internal documents using RAG")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Query processed successfully"),
        ApiResponse(responseCode = "400", description = "Invalid request"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun queryDocuments(
        @Valid @RequestBody request: RagQueryRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): Mono<ResponseEntity<RagQueryResponse>> {
        logger.debug("Processing RAG query for user: {}", userPrincipal.username)
        
        val user = userPrincipal.getUser()
        
        return ragService.queryDocuments(
            query = request.query,
            user = user,
            maxResults = request.maxResults ?: 5,
            threshold = request.threshold ?: 0.7
        ).map { response ->
            logger.debug("Successfully processed RAG query for user: {}", userPrincipal.username)
            ResponseEntity.ok(response)
        }.onErrorResume { exception ->
            logger.error("Error processing RAG query for user: {}", userPrincipal.username, exception)
            Mono.just(
                ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(RagQueryResponse(
                        answer = "Error processing query: ${exception.message}",
                        sources = emptyList(),
                        confidence = 0.0
                    ))
            )
        }
    }
    
    /**
     * Query internal documents using RAG with streaming response.
     * POST /api/rag/query/stream
     */
    @PostMapping("/query/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    @Operation(summary = "Stream query documents", description = "Query internal documents using RAG with streaming response")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Streaming query started"),
        ApiResponse(responseCode = "400", description = "Invalid request"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun queryDocumentsStream(
        @Valid @RequestBody request: RagQueryRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): Flux<String> {
        logger.debug("Processing streaming RAG query for user: {}", userPrincipal.username)
        
        val user = userPrincipal.getUser()
        
        return ragService.queryDocumentsStream(
            query = request.query,
            user = user,
            maxResults = request.maxResults ?: 5,
            threshold = request.threshold ?: 0.7
        ).map { chunk ->
            "data: $chunk\n\n"
        }.onErrorResume { exception ->
            logger.error("Error in streaming RAG query for user: {}", userPrincipal.username, exception)
            Flux.just("event: error\ndata: ${exception.message}\n\n")
        }
    }
    
    /**
     * Upload a document for RAG indexing.
     * POST /api/rag/documents
     */
    @PostMapping("/documents", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Upload document", description = "Upload a document for RAG indexing")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Document uploaded successfully"),
        ApiResponse(responseCode = "400", description = "Invalid file or request"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun uploadDocument(
        @Parameter(description = "Document file") @RequestPart("file") file: Mono<FilePart>,
        @Parameter(description = "Document title") @RequestParam(required = false) title: String?,
        @Parameter(description = "Document description") @RequestParam(required = false) description: String?,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): Mono<ResponseEntity<DocumentUploadResponse>> {
        logger.debug("Uploading document for user: {}", userPrincipal.username)
        
        val user = userPrincipal.getUser()
        
        return file.flatMap { filePart ->
            ragService.uploadDocument(
                filePart = filePart,
                title = title,
                description = description,
                user = user
            )
        }.map { response ->
            logger.debug("Successfully uploaded document for user: {}", userPrincipal.username)
            ResponseEntity.status(HttpStatus.CREATED).body(response)
        }.onErrorResume { exception ->
            logger.error("Error uploading document for user: {}", userPrincipal.username, exception)
            Mono.just(
                ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(DocumentUploadResponse(
                        id = null,
                        filename = "unknown",
                        status = "error",
                        message = "Error uploading document: ${exception.message}"
                    ))
            )
        }
    }
    
    /**
     * Get user's uploaded documents.
     * GET /api/rag/documents
     */
    @GetMapping("/documents")
    @Operation(summary = "Get documents", description = "Get user's uploaded documents")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Documents retrieved successfully"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun getDocuments(
        @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): Mono<ResponseEntity<PageResponse<DocumentResponse>?>?> {
        logger.debug("Retrieving documents for user: {}", userPrincipal.username)
        
        val user = userPrincipal.getUser()
        
        return ragService.getUserDocuments(user, page, size)
            .map { documents ->
                logger.debug("Successfully retrieved documents for user: {}", userPrincipal.username)
                ResponseEntity.ok(documents)
            }.onErrorResume { exception ->
                logger.error("Error retrieving documents for user: {}", userPrincipal.username, exception)
                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null))
            }
    }
    
    /**
     * Delete a document.
     * DELETE /api/rag/documents/{documentId}
     */
    @DeleteMapping("/documents/{documentId}")
    @Operation(summary = "Delete document", description = "Delete a document and its embeddings")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Document deleted successfully"),
        ApiResponse(responseCode = "404", description = "Document not found"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun deleteDocument(
        @Parameter(description = "Document ID") @PathVariable documentId: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): Mono<ResponseEntity<Map<String, String>>> {
        logger.debug("Deleting document: {} for user: {}", documentId, userPrincipal.username)
        
        val user = userPrincipal.getUser()
        
        return ragService.deleteDocument(documentId, user)
            .map {
                logger.debug("Successfully deleted document: {} for user: {}", documentId, userPrincipal.username)
                ResponseEntity.ok(mapOf("message" to "Document deleted successfully"))
            }.onErrorResume { exception ->
                logger.error("Error deleting document: {} for user: {}", documentId, userPrincipal.username, exception)
                Mono.just(
                    ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(mapOf("error" to "Document not found or access denied"))
                )
            }
    }
}