package kz.aqyldykundelik.ai.service

import com.fasterxml.jackson.databind.ObjectMapper
import kz.aqyldykundelik.ai.client.AiClient
import kz.aqyldykundelik.ai.client.AiGenerateRequest
import kz.aqyldykundelik.ai.config.AiProperties
import kz.aqyldykundelik.ai.domain.AiGeneratedContentEntity
import kz.aqyldykundelik.ai.domain.AiGeneratedType
import kz.aqyldykundelik.ai.repo.AiGeneratedContentRepository
import kz.aqyldykundelik.ai.dto.AiPlanResponseDto
import kz.aqyldykundelik.ai.dto.AiTopicHelpResponseDto
import kz.aqyldykundelik.assessment.api.dto.AiGeneratedDto
import kz.aqyldykundelik.assessment.service.AnalyticsService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.time.OffsetDateTime
import java.util.UUID

@Service
class AiCoachService(
    private val analyticsService: AnalyticsService,
    private val aiClient: AiClient,
    private val aiProperties: AiProperties,
    private val aiGeneratedContentRepository: AiGeneratedContentRepository,
    private val objectMapper: ObjectMapper
) {
    private val snapshotVersion = "v1"
    private val schemaVersion = "v1"

    fun generatePlan(studentId: UUID, attemptId: UUID, language: String?): AiGeneratedDto {
        ensureEnabled()
        val resolvedLanguage = normalizeLanguage(language)
        val promptHash = buildPromptHash(
            studentId = studentId,
            type = AiGeneratedType.PLAN,
            attemptId = attemptId,
            topicId = null,
            language = resolvedLanguage,
            mode = null
        )
        val cached = findCached(studentId, AiGeneratedType.PLAN, promptHash)
        if (cached != null) {
            return toDto(cached, cached = true)
        }

        enforceDailyLimit(studentId)

        val snapshot = buildPlanSnapshot(attemptId, studentId)
        val userPrompt = buildPlanUserPrompt(snapshot)
        val systemPrompt = buildSystemPrompt(resolvedLanguage)
        val result = aiClient.generate(
            AiGenerateRequest(
                type = AiGeneratedType.PLAN,
                system = systemPrompt,
                user = userPrompt,
                model = aiProperties.model,
                maxTokens = aiProperties.maxTokens,
                temperature = aiProperties.temperature
            )
        )
        val payload = parsePayload(AiGeneratedType.PLAN, result.content)

        val saved = aiGeneratedContentRepository.save(
            AiGeneratedContentEntity(
                studentId = studentId,
                attemptId = attemptId,
                topicId = null,
                type = AiGeneratedType.PLAN.name,
                promptHash = promptHash,
                content = result.content,
                model = result.model ?: aiProperties.model,
                provider = result.provider ?: aiProperties.provider,
                cachedUntil = OffsetDateTime.now().plusDays(aiProperties.cacheTtlDays.plan),
                inputTokens = result.inputTokens,
                outputTokens = result.outputTokens
            )
        )

        return toDto(saved, cached = false, payload = payload)
    }

    fun generateTopicHelp(
        studentId: UUID,
        attemptId: UUID,
        topicId: UUID,
        language: String?,
        mode: String?
    ): AiGeneratedDto {
        ensureEnabled()
        val resolvedLanguage = normalizeLanguage(language)
        val resolvedMode = mode?.lowercase() ?: "coach"
        val promptHash = buildPromptHash(
            studentId = studentId,
            type = AiGeneratedType.TOPIC_HELP,
            attemptId = attemptId,
            topicId = topicId,
            language = resolvedLanguage,
            mode = resolvedMode
        )
        val cached = findCached(studentId, AiGeneratedType.TOPIC_HELP, promptHash)
        if (cached != null) {
            return toDto(cached, cached = true)
        }

        enforceDailyLimit(studentId)

        val snapshot = buildTopicHelpSnapshot(attemptId, topicId, studentId)
        val userPrompt = buildTopicHelpUserPrompt(snapshot, resolvedMode)
        val systemPrompt = buildSystemPrompt(resolvedLanguage)
        val result = aiClient.generate(
            AiGenerateRequest(
                type = AiGeneratedType.TOPIC_HELP,
                system = systemPrompt,
                user = userPrompt,
                model = aiProperties.model,
                maxTokens = aiProperties.maxTokens,
                temperature = aiProperties.temperature
            )
        )
        val payload = parsePayload(AiGeneratedType.TOPIC_HELP, result.content)

        val saved = aiGeneratedContentRepository.save(
            AiGeneratedContentEntity(
                studentId = studentId,
                attemptId = attemptId,
                topicId = topicId,
                type = AiGeneratedType.TOPIC_HELP.name,
                promptHash = promptHash,
                content = result.content,
                model = result.model ?: aiProperties.model,
                provider = result.provider ?: aiProperties.provider,
                cachedUntil = OffsetDateTime.now().plusDays(aiProperties.cacheTtlDays.topicHelp),
                inputTokens = result.inputTokens,
                outputTokens = result.outputTokens
            )
        )

        return toDto(saved, cached = false, payload = payload)
    }

    private fun ensureEnabled() {
        if (!aiProperties.enabled) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI disabled")
        }
    }

    private fun findCached(
        studentId: UUID,
        type: AiGeneratedType,
        promptHash: String
    ): AiGeneratedContentEntity? {
        val cached = aiGeneratedContentRepository
            .findTop1ByStudentIdAndTypeAndPromptHashOrderByCreatedAtDesc(studentId, type.name, promptHash)
            ?: return null
        val cachedUntil = cached.cachedUntil ?: return null
        return if (cachedUntil.isAfter(OffsetDateTime.now())) cached else null
    }

    private fun enforceDailyLimit(studentId: UUID) {
        val limit = aiProperties.limits.perStudentPerDay
        if (limit <= 0) return
        val since = OffsetDateTime.now().minusDays(1)
        val count = aiGeneratedContentRepository.countByStudentIdAndCreatedAtAfter(studentId, since)
        if (count >= limit) {
            throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "AI limit reached")
        }
    }

    private fun buildPlanSnapshot(attemptId: UUID, studentId: UUID): Map<String, Any> {
        val topics = analyticsService.getAttemptTopics(attemptId, studentId)
        val takeCount = minOf(5, maxOf(3, topics.size))
        val weakTopics = topics.sortedBy { it.percent }.take(takeCount).map { topic ->
            val details = analyticsService.getAttemptTopicDetails(attemptId, topic.topicId, studentId)
            val wrongQuestions = details.filter { it.isCorrect != true }
            val examples = wrongQuestions.take(5).map { question ->
                mapOf(
                    "text" to question.text,
                    "explanation" to question.explanation
                )
            }
            val mainMistake = wrongQuestions.mapNotNull { it.explanation }.firstOrNull()
                ?: "Ошибки в решении задач по теме"
            mapOf(
                "topicId" to topic.topicId,
                "topicName" to topic.topicName,
                "accuracy" to (topic.percent.toDouble() / 100.0),
                "mainMistake" to mainMistake,
                "wrongCount" to details.count { it.isCorrect == false },
                "exampleWrongQuestions" to examples
            )
        }

        return mapOf(
            "snapshotVersion" to snapshotVersion,
            "attemptId" to attemptId,
            "weakTopics" to weakTopics
        )
    }

    private fun buildTopicHelpSnapshot(attemptId: UUID, topicId: UUID, studentId: UUID): Map<String, Any> {
        val topics = analyticsService.getAttemptTopics(attemptId, studentId)
        val topicName = topics.firstOrNull { it.topicId == topicId }?.topicName ?: "Unknown"
        val details = analyticsService.getAttemptTopicDetails(attemptId, topicId, studentId)
        val wrongQuestions = details.filter { it.isCorrect != true }.take(5).map { question ->
            mapOf(
                "text" to question.text,
                "explanation" to question.explanation
            )
        }

        return mapOf(
            "snapshotVersion" to snapshotVersion,
            "attemptId" to attemptId,
            "topicId" to topicId,
            "topicName" to topicName,
            "wrongQuestions" to wrongQuestions
        )
    }

    private fun buildSystemPrompt(language: String): String {
        return """
            Ты школьный ИИ-репетитор.
            Язык ответа: $language.
            Стиль: учебный, краткий.
            Запрещено: markdown, лишний текст, пояснения вне схемы.
            Отвечай строго валидным JSON по схеме.
        """.trimIndent()
    }

    private fun buildPlanUserPrompt(snapshot: Map<String, Any>): String {
        val snapshotJson = objectMapper.writeValueAsString(snapshot)
        return """
            Тип: PLAN
            Snapshot: $snapshotJson
            Требования:
            - список слабых тем
            - план на 7 дней по 10-15 минут
            - 2-3 коротких правила или формулы
            - 3 вопроса для самопроверки
            Верни строго JSON по схеме.
        """.trimIndent()
    }

    private fun buildTopicHelpUserPrompt(snapshot: Map<String, Any>, mode: String): String {
        val snapshotJson = objectMapper.writeValueAsString(snapshot)
        return """
            Тип: TOPIC_HELP
            Режим: $mode
            Snapshot: $snapshotJson
            Требования:
            - объясни главную ошибку
            - приведи 2 правильных примера
            - дай 5 задач для практики с ответами
            - используй существующие объяснения, если есть
            Верни строго JSON по схеме.
        """.trimIndent()
    }

    private fun buildPromptHash(
        studentId: UUID,
        type: AiGeneratedType,
        attemptId: UUID,
        topicId: UUID?,
        language: String?,
        mode: String?
    ): String {
        val raw = listOf(
            studentId.toString(),
            type.name,
            attemptId.toString(),
            topicId?.toString() ?: "-",
            language ?: "-",
            mode ?: "-",
            snapshotVersion,
            "schema=$schemaVersion"
        ).joinToString("|")
        return sha256Hex(raw)
    }

    private fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun toDto(entity: AiGeneratedContentEntity, cached: Boolean, payload: Any? = null): AiGeneratedDto {
        val resolvedPayload = payload ?: parsePayload(AiGeneratedType.valueOf(entity.type), entity.content)
        return AiGeneratedDto(
            type = entity.type,
            attemptId = entity.attemptId,
            topicId = entity.topicId,
            payload = resolvedPayload,
            cached = cached,
            createdAt = entity.createdAt
        )
    }

    private fun normalizeLanguage(language: String?): String {
        val normalized = language?.lowercase()
        return when (normalized) {
            "ru", "kk", "en" -> normalized
            else -> "ru"
        }
    }

    private fun parsePayload(type: AiGeneratedType, content: String): Any {
        return try {
            when (type) {
                AiGeneratedType.PLAN -> objectMapper.readValue(content, AiPlanResponseDto::class.java)
                AiGeneratedType.TOPIC_HELP -> objectMapper.readValue(content, AiTopicHelpResponseDto::class.java)
            }
        } catch (ex: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "AI response mapping failed")
        }
    }
}
