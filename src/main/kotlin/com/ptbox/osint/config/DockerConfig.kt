package com.ptbox.osint.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "docker")
data class DockerConfig(
    var host: String = "",
    val amass: ImageConfig = ImageConfig(),
    var logsDirectory: String = "/tmp/docker-logs"
) {
    data class ImageConfig(
        var image: String = ""
    )
}
