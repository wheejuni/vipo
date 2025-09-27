package com.hightemplar.vipo.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "threads")
data class Thread(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    
    @Column(name = "last_activity_at", nullable = false)
    var lastActivityAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity() {
    
    @OneToMany(mappedBy = "thread", cascade = [CascadeType.ALL], orphanRemoval = true)
    val conversations: MutableList<Conversation> = mutableListOf()
}