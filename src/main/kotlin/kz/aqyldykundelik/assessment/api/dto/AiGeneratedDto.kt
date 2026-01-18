package kz.aqyldykundelik.assessment.api.dto

import java.time.OffsetDateTime
import java.util.UUID

data class AiGeneratedDto(
    val type: String,
    val attemptId: UUID?,
    val topicId: UUID?,
    val payload: Any,
    val cached: Boolean,
    val createdAt: OffsetDateTime
)
