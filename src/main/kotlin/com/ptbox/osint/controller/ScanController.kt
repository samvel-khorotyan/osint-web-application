package com.ptbox.osint.controller

import com.ptbox.osint.dto.ScanRequest
import com.ptbox.osint.dto.ScanResponse
import com.ptbox.osint.dto.UpdateDisplayOrderRequest
import com.ptbox.osint.service.ScanService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@RequestMapping("/api/scans")
class ScanController(
    private val scanService: ScanService
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun initiateScan(@Valid @RequestBody request: ScanRequest): ScanResponse {
        return scanService.initiateScan(request)
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    fun getAllScans(): List<ScanResponse> {
        return scanService.getAllScans()
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    fun getScanById(@PathVariable id: Long): ScanResponse {
        return scanService.getScanById(id)
    }

    @PutMapping("/order")
    @ResponseStatus(HttpStatus.OK)
    fun updateDisplayOrder(@Valid @RequestBody request: UpdateDisplayOrderRequest) {
        scanService.updateDisplayOrder(request.scanId, request.newOrder)
    }
}
