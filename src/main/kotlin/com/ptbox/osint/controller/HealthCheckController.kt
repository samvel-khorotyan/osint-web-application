package com.ptbox.osint.controller

import com.ptbox.osint.service.DockerService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/api/health")
class HealthCheckController(
    private val dockerService: DockerService
) {
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    fun healthCheck(): Map<String, Any> {
        val status = mapOf(
            "status" to "UP",
            "timestamp" to Date(),
            "dockerAvailable" to dockerService.isDockerAvailable(),
            "version" to "1.0.0"
        )
        return status
    }
}
