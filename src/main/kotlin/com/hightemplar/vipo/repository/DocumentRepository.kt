package com.hightemplar.vipo.repository

import com.hightemplar.vipo.entity.Document
import com.hightemplar.vipo.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DocumentRepository : JpaRepository<Document, Long> {
    
    fun findByUploadedByOrderByUploadedAtDesc(user: User): List<Document>
    
    fun findByIdAndUploadedBy(id: Long, user: User): Document?
    
    fun findByStatusAndUploadedBy(status: String, user: User): List<Document>
    
    fun countByUploadedBy(user: User): Long
}