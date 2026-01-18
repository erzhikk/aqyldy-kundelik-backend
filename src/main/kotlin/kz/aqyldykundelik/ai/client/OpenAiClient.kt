package kz.aqyldykundelik.ai.client

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kz.aqyldykundelik.ai.config.OpenAiProperties
import kz.aqyldykundelik.ai.domain.AiGeneratedType
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Component
@ConditionalOnProperty(name = ["ai.provider"], havingValue = "openai")
class OpenAiClient(
    private val openAiProperties: OpenAiProperties,
    private val objectMapper: ObjectMapper
) : AiClient {
    private val httpClient = HttpClient.newBuilder().build()

    override fun generate(request: AiGenerateRequest): AiGenerateResult {
        val schema = when (request.type) {
            AiGeneratedType.PLAN -> buildPlanSchema()
            AiGeneratedType.TOPIC_HELP -> buildTopicHelpSchema()
        }
        val schemaName = when (request.type) {
            AiGeneratedType.PLAN -> "ai_plan"
            AiGeneratedType.TOPIC_HELP -> "ai_topic_help"
        }

        val requestBody = mapOf(
            "model" to request.model,
            "input" to listOf(
                mapOf("role" to "system", "content" to request.system),
                mapOf("role" to "user", "content" to request.user)
            ),
            "response_format" to mapOf(
                "type" to "json_schema",
                "json_schema" to mapOf(
                    "name" to schemaName,
                    "schema" to schema,
                    "strict" to true
                )
            ),
            "max_output_tokens" to request.maxTokens,
            "temperature" to request.temperature
        )

        val url = openAiProperties.baseUrl.trimEnd('/') + "/responses"
        val httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer ${openAiProperties.apiKey}")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
            .build()

        val response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            val message = extractErrorMessage(response.body())
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "OpenAI error: ${message ?: "status ${response.statusCode()}"}"
            )
        }

        val root = objectMapper.readTree(response.body())
        val text = extractText(root)
        val usage = root.path("usage")
        val inputTokens = usage.path("input_tokens").takeIf { it.isInt }?.asInt()
        val outputTokens = usage.path("output_tokens").takeIf { it.isInt }?.asInt()
        val model = root.path("model").takeIf { it.isTextual }?.asText()

        return AiGenerateResult(
            content = text,
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            provider = "openai",
            model = model ?: request.model
        )
    }

    private fun extractText(root: JsonNode): String {
        val text = root.path("output").path(0).path("content").path(0).path("text").asText()
        if (text.isBlank()) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "OpenAI empty response")
        }
        return text
    }

    private fun extractErrorMessage(body: String): String? {
        return runCatching {
            val node = objectMapper.readTree(body)
            node.path("error").path("message").takeIf { it.isTextual }?.asText()
        }.getOrNull()
    }

    private fun buildPlanSchema(): Map<String, Any> {
        val weakTopicSchema = mapOf(
            "type" to "object",
            "additionalProperties" to false,
            "properties" to mapOf(
                "topicId" to mapOf("type" to "string"),
                "topicName" to mapOf("type" to "string"),
                "accuracy" to mapOf("type" to "number", "minimum" to 0, "maximum" to 1),
                "mainMistake" to mapOf("type" to "string")
            ),
            "required" to listOf("topicId", "topicName", "accuracy", "mainMistake")
        )
        val dayPlanSchema = mapOf(
            "type" to "object",
            "additionalProperties" to false,
            "properties" to mapOf(
                "day" to mapOf("type" to "integer", "minimum" to 1, "maximum" to 7),
                "focus" to mapOf("type" to "string"),
                "actions" to mapOf(
                    "type" to "array",
                    "items" to mapOf("type" to "string")
                ),
                "timeMinutes" to mapOf("type" to "integer", "minimum" to 1)
            ),
            "required" to listOf("day", "focus", "actions", "timeMinutes")
        )
        val selfCheckSchema = mapOf(
            "type" to "object",
            "additionalProperties" to false,
            "properties" to mapOf(
                "question" to mapOf("type" to "string"),
                "answer" to mapOf("type" to "string")
            ),
            "required" to listOf("question", "answer")
        )
        return mapOf(
            "type" to "object",
            "additionalProperties" to false,
            "properties" to mapOf(
                "weakTopics" to mapOf(
                    "type" to "array",
                    "items" to weakTopicSchema
                ),
                "weeklyPlan" to mapOf(
                    "type" to "array",
                    "items" to dayPlanSchema
                ),
                "rules" to mapOf(
                    "type" to "array",
                    "items" to mapOf("type" to "string")
                ),
                "selfCheck" to mapOf(
                    "type" to "array",
                    "items" to selfCheckSchema
                )
            ),
            "required" to listOf("weakTopics", "weeklyPlan", "rules", "selfCheck")
        )
    }

    private fun buildTopicHelpSchema(): Map<String, Any> {
        val topicSchema = mapOf(
            "type" to "object",
            "additionalProperties" to false,
            "properties" to mapOf(
                "topicId" to mapOf("type" to "string"),
                "topicName" to mapOf("type" to "string")
            ),
            "required" to listOf("topicId", "topicName")
        )
        val exampleSchema = mapOf(
            "type" to "object",
            "additionalProperties" to false,
            "properties" to mapOf(
                "question" to mapOf("type" to "string"),
                "solution" to mapOf("type" to "string")
            ),
            "required" to listOf("question", "solution")
        )
        val practiceSchema = mapOf(
            "type" to "object",
            "additionalProperties" to false,
            "properties" to mapOf(
                "question" to mapOf("type" to "string"),
                "answer" to mapOf("type" to "string")
            ),
            "required" to listOf("question", "answer")
        )

        return mapOf(
            "type" to "object",
            "additionalProperties" to false,
            "properties" to mapOf(
                "topic" to topicSchema,
                "mainError" to mapOf("type" to "string"),
                "explanation" to mapOf("type" to "string"),
                "examples" to mapOf(
                    "type" to "array",
                    "items" to exampleSchema
                ),
                "practice" to mapOf(
                    "type" to "array",
                    "items" to practiceSchema
                )
            ),
            "required" to listOf("topic", "mainError", "explanation", "examples", "practice")
        )
    }
}
