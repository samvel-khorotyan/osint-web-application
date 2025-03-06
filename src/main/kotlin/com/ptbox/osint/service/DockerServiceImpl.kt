package com.ptbox.osint.service

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import com.ptbox.osint.config.DockerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException
import java.time.Duration
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Service
class DockerServiceImpl(
    private val dockerConfig: DockerConfig,
    @Value("\${app.docker.fallback-mode:false}") private val fallbackMode: Boolean
) : DockerService {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val semaphore = Semaphore(3)
    private val dockerClient: DockerClient? by lazy { initDockerClient() }

    private fun initDockerClient(): DockerClient? {
        if (fallbackMode) {
            logger.info("Running in fallback mode, Docker client will not be initialized")
            return null
        }

        return try {
            val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerConfig.host)
                .withDockerTlsVerify(false)
                .build()

            val httpClient: DockerHttpClient = ApacheDockerHttpClient.Builder()
                .dockerHost(config.dockerHost)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofMinutes(10))
                .build()

            val client = DockerClientImpl.getInstance(config, httpClient)
            client.pingCmd().exec()
            logger.info("Docker client initialized successfully")
            client
        } catch (e: Exception) {
            logger.error("Failed to initialize Docker client: ${e.message}")
            null
        }
    }

    override fun isDockerAvailable(): Boolean {
        return dockerClient != null && !fallbackMode
    }

    override suspend fun runAmass(domain: String, timeout: Int, passive: Boolean): String =
        withContext(Dispatchers.IO) {
            logger.info("Running Amass for domain: $domain, passive mode: $passive, timeout: $timeout minutes")

            if (fallbackMode || dockerClient == null) {
                logger.info("Running in fallback mode or Docker client is not available. Returning mock results.")
                return@withContext generateMockAmassResults(domain)
            }

            val outputFile = "/tmp/amass_output.txt"
            val cmd = mutableListOf(
                "enum",
                "-d", domain,
                "-timeout", timeout.toString(),
                "-o", outputFile
            )
            if (passive) {
                cmd.add("-passive")
            }

            logger.info("Running command: amass ${cmd.joinToString(" ")}")
            try {
                val (_, result) = runContainer(dockerConfig.amass.image, cmd.toTypedArray())
                result
            } catch (e: Exception) {
                logger.error("Error running Amass container: ${e.message}")
                generateMockAmassResults(domain)
            }
        }

    private fun generateMockAmassResults(domain: String): String {
        logger.info("Generating mock results for domain: $domain")
        val subdomains = listOf("www.$domain", "api.$domain", "mail.$domain", "blog.$domain")
        val ips = listOf("192.168.1.1", "192.168.1.2")

        return buildString {
            append("Mock Amass results for $domain\n")
            append("Subdomains:\n")
            subdomains.forEach { append("$it\n") }
            append("\nIPs:\n")
            ips.forEach { append("$it\n") }
        }
    }

    private suspend fun runContainer(image: String, cmd: Array<String>): Pair<String, String> {
        return suspendCancellableCoroutine { continuation ->
            semaphore.acquire()
            var containerId: String? = null
            try {
                val client = dockerClient ?: throw IllegalStateException("Docker client is not available")

                logger.info("Pulling image: $image")

                if (!isImageAvailable(image)) {
                    client.pullImageCmd(image).exec(PullImageResultCallback())
                        .awaitCompletion(5, TimeUnit.MINUTES)
                }

                logger.info("Creating container with image: $image")
                val container: CreateContainerResponse = client.createContainerCmd(image)
                    .withHostConfig(
                        HostConfig.newHostConfig()
                            .withMemory(512 * 1024 * 1024)  //512MB
                            .withCpuCount(2L) // 2 CPUs
                    )
                    .withCmd(*cmd)
                    .exec()

                containerId = container.id
                logger.info("Starting container: $containerId")
                client.startContainerCmd(containerId).exec()

                logger.info("Waiting for container to complete...")
                val waitResult = client.waitContainerCmd(containerId)
                    .start()
                    .awaitCompletion(10, TimeUnit.MINUTES) // Wait up to 10 minutes

                logger.info("Container completion result: $waitResult")

                val containerInfo = client.inspectContainerCmd(containerId).exec()
                logger.info("Container exit code: ${containerInfo.state.exitCode}")
                logger.info("Container status: ${containerInfo.state.status}")

                val logs = StringBuilder()
                client.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .exec(object : ResultCallback.Adapter<Frame>() {
                        override fun onNext(frame: Frame) {
                            logs.append(String(frame.payload))
                        }
                    }).awaitCompletion()


                logger.info("Container logs: $logs")
                saveLogsToFile(containerId, logs.toString())

                val outputContent = try {
                    client.copyArchiveFromContainerCmd(containerId, "/tmp/amass_output.txt")
                        .exec()
                        .use { it.readAllBytes().toString(Charsets.UTF_8) }
                } catch (e: Exception) {
                    logger.error("Error reading output file from container: ${e.message}")
                    "" // Return empty string if file can't be read
                }
                logger.info("Amass output file content: $outputContent")

                continuation.resume(Pair(containerId, logs.toString() + "\n" + outputContent))

            } catch (e: Exception) {
                continuation.resumeWithException(e)
            } finally {
                containerId?.let {
                    try {
                        logger.info("Removing container: $it")
                        dockerClient?.removeContainerCmd(it)?.withForce(true)?.exec()
                    } catch (e: Exception) {
                        logger.error("Error removing container $it: ${e.message}")
                    }
                }
                semaphore.release()
            }
        }
    }

    private fun isImageAvailable(image: String): Boolean {
        return dockerClient?.listImagesCmd()?.exec()?.any {
            it.repoTags?.contains(image) ?: false
        } ?: false
    }

    private fun saveLogsToFile(containerId: String, logContent: String) {
        try {
            val logDir = File(dockerConfig.logsDirectory)
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            val logFile = File(logDir, "$containerId.log")
            logFile.writeText(logContent)
            logger.info("Logs saved to: ${logFile.absolutePath}")
        } catch (e: IOException) {
            logger.error("Error saving logs to file: ${e.message}")
        }
    }
}
