package kz.aqyldykundelik.ai.service

import com.fasterxml.jackson.databind.ObjectMapper
import kz.aqyldykundelik.ai.client.AiClient
import kz.aqyldykundelik.ai.client.AiGenerateRequest
import kz.aqyldykundelik.ai.config.AiProperties
import kz.aqyldykundelik.ai.domain.AiGeneratedContentEntity
import kz.aqyldykundelik.ai.domain.AiGeneratedType
import kz.aqyldykundelik.ai.repo.AiGeneratedContentRepository
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
                system = systemPrompt,
                user = userPrompt,
                model = aiProperties.model,
                maxTokens = aiProperties.maxTokens,
                temperature = aiProperties.temperature
            )
        )

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

        return toDto(saved, cached = false)
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
                system = systemPrompt,
                user = userPrompt,
                model = aiProperties.model,
                maxTokens = aiProperties.maxTokens,
                temperature = aiProperties.temperature
            )
        )

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

        return toDto(saved, cached = false)
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
            mapOf(
                "topicId" to topic.topicId,
                "topicName" to topic.topicName,
                "percent" to topic.percent,
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
            "wrongQuestions" to wrongQuestions
        )
    }

    private fun buildSystemPrompt(language: String): String {
        return """
            You are an AI learning coach. Language: $language.
            Style: concise, step-by-step. Do not request personal data.
            Do not mention tokens, models, or system details.
        """.trimIndent()
    }

    private fun buildPlanUserPrompt(snapshot: Map<String, Any>): String {
        val snapshotJson = objectMapper.writeValueAsString(snapshot)
        return """
            Type: PLAN
            Snapshot: $snapshotJson
            Requirements:
            - list weak topics
            - 7-day plan (10-15 minutes per day)
            - 2-3 short rules or formulas
            - 3 self-check questions
        """.trimIndent()
    }

    private fun buildTopicHelpUserPrompt(snapshot: Map<String, Any>, mode: String): String {
        val snapshotJson = objectMapper.writeValueAsString(snapshot)
        return """
            Type: TOPIC_HELP
            Mode: $mode
            Snapshot: $snapshotJson
            Requirements:
            - explain the main mistake
            - provide 2 correct examples
            - give 5 practice tasks with answers
            - use existing explanations when available
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
            snapshotVersion
        ).joinToString("|")
        return sha256Hex(raw)
    }

    private fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun toDto(entity: AiGeneratedContentEntity, cached: Boolean): AiGeneratedDto {
        return AiGeneratedDto(
            type = entity.type,
            attemptId = entity.attemptId,
            topicId = entity.topicId,
            content = entity.content,
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
}
