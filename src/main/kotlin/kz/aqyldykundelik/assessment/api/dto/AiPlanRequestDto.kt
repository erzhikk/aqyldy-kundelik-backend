package kz.aqyldykundelik.assessment.api.dto

import java.util.UUID

data class AiPlanRequestDto(
    val attemptId: UUID,
    val language: String? = null
)
