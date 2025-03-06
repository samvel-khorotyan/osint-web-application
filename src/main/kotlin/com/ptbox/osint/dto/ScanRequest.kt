package com.ptbox.osint.dto

import javax.validation.constraints.NotBlank
import javax.validation.constraints.Pattern

data class ScanRequest(
    @field:NotBlank(message = "Domain is required")
    @field:Pattern(
        regexp = "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$",
        message = "Invalid domain format"
    )
    val domain: String,

    val timeout: Int? = 60,
    val passive: Boolean? = true
)
