package com.hightemplar.vipo.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.hightemplar.vipo.SecurityTestSupport
import com.hightemplar.vipo.config.UserPrincipal
import com.hightemplar.vipo.dto.*
import com.hightemplar.vipo.entity.*
import com.hightemplar.vipo.exception.ConversationNotFoundException
import com.hightemplar.vipo.exception.DuplicateFeedbackException
import com.hightemplar.vipo.exception.FeedbackNotFoundException
import com.hightemplar.vipo.service.FeedbackService
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClientConfigurer
import java.time.LocalDateTime

@WebFluxTest(controllers = [FeedbackController::class])
@Import(SecurityTestSupport::class) // 선택: WebFlux Security 설정을 별도 구성했으면 임포트
class FeedbackControllerWebFluxTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var feedbackService: FeedbackService

    private lateinit var memberUser: User
    private lateinit var adminUser: User
    private lateinit var memberPrincipal: UserPrincipal
    private lateinit var adminPrincipal: UserPrincipal
    private lateinit var feedbackResponse: FeedbackResponse

    private val objectMapper = jacksonObjectMapper()

    @BeforeEach
    fun setUp() {
        // 엔티티 id 세팅(상속 구조일 경우 리플렉션 유지)
        memberUser = User(
            email = "member@test.com",
            password = "password",
            name = "Member User",
            role = UserRole.MEMBER
        ).apply {
            val idField = this::class.java.superclass.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, 1L)
        }

        adminUser = User(
            email = "admin@test.com",
            password = "password",
            name = "Admin User",
            role = UserRole.ADMIN
        ).apply {
            val idField = this::class.java.superclass.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, 2L)
        }

        memberPrincipal = UserPrincipal(memberUser)
        adminPrincipal = UserPrincipal(adminUser)

        feedbackResponse = FeedbackResponse(
            id = 1L,
            conversationId = 1L,
            userId = 1L,
            userEmail = "member@test.com",
            isPositive = true,
            status = FeedbackStatus.PENDING,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    // --- helpers -------------------------------------------------------------

    private fun withPrincipal(principal: UserPrincipal): WebTestClientConfigurer {
        val authority = SimpleGrantedAuthority("ROLE_${principal.authorities.first().authority}")
        val auth = UsernamePasswordAuthenticationToken(principal, null, listOf(authority))
        return SecurityMockServerConfigurers.mockAuthentication(auth)
    }

    private fun asJson(body: Any) = objectMapper.writeValueAsString(body)

    // --- tests : create ------------------------------------------------------

    @Test
    fun `createFeedback should create feedback successfully`() {
        // Given
        val request = FeedbackRequest(conversationId = 1L, isPositive = true)
        every { feedbackService.createFeedback(request, memberUser) } returns feedbackResponse

        // When & Then
        webTestClient.mutateWith(withPrincipal(memberPrincipal))
            .post()
            .uri("/api/feedback")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(asJson(request))
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.id").isEqualTo(1)
            .jsonPath("$.conversationId").isEqualTo(1)
            .jsonPath("$.userId").isEqualTo(1)
            .jsonPath("$.userEmail").isEqualTo("member@test.com")
            .jsonPath("$.isPositive").isEqualTo(true)
            .jsonPath("$.status").isEqualTo("PENDING")

        verify { feedbackService.createFeedback(request, memberUser) }
    }

    @Test
    fun `createFeedback should return bad request when conversation not found`() {
        val request = FeedbackRequest(conversationId = 999L, isPositive = true)
        every { feedbackService.createFeedback(any(), any()) } throws ConversationNotFoundException("Conversation not found")

        webTestClient.mutateWith(withPrincipal(memberPrincipal))
            .post()
            .uri("/api/feedback")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(asJson(request))
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `createFeedback should return bad request when duplicate feedback`() {
        val request = FeedbackRequest(conversationId = 1L, isPositive = true)
        every { feedbackService.createFeedback(any(), any()) } throws DuplicateFeedbackException("Feedback already exists")

        webTestClient.mutateWith(withPrincipal(memberPrincipal))
            .post()
            .uri("/api/feedback")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(asJson(request))
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `createFeedback should return bad request when access denied`() {
        val request = FeedbackRequest(conversationId = 1L, isPositive = true)
        every { feedbackService.createFeedback(any(), any()) } throws org.springframework.security.access.AccessDeniedException("Access denied")

        webTestClient.mutateWith(withPrincipal(memberPrincipal))
            .post()
            .uri("/api/feedback")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(asJson(request))
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `createFeedback should return unauthorized without authentication`() {
        val request = FeedbackRequest(conversationId = 1L, isPositive = true)

        webTestClient
            .post()
            .uri("/api/feedback")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(asJson(request))
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `createFeedback should return bad request with invalid request body`() {
        val invalidJson = """{"conversationId": null, "isPositive": true}"""

        webTestClient.mutateWith(withPrincipal(memberPrincipal))
            .post()
            .uri("/api/feedback")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invalidJson)
            .exchange()
            .expectStatus().isBadRequest
    }

    // --- tests : get list ----------------------------------------------------

    @Test
    fun `getFeedback should return member's feedback`() {
        val page = PageImpl(listOf(feedbackResponse))
        every { feedbackService.getFeedback(any(), any(), any()) } returns page

        webTestClient.mutateWith(withPrincipal(memberPrincipal))
            .get()
            .uri { b -> b.path("/api/feedback").queryParam("page", "0").queryParam("size", "10").build() }
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.content[0].id").isEqualTo(1)
            .jsonPath("$.content[0].userId").isEqualTo(1)
            .jsonPath("$.totalElements").isEqualTo(1)

        verify { feedbackService.getFeedback(any(), null, any()) }
    }

    @Test
    fun `getFeedback should return filtered feedback by status`() {
        val page = PageImpl(listOf(feedbackResponse))
        every { feedbackService.getFeedback(any(), FeedbackStatus.PENDING, any()) } returns page

        webTestClient.mutateWith(withPrincipal(memberPrincipal))
            .get()
            .uri { b -> b.path("/api/feedback").queryParam("status", "PENDING").build() }
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.content[0].status").isEqualTo("PENDING")

        verify { feedbackService.getFeedback(any(), FeedbackStatus.PENDING, any()) }
    }

    @Test
    fun `getFeedback should return all feedback for admin`() {
        val page = PageImpl(listOf(feedbackResponse))
        every { feedbackService.getFeedback(any(), any(), any()) } returns page

        webTestClient.mutateWith(withPrincipal(adminPrincipal))
            .get()
            .uri("/api/feedback")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.content").isArray

        verify { feedbackService.getFeedback(any(), null, any()) }
    }

    // --- tests : get by id ---------------------------------------------------

    @Test
    fun `getFeedbackById should return feedback for member`() {
        every { feedbackService.getFeedbackById(1L, any()) } returns feedbackResponse

        webTestClient.mutateWith(withPrincipal(memberPrincipal))
            .get()
            .uri("/api/feedback/1")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(1)
            .jsonPath("$.userId").isEqualTo(1)

        verify { feedbackService.getFeedbackById(1L, any()) }
    }

    @Test
    fun `getFeedbackById should return not found when feedback not accessible`() {
        every { feedbackService.getFeedbackById(1L, any()) } throws FeedbackNotFoundException("Feedback not found")

        webTestClient.mutateWith(withPrincipal(memberPrincipal))
            .get()
            .uri("/api/feedback/1")
            .exchange()
            .expectStatus().isNotFound
    }

    // --- tests : update status (admin only) ---------------------------------

    @Test
    fun `updateFeedbackStatus should update status for admin`() {
        val request = FeedbackStatusUpdateRequest(status = FeedbackStatus.RESOLVED)
        val updated = feedbackResponse.copy(status = FeedbackStatus.RESOLVED)

        every { feedbackService.updateFeedbackStatus(1L, FeedbackStatus.RESOLVED, any()) } returns updated

        webTestClient.mutateWith(withPrincipal(adminPrincipal))
            .put()
            .uri("/api/feedback/1/status")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(asJson(request))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(1)
            .jsonPath("$.status").isEqualTo("RESOLVED")

        verify { feedbackService.updateFeedbackStatus(1L, FeedbackStatus.RESOLVED, any()) }
    }

    @Test
    fun `updateFeedbackStatus should return forbidden for member`() {
        val request = FeedbackStatusUpdateRequest(status = FeedbackStatus.RESOLVED)

        webTestClient.mutateWith(withPrincipal(memberPrincipal))
            .put()
            .uri("/api/feedback/1/status")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(asJson(request))
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `updateFeedbackStatus should return bad request when feedback not found`() {
        val request = FeedbackStatusUpdateRequest(status = FeedbackStatus.RESOLVED)
        every { feedbackService.updateFeedbackStatus(999L, FeedbackStatus.RESOLVED, any()) } throws FeedbackNotFoundException("Feedback not found")

        webTestClient.mutateWith(withPrincipal(adminPrincipal))
            .put()
            .uri("/api/feedback/999/status")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(asJson(request))
            .exchange()
            .expectStatus().isBadRequest
    }

    // --- tests : can create --------------------------------------------------

    @Test
    fun `canCreateFeedback should return true when user can create feedback`() {
        every { feedbackService.canCreateFeedback(1L, any()) } returns true

        webTestClient.mutateWith(withPrincipal(memberPrincipal))
            .get()
            .uri("/api/feedback/can-create/1")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.canCreate").isEqualTo(true)

        verify { feedbackService.canCreateFeedback(1L, any()) }
    }

    @Test
    fun `canCreateFeedback should return false when user cannot create feedback`() {
        every { feedbackService.canCreateFeedback(1L, any()) } returns false

        webTestClient.mutateWith(withPrincipal(memberPrincipal))
            .get()
            .uri("/api/feedback/can-create/1")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.canCreate").isEqualTo(false)

        verify { feedbackService.canCreateFeedback(1L, any()) }
    }

    @Test
    fun `canCreateFeedback should return false on service error`() {
        every { feedbackService.canCreateFeedback(1L, any()) } throws RuntimeException("Service error")

        webTestClient.mutateWith(withPrincipal(memberPrincipal))
            .get()
            .uri("/api/feedback/can-create/1")
            .exchange()
            .expectStatus().is5xxServerError
            .expectBody()
            .jsonPath("$.canCreate").isEqualTo(false)
    }
}