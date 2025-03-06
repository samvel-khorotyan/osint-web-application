package com.ptbox.osint.repository

import com.ptbox.osint.model.Scan
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface ScanPersistenceRepository : JpaRepository<Scan, Long> {
    fun findAllByOrderByDisplayOrderAsc(): List<Scan>

    @Transactional
    @Modifying
    @Query("UPDATE Scan s SET s.displayOrder = :newOrder WHERE s.id = :scanId")
    fun updateDisplayOrder(scanId: Long, newOrder: Int)
}
