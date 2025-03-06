package com.ptbox.osint.service

import com.ptbox.osint.dto.ScanRequest
import com.ptbox.osint.dto.ScanResponse
import com.ptbox.osint.model.Scan
import com.ptbox.osint.model.ScanStatus
import com.ptbox.osint.repository.ScanRepository
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ScanServiceImpl(
    private val scanRepository: ScanRepository,
    private val dockerService: DockerService,
) : ScanService {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(Dispatchers.IO)

    @Transactional
    override fun initiateScan(scanRequest: ScanRequest): ScanResponse {
        logger.info("Initiating scan for domain: ${scanRequest.domain}")

        val maxDisplayOrder =
            scanRepository.findAllByOrderByDisplayOrderAsc().maxByOrNull { it.displayOrder }?.displayOrder ?: 0

        val scan = Scan(
            domain = scanRequest.domain,
            tool = "amass",
            status = ScanStatus.PENDING,
            displayOrder = maxDisplayOrder + 1
        )

        val savedScan = scanRepository.save(scan)
        logger.info("Saved scan with id: ${savedScan.id}")

        if (!dockerService.isDockerAvailable()) {
            logger.warn("Docker is not available. Scan will be processed in fallback mode.")
        }

        scope.launch {
            runScan(savedScan.id!!, scanRequest)
        }.invokeOnCompletion { throwable ->
            if (throwable != null) {
                logger.error("Async execution failed for scan id: ${savedScan.id}", throwable)
            }
        }

        return mapToScanResponse(savedScan)
    }

    private suspend fun runScan(scanId: Long, request: ScanRequest) {
        var retries = 3
        while (retries > 0) {
            try {
                logger.info("Attempting to run scan for id: $scanId (attempts left: $retries)")

                val scan = withContext(Dispatchers.IO) {
                    scanRepository.findById(scanId)
                }

                withContext(Dispatchers.IO) {
                    scan.status = ScanStatus.RUNNING
                    scanRepository.save(scan)
                }

                logger.info("Starting Amass scan for domain: ${request.domain}")
                val result = dockerService.runAmass(
                    domain = request.domain,
                    timeout = request.timeout ?: 1,
                    passive = request.passive ?: true
                )
                logger.info("Amass scan completed. Raw output length: ${result.length}")
                logger.info("First 1000 characters of raw output: ${result.take(1000)}")

                logger.info("Parsing Amass results...")
                val parsedResults = parseAmassResults(result)
                logger.info("Parsed results: $parsedResults")

                withContext(Dispatchers.IO) {
                    scan.endTime = LocalDateTime.now()
                    scan.status = ScanStatus.COMPLETED
                    scan.results = parsedResults
                    scanRepository.save(scan)
                }

                logger.info("Scan completed successfully for domain: ${request.domain}")
                break
            } catch (e: Exception) {
                logger.error("Error running scan for domain: ${request.domain}", e)
                retries--
                if (retries == 0) {
                    withContext(Dispatchers.IO) {
                        val failedScan = scanRepository.findById(scanId)
                        failedScan.endTime = LocalDateTime.now()
                        failedScan.status = ScanStatus.FAILED
                        failedScan.results = mapOf("error" to (e.message ?: "Unknown error"))
                        scanRepository.save(failedScan)
                    }
                } else {
                    delay(1000)
                }
            }
        }
    }

    private fun parseAmassResults(output: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        val subdomains = mutableSetOf<String>()
        val ips = mutableSetOf<String>()

        val lines = output.lines()
        val domainPattern = "([a-zA-Z0-9.-]+)".toRegex()
        val ipPattern = "(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})".toRegex()

        for (line in lines) {
            domainPattern.findAll(line).forEach { subdomains.add(it.value) }
            ipPattern.findAll(line).forEach { ips.add(it.value) }
        }

        result["subdomains"] = subdomains.filter { it.isNotBlank() }.toList()
        result["ips"] = ips.filter { it.isNotBlank() }.toList()
        result["rawOutput"] = output.replace("\u0000", "")

        logger.info("Parsed ${subdomains.size} subdomains and ${ips.size} IPs")

        return result
    }

    @Transactional(readOnly = true)
    override fun getAllScans(): List<ScanResponse> {
        return scanRepository.findAllByOrderByDisplayOrderAsc().map { mapToScanResponse(it) }
    }

    @Transactional(readOnly = true)
    override fun getScanById(id: Long): ScanResponse {
        val scan = scanRepository.findById(id)
        return mapToScanResponse(scan)
    }

    @Transactional
    override fun updateDisplayOrder(scanId: Long, newOrder: Int) {
        scanRepository.updateDisplayOrder(scanId, newOrder)
    }

    private fun mapToScanResponse(scan: Scan): ScanResponse {
        return ScanResponse(
            id = scan.id!!,
            domain = scan.domain,
            tool = scan.tool,
            startTime = scan.startTime,
            endTime = scan.endTime,
            status = scan.status,
            results = scan.results,
            displayOrder = scan.displayOrder
        )
    }

    @Transactional(readOnly = true)
    override fun findById(id: Long): Scan {
        return scanRepository.findById(id)
    }
}
