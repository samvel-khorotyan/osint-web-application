package com.ptbox.osint.service

interface DockerService {
    suspend fun runAmass(domain: String, timeout: Int = 2, passive: Boolean = true): String
    fun isDockerAvailable(): Boolean
}
