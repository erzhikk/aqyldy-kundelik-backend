package kz.aqyldykundelik.assessment.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import kz.aqyldykundelik.assessment.domain.Difficulty
import java.util.*

// ============= TOPIC DTOs =============

data class CreateTopicDto(
    @field:NotNull val subjectId: UUID,
    @field:NotBlank val name: String,
    val description: String? = null
)

data class TopicDto(
    val id: UUID,
    val subjectId: UUID,
    val name: String,
    val description: String?
)

// ============= QUESTION DTOs =============

data class CreateChoiceDto(
    @field:NotBlank val text: String,
    @field:NotNull val isCorrect: Boolean,
    val order: Int = 0,
    val mediaId: UUID? = null
)

data class ChoiceDto(
    val id: UUID,
    val text: String,
    val isCorrect: Boolean,  // Will be hidden for student views
    val order: Int,
    val mediaId: UUID?
)

data class CreateQuestionDto(
    @field:NotNull val topicId: UUID,
    @field:NotBlank val text: String,
    val mediaId: UUID? = null,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val explanation: String? = null,
    @field:NotEmpty val choices: List<CreateChoiceDto>
)

data class UpdateQuestionDto(
    @field:NotBlank val text: String,
    val mediaId: UUID? = null,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val explanation: String? = null,
    @field:NotEmpty val choices: List<CreateChoiceDto>
)

data class QuestionDto(
    val id: UUID,
    val topicId: UUID,
    val text: String,
    val mediaId: UUID?,
    val difficulty: Difficulty,
    val explanation: String?,
    val choices: List<ChoiceDto>
)

// ============= TEST DTOs =============

data class CreateTestDto(
    @field:NotNull val subjectId: UUID,
    @field:NotBlank val name: String,
    val grade: Int? = null,
    val durationSec: Int? = null,
    val shuffleQuestions: Boolean = true,
    val shuffleChoices: Boolean = true,
    val allowedAttempts: Int? = null
)

data class UpdateTestDto(
    @field:NotBlank val name: String,
    val grade: Int? = null,
    val durationSec: Int? = null,
    val shuffleQuestions: Boolean = true,
    val shuffleChoices: Boolean = true,
    val allowedAttempts: Int? = null
)

data class TestDto(
    val id: UUID,
    val subjectId: UUID,
    val name: String,
    val grade: Int?,
    val durationSec: Int?,
    val maxScore: Int?,
    val isPublished: Boolean,
    val shuffleQuestions: Boolean,
    val shuffleChoices: Boolean,
    val allowedAttempts: Int?
)

data class TestDetailDto(
    val id: UUID,
    val subjectId: UUID,
    val name: String,
    val grade: Int?,
    val durationSec: Int?,
    val maxScore: Int?,
    val isPublished: Boolean,
    val shuffleQuestions: Boolean,
    val shuffleChoices: Boolean,
    val allowedAttempts: Int?,
    val questions: List<TestQuestionDto>
)

data class TestQuestionDto(
    val questionId: UUID,
    val text: String,
    val difficulty: Difficulty,
    val order: Int,
    val weight: Int,
    val choices: List<ChoiceDto>  // isCorrect will be hidden for students
)

// ============= TEST QUESTION MANAGEMENT DTOs =============

data class TestQuestionItemDto(
    @field:NotNull val questionId: UUID,
    val weight: Int = 1,
    val order: Int
)

data class AddQuestionsToTestDto(
    @field:NotEmpty val items: List<TestQuestionItemDto>
)

data class ReorderQuestionsDto(
    @field:NotEmpty val items: List<TestQuestionItemDto>
)

// ============= STUDENT ATTEMPT DTOs =============

data class AttemptQuestionDto(
    val id: UUID,
    val text: String,
    val mediaId: UUID?,
    val choices: List<AttemptChoiceDto>  // No isCorrect for students!
)

data class AttemptChoiceDto(
    val id: UUID,
    val text: String,
    val order: Int,
    val mediaId: UUID?
)

data class StartAttemptResponseDto(
    val attemptId: UUID,
    val testId: UUID,
    val durationSec: Int?,
    val questions: List<AttemptQuestionDto>
)

data class AnswerDto(
    @field:NotNull val questionId: UUID,
    @field:NotNull val choiceId: UUID
)

data class SaveAnswersDto(
    @field:NotEmpty val answers: List<AnswerDto>
)

data class SubmitAttemptResponseDto(
    val score: Int,
    val percent: Double,
    val status: String  // "GRADED"
)

data class TopicResultDto(
    val topicId: UUID,
    val topicName: String,
    val correct: Int,
    val total: Int,
    val percent: Double
)

data class QuestionReviewDto(
    val questionId: UUID,
    val text: String,
    val correct: Boolean,
    val yourAnswer: UUID?,  // choiceId
    val correctAnswer: UUID,  // choiceId
    val explanation: String?
)

data class AttemptResultDto(
    val score: Int,
    val percent: Double,
    val status: String,
    val byTopics: List<TopicResultDto>,
    val reviewAllowed: Boolean,
    val review: List<QuestionReviewDto>?
)

// ============= ANALYTICS DTOs =============

data class StudentTopicAnalyticsDto(
    val topicId: UUID,
    val topicName: String,
    val correct: Int,
    val total: Int,
    val percent: Double
)

data class ScoreDistributionDto(
    val range: String,  // e.g., "0-20", "21-40", "41-60", "61-80", "81-100"
    val count: Int
)

data class WeakTopicDto(
    val topicId: UUID,
    val topicName: String,
    val avgPercent: Double
)

data class ClassTestAnalyticsDto(
    val avgPercent: Double,
    val distribution: List<ScoreDistributionDto>,
    val topWeakTopics: List<WeakTopicDto>
)
