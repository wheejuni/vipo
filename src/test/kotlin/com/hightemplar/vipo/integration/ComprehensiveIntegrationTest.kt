package com.hightemplar.vipo.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.hightemplar.vipo.config.JwtTokenProvider
import com.hightemplar.vipo.dto.*
import com.hightemplar.vipo.entity.*
import com.hightemplar.vipo.repository.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

/**
 * Comprehensive integration tests covering all major functionality:
 * - Authentication flows (registration, login, JWT validation)
 * - Conversation management (creation, thread management, 30-minute rule)
 * - Feedback system (creation, management, admin controls)
 * - Analytics and reporting (activity tracking, CSV reports)
 * - Role-based access control
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class ComprehensiveIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var threadRepository: ThreadRepository

    @Autowired
    private lateinit var conversationRepository: ConversationRepository

    @Autowired
    private lateinit var feedbackRepository: FeedbackRepository

    @Autowired
    private lateinit var activityLogRepository: ActivityLogRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var adminUser: User
    private lateinit var memberUser: User
    private lateinit var adminToken: String
    private lateinit var memberToken: String

    @BeforeEach
    fun setUp() {
        // Clean up all repositories
        feedbackRepository.deleteAll()
        conversationRepository.deleteAll()
        threadRepository.deleteAll()
        activityLogRepository.deleteAll()
        userRepository.deleteAll()

        // Create test users
        adminUser = User(
            email = "admin@example.com",
            password = passwordEncoder.encode("AdminPass123!"),
            name = "Admin User",
            role = UserRole.ADMIN
        )
        userRepository.save(adminUser)

        memberUser = User(
            email = "member@example.com",
            password = passwordEncoder.encode("MemberPass123!"),
            name = "Member User",
            role = UserRole.MEMBER
        )
        userRepository.save(memberUser)

        // Generate tokens
        adminToken = jwtTokenProvider.generateToken(adminUser.email, adminUser.role.name)
        memberToken = jwtTokenProvider.generateToken(memberUser.email, memberUser.role.name)
    }

    // ========== AUTHENTICATION FLOW TESTS ==========

    @Test
    fun `should complete user registration flow`() {
        val registrationRequest = UserRegistrationRequest(
            email = "newuser@example.com",
            password = "NewUserPass123!",
            name = "New User"
        )

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.email").value("newuser@example.com"))
            .andExpect(jsonPath("$.name").value("New User"))
            .andExpect(jsonPath("$.role").value("MEMBER"))

        // Verify user was created
        val savedUser = userRepository.findByEmail("newuser@example.com")
        assert(savedUser.isPresent)
    }

    @Test
    fun `should complete user login flow`() {
        val loginRequest = UserLoginRequest(
            email = "member@example.com",
            password = "MemberPass123!"
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.email").value("member@example.com"))
            .andExpect(jsonPath("$.role").value("MEMBER"))
    }

    @Test
    fun `should reject invalid credentials`() {
        val loginRequest = UserLoginRequest(
            email = "member@example.com",
            password = "WrongPassword"
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should validate JWT tokens correctly`() {
        // Valid token should work
        mockMvc.perform(
            get("/api/conversations/threads")
                .header("Authorization", "Bearer $memberToken")
        )
            .andExpect(status().isOk)

        // No token should fail
        mockMvc.perform(
            get("/api/conversations/threads")
        )
            .andExpect(status().isUnauthorized)

        // Invalid token should fail
        mockMvc.perform(
            get("/api/conversations/threads")
                .header("Authorization", "Bearer invalid.token.here")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should enforce role-based access control`() {
        // Admin should access admin endpoints
        mockMvc.perform(
            get("/api/analytics/activity")
                .header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isOk)

        // Member should not access admin endpoints
        mockMvc.perform(
            get("/api/analytics/activity")
                .header("Authorization", "Bearer $memberToken")
        )
            .andExpect(status().isForbidden)
    }

    // ========== CONVERSATION MANAGEMENT TESTS ==========

    @Test
    fun `should create conversation and manage threads`() {
        val conversationRequest = ConversationRequest(
            question = "What is artificial intelligence?",
            model = "gpt-3.5-turbo",
            isStreaming = false
        )

        // Create first conversation
        val result1 = mockMvc.perform(
            post("/api/conversations")
                .header("Authorization", "Bearer $memberToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(conversationRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.question").value("What is artificial intelligence?"))
            .andExpect(jsonPath("$.threadId").exists())
            .andReturn()

        val response1 = objectMapper.readTree(result1.response.contentAsString)
        val threadId1 = response1.get("threadId").asLong()

        // Create second conversation (should use same thread within 30 minutes)
        val conversationRequest2 = ConversationRequest(
            question = "How does machine learning work?",
            model = "gpt-4",
            isStreaming = true
        )

        val result2 = mockMvc.perform(
            post("/api/conversations")
                .header("Authorization", "Bearer $memberToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(conversationRequest2))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.threadId").value(threadId1))
            .andReturn()

        // Verify both conversations are in the same thread
        val threads = threadRepository.findByUserOrderByLastActivityAtDesc(memberUser)
        assert(threads.size == 1)
        
        val conversations = conversationRepository.findByThreadOrderByCreatedAtAsc(threads[0])
        assert(conversations.size == 2)
    }

    @Test
    fun `should list user threads with pagination`() {
        // Create test data
        val thread = Thread(user = memberUser, lastActivityAt = java.time.LocalDateTime.now())
        threadRepository.save(thread)

        val conversation = Conversation(
            thread = thread,
            question = "Test question",
            answer = "Test answer",
            model = "gpt-3.5-turbo",
            isStreaming = false
        )
        conversationRepository.save(conversation)

        // Test thread listing
        mockMvc.perform(
            get("/api/conversations/threads")
                .header("Authorization", "Bearer $memberToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.totalElements").value(1))
    }

    @Test
    fun `should delete threads`() {
        // Create test thread
        val thread = Thread(user = memberUser, lastActivityAt = java.time.LocalDateTime.now())
        val savedThread = threadRepository.save(thread)

        // Delete thread
        mockMvc.perform(
            delete("/api/conversations/threads/${savedThread.id}")
                .header("Authorization", "Bearer $memberToken")
        )
            .andExpect(status().isOk)

        // Verify thread is deleted
        assert(!threadRepository.existsById(savedThread.id))
    }

    @Test
    fun `should prevent access to other users data`() {
        // Create thread for member user
        val memberThread = Thread(user = memberUser, lastActivityAt = java.time.LocalDateTime.now())
        val savedMemberThread = threadRepository.save(memberThread)

        // Create another user
        val otherUser = User(
            email = "other@example.com",
            password = passwordEncoder.encode("password123"),
            name = "Other User",
            role = UserRole.MEMBER
        )
        userRepository.save(otherUser)
        val otherToken = jwtTokenProvider.generateToken(otherUser.email, otherUser.role.name)

        // Other user should not be able to delete member's thread
        mockMvc.perform(
            delete("/api/conversations/threads/${savedMemberThread.id}")
                .header("Authorization", "Bearer $otherToken")
        )
            .andExpect(status().isNotFound)

        // Thread should still exist
        assert(threadRepository.existsById(savedMemberThread.id))
    }

    // ========== FEEDBACK SYSTEM TESTS ==========

    @Test
    fun `should create and manage feedback`() {
        // Create test conversation
        val thread = Thread(user = memberUser, lastActivityAt = java.time.LocalDateTime.now())
        threadRepository.save(thread)

        val conversation = Conversation(
            thread = thread,
            question = "Test question",
            answer = "Test answer",
            model = "gpt-3.5-turbo",
            isStreaming = false
        )
        val savedConversation = conversationRepository.save(conversation)

        // Create feedback
        val feedbackRequest = FeedbackRequest(
            conversationId = savedConversation.id,
            isPositive = true
        )

        mockMvc.perform(
            post("/api/feedback")
                .header("Authorization", "Bearer $memberToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(feedbackRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.isPositive").value(true))
            .andExpect(jsonPath("$.status").value("PENDING"))

        // Verify feedback was created
        val feedback = feedbackRepository.findByUserAndConversation(memberUser, savedConversation)
        assert(feedback != null)
        assert(feedback!!.isPositive)
    }

    @Test
    fun `should list user feedback`() {
        // Create test data
        val thread = Thread(user = memberUser, lastActivityAt = java.time.LocalDateTime.now())
        threadRepository.save(thread)

        val conversation = Conversation(
            thread = thread,
            question = "Test question",
            answer = "Test answer",
            model = "gpt-3.5-turbo",
            isStreaming = false
        )
        conversationRepository.save(conversation)

        val feedback = Feedback(
            user = memberUser,
            conversation = conversation,
            isPositive = true,
            status = FeedbackStatus.PENDING
        )
        feedbackRepository.save(feedback)

        // List feedback
        mockMvc.perform(
            get("/api/feedback")
                .header("Authorization", "Bearer $memberToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.totalElements").value(1))
    }

    @Test
    fun `admin should update feedback status`() {
        // Create test feedback
        val thread = Thread(user = memberUser, lastActivityAt = java.time.LocalDateTime.now())
        threadRepository.save(thread)

        val conversation = Conversation(
            thread = thread,
            question = "Test question",
            answer = "Test answer",
            model = "gpt-3.5-turbo",
            isStreaming = false
        )
        conversationRepository.save(conversation)

        val feedback = Feedback(
            user = memberUser,
            conversation = conversation,
            isPositive = true,
            status = FeedbackStatus.PENDING
        )
        val savedFeedback = feedbackRepository.save(feedback)

        // Admin updates status
        val statusUpdate = FeedbackStatusUpdateRequest(status = FeedbackStatus.RESOLVED)

        mockMvc.perform(
            put("/api/feedback/${savedFeedback.id}/status")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusUpdate))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("RESOLVED"))
    }

    @Test
    fun `member should not update feedback status`() {
        // Create test feedback
        val thread = Thread(user = memberUser, lastActivityAt = java.time.LocalDateTime.now())
        threadRepository.save(thread)

        val conversation = Conversation(
            thread = thread,
            question = "Test question",
            answer = "Test answer",
            model = "gpt-3.5-turbo",
            isStreaming = false
        )
        conversationRepository.save(conversation)

        val feedback = Feedback(
            user = memberUser,
            conversation = conversation,
            isPositive = true,
            status = FeedbackStatus.PENDING
        )
        val savedFeedback = feedbackRepository.save(feedback)

        // Member tries to update status
        val statusUpdate = FeedbackStatusUpdateRequest(status = FeedbackStatus.RESOLVED)

        mockMvc.perform(
            put("/api/feedback/${savedFeedback.id}/status")
                .header("Authorization", "Bearer $memberToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusUpdate))
        )
            .andExpect(status().isForbidden)
    }

    // ========== ANALYTICS AND REPORTING TESTS ==========

    @Test
    fun `admin should access activity analytics`() {
        // Create some activity logs
        activityLogRepository.save(ActivityLog(memberUser, ActivityType.USER_REGISTRATION))
        activityLogRepository.save(ActivityLog(memberUser, ActivityType.USER_LOGIN))
        activityLogRepository.save(ActivityLog(memberUser, ActivityType.CONVERSATION_CREATED))

        mockMvc.perform(
            get("/api/analytics/activity")
                .header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.registrationCount").exists())
            .andExpect(jsonPath("$.loginCount").exists())
            .andExpect(jsonPath("$.conversationCount").exists())
            .andExpect(jsonPath("$.timeRange").value("24 hours"))
    }

    @Test
    fun `admin should generate CSV reports`() {
        // Create test conversation
        val thread = Thread(user = memberUser, lastActivityAt = java.time.LocalDateTime.now())
        threadRepository.save(thread)

        val conversation = Conversation(
            thread = thread,
            question = "What is AI?",
            answer = "AI is artificial intelligence",
            model = "gpt-3.5-turbo",
            isStreaming = false
        )
        conversationRepository.save(conversation)

        mockMvc.perform(
            get("/api/analytics/reports/conversations")
                .header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/csv"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Question,Answer,Model,Streaming,Author,Created At")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("What is AI?")))
    }

    @Test
    fun `member should not access analytics`() {
        mockMvc.perform(
            get("/api/analytics/activity")
                .header("Authorization", "Bearer $memberToken")
        )
            .andExpect(status().isForbidden)

        mockMvc.perform(
            get("/api/analytics/reports/conversations")
                .header("Authorization", "Bearer $memberToken")
        )
            .andExpect(status().isForbidden)
    }

    // ========== INTEGRATION FLOW TESTS ==========

    @Test
    fun `should complete end-to-end user journey`() {
        // 1. Register new user
        val registrationRequest = UserRegistrationRequest(
            email = "journey@example.com",
            password = "JourneyPass123!",
            name = "Journey User"
        )

        val registrationResult = mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationRequest))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val registrationResponse = objectMapper.readValue(
            registrationResult.response.contentAsString,
            AuthResponse::class.java
        )
        val userToken = registrationResponse.token

        // 2. Create conversation
        val conversationRequest = ConversationRequest(
            question = "Hello, AI!",
            model = "gpt-3.5-turbo",
            isStreaming = false
        )

        mockMvc.perform(
            post("/api/conversations")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(conversationRequest))
        )
            .andExpect(status().isOk)

        // 3. List threads
        mockMvc.perform(
            get("/api/conversations/threads")
                .header("Authorization", "Bearer $userToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))

        // 4. Create feedback (get conversation first)
        val user = userRepository.findByEmail("journey@example.com").get()
        val threads = threadRepository.findByUserOrderByLastActivityAtDesc(user)
        val conversations = conversationRepository.findByThreadOrderByCreatedAtAsc(threads[0])
        val conversation = conversations[0]

        val feedbackRequest = FeedbackRequest(
            conversationId = conversation.id,
            isPositive = true
        )

        mockMvc.perform(
            post("/api/feedback")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(feedbackRequest))
        )
            .andExpect(status().isCreated)

        // 5. List feedback
        mockMvc.perform(
            get("/api/feedback")
                .header("Authorization", "Bearer $userToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
    }

    @Test
    fun `should track activities throughout user journey`() {
        // Register user (should log registration activity)
        val registrationRequest = UserRegistrationRequest(
            email = "activity@example.com",
            password = "ActivityPass123!",
            name = "Activity User"
        )

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationRequest))
        )
            .andExpect(status().isCreated)

        // Login user (should log login activity)
        val loginRequest = UserLoginRequest(
            email = "activity@example.com",
            password = "ActivityPass123!"
        )

        val loginResult = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val loginResponse = objectMapper.readValue(
            loginResult.response.contentAsString,
            AuthResponse::class.java
        )
        val userToken = loginResponse.token

        // Create conversation (should log conversation activity)
        val conversationRequest = ConversationRequest(
            question = "Track this activity",
            model = "gpt-3.5-turbo",
            isStreaming = false
        )

        mockMvc.perform(
            post("/api/conversations")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(conversationRequest))
        )
            .andExpect(status().isOk)

        // Check analytics (admin only)
        mockMvc.perform(
            get("/api/analytics/activity")
                .header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.registrationCount").value(org.hamcrest.Matchers.greaterThan(0)))
            .andExpect(jsonPath("$.loginCount").value(org.hamcrest.Matchers.greaterThan(0)))
            .andExpect(jsonPath("$.conversationCount").value(org.hamcrest.Matchers.greaterThan(0)))
    }
}