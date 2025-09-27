package com.hightemplar.vipo.entity

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank

@Entity
@Table(name = "conversations")
data class Conversation(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    val thread: Thread,
    
    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Question is required")
    val question: String,
    
    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Answer is required")
    val answer: String,
    
    @Column(nullable = false)
    @NotBlank(message = "Model is required")
    val model: String,
    
    @Column(name = "is_streaming", nullable = false)
    val isStreaming: Boolean
) : BaseEntity()