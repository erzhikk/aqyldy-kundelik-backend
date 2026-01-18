package kz.aqyldykundelik.ai.client

import kz.aqyldykundelik.ai.domain.AiGeneratedType

data class AiGenerateRequest(
    val type: AiGeneratedType,
    val system: String,
    val user: String,
    val model: String,
    val maxTokens: Int,
    val temperature: Double
)

data class AiGenerateResult(
    val content: String,
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
    val provider: String? = null,
    val model: String? = null
)

interface AiClient {
    fun generate(request: AiGenerateRequest): AiGenerateResult
}
