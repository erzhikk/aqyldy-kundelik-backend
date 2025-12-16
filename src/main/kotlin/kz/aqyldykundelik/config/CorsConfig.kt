package kz.aqyldykundelik.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
class CorsConfig {
    @Bean
    fun corsFilter(): CorsFilter {
        val cfg = CorsConfiguration()
        cfg.allowedOrigins = listOf("http://localhost:4200", "http://localhost:5173")
        cfg.allowedMethods = listOf("GET","POST","PUT","PATCH","DELETE","OPTIONS")
        cfg.allowedHeaders = listOf("*")
        cfg.allowCredentials = true
        cfg.maxAge = 3600
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", cfg)
        return CorsFilter(source)
    }
}
