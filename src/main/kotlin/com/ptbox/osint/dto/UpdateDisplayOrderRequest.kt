package com.ptbox.osint.dto

data class UpdateDisplayOrderRequest(
    val scanId: Long,
    val newOrder: Int
)
