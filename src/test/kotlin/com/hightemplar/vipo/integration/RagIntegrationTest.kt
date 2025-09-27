package com.hightemplar.vipo.integration

import com.hightemplar.vipo.entity.User
import com.hightemplar.vipo.entity.UserRole
import com.hightemplar.vipo.repository.UserRepository
import com.hightemplar.vipo.service.RagService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import reactor.test.StepVerifier

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RagIntegrationTest {
    
    @Autowired
    private lateinit var ragService: RagService
    
    @Autowired
    private lateinit var userRepository: UserRepository
    
    private lateinit var testUser: User
    
    @BeforeEach
    fun setUp() {
        testUser = User(
            email = "rag-test@example.com",
            password = "password123",
            name = "RAG Test User",
            role = UserRole.MEMBER
        )
        testUser = userRepository.save(testUser)
    }
    
    @Test
    fun `queryDocuments should handle empty vector store gracefully`() {
        // Given
        val query = "What is artificial intelligence?"
        
        // When
        val result = ragService.queryDocuments(
            query = query,
            user = testUser,
            maxResults = 5,
            threshold = 0.7
        )
        
        // Then
        StepVerifier.create(result)
            .assertNext { response ->
                // Should return a response even with empty vector store
                assert(response.answer.isNotEmpty())
                assert(response.sources.isEmpty()) // No documents in vector store
                assert(response.confidence >= 0.0)
            }
            .verifyComplete()
    }
    
    @Test
    fun `queryDocumentsStream should handle empty vector store gracefully`() {
        // Given
        val query = "What is machine learning?"
        
        // When
        val result = ragService.queryDocumentsStream(
            query = query,
            user = testUser,
            maxResults = 5,
            threshold = 0.7
        )
        
        // Then
        StepVerifier.create(result)
            .expectNextCount(0) // May not emit anything if no context
            .verifyComplete()
    }
    
    @Test
    fun `getUserDocuments should return empty list for new user`() {
        // When
        val result = ragService.getUserDocuments(testUser, 0, 20)
        
        // Then
        StepVerifier.create(result)
            .assertNext { pageResponse ->
                assert(pageResponse.content.isEmpty())
                assert(pageResponse.totalElements == 0L)
                assert(pageResponse.page == 0)
                assert(pageResponse.size == 20)
            }
            .verifyComplete()
    }
}