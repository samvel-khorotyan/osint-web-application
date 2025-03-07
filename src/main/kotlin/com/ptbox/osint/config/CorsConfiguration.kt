package com.ptbox.osint.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CorsConfiguration {
    @Value("\${cors.origins}")
    private val origins: Array<String> = arrayOf()

    @Bean
    fun corsConfigurer(): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun addCorsMappings(registry: CorsRegistry) {
                registry
                    .addMapping("/**")
                    .allowedOrigins(*origins)
                    .allowedMethods(
                        HttpMethod.GET.toString(),
                        HttpMethod.DELETE.toString(),
                        HttpMethod.PUT.toString(),
                        HttpMethod.PATCH.toString(),
                        HttpMethod.POST.toString()
                    )
            }
        }
    }
}
