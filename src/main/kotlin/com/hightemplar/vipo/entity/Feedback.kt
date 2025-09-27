package com.hightemplar.vipo.entity

import jakarta.persistence.*

@Entity
@Table(name = "feedback")
data class Feedback(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    val conversation: Conversation,
    
    @Column(name = "is_positive", nullable = false)
    val isPositive: Boolean,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: FeedbackStatus = FeedbackStatus.PENDING
) : BaseEntity()