package com.ptbox.osint.service

import com.ptbox.osint.dto.ScanRequest
import com.ptbox.osint.dto.ScanResponse
import com.ptbox.osint.model.Scan

interface ScanService {
    fun initiateScan(scanRequest: ScanRequest): ScanResponse
    fun getAllScans(): List<ScanResponse>
    fun getScanById(id: Long): ScanResponse
    fun updateDisplayOrder(scanId: Long, newOrder: Int)
    fun findById(id: Long): Scan
}
