package com.hightemplar.vipo.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "documents")
data class Document(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "filename", nullable = false)
    var filename: String,
    
    @Column(name = "title", nullable = false)
    var title: String,
    
    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,
    
    @Column(name = "content", columnDefinition = "TEXT")
    var content: String,
    
    @Column(name = "size", nullable = false)
    var size: Long,
    
    @Column(name = "status", nullable = false)
    var status: String = "processing", // processing, indexed, error
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    val uploadedBy: User,
    
    @Column(name = "uploaded_at", nullable = false)
    val uploadedAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)