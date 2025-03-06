package com.ptbox.osint.repository

import com.ptbox.osint.model.Scan

interface ScanRepository {
    fun findById(scanId: Long): Scan
    fun findAllByOrderByDisplayOrderAsc(): List<Scan>
    fun updateDisplayOrder(scanId: Long, newOrder: Int)
    fun save(scan: Scan): Scan
}
