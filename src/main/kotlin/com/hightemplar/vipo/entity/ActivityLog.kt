package com.hightemplar.vipo.entity

import jakarta.persistence.*

@Entity
@Table(name = "activity_logs")
data class ActivityLog(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User?,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    val activityType: ActivityType
) : BaseEntity()