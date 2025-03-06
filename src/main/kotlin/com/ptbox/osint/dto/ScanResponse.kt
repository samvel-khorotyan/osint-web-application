package com.ptbox.osint.dto

import com.ptbox.osint.model.ScanStatus
import java.time.LocalDateTime

data class ScanResponse(
    val id: Long,
    val domain: String,
    val tool: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime?,
    val status: ScanStatus,
    val results: Map<String, Any>?,
    val displayOrder: Int
)
