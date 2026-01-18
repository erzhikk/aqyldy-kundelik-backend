package kz.aqyldykundelik.ai.dto

import java.util.UUID

data class AiPlanResponseDto(
    val weakTopics: List<WeakTopicDto>,
    val weeklyPlan: List<DayPlanDto>,
    val rules: List<String>,
    val selfCheck: List<SelfCheckDto>
)

data class WeakTopicDto(
    val topicId: UUID,
    val topicName: String,
    val accuracy: Double,
    val mainMistake: String
)

data class DayPlanDto(
    val day: Int,
    val focus: String,
    val actions: List<String>,
    val timeMinutes: Int
)

data class SelfCheckDto(
    val question: String,
    val answer: String
)
