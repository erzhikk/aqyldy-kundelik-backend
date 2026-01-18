package kz.aqyldykundelik.ai.dto

import java.util.UUID

data class AiTopicHelpResponseDto(
    val topic: TopicRefDto,
    val mainError: String,
    val explanation: String,
    val examples: List<ExampleDto>,
    val practice: List<PracticeDto>
)

data class TopicRefDto(
    val topicId: UUID,
    val topicName: String
)

data class ExampleDto(
    val question: String,
    val solution: String
)

data class PracticeDto(
    val question: String,
    val answer: String
)
