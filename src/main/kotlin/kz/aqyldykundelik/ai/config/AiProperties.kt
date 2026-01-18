package kz.aqyldykundelik.ai.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ai")
data class AiProperties(
    val enabled: Boolean = true,
    val provider: String = "stub",
    val model: String = "gpt-4.1-mini",
    val maxTokens: Int = 900,
    val temperature: Double = 0.4,
    val cacheTtlDays: CacheTtlDays = CacheTtlDays(),
    val limits: Limits = Limits()
) {
    data class CacheTtlDays(
        val plan: Long = 7,
        val topicHelp: Long = 3
    )

    data class Limits(
        val perStudentPerDay: Int = 5
    )
}
