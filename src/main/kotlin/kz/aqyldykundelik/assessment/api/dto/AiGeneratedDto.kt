package kz.aqyldykundelik.assessment.api.dto

import java.time.OffsetDateTime
import java.util.UUID

data class AiGeneratedDto(
    val type: String,
    val attemptId: UUID?,
    val topicId: UUID?,
    val content: String,
    val cached: Boolean,
    val createdAt: OffsetDateTime
)
