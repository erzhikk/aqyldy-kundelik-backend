package kz.aqyldykundelik.assessment.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import kz.aqyldykundelik.assessment.domain.Difficulty
import kz.aqyldykundelik.assessment.domain.ReviewPolicy
import kz.aqyldykundelik.assessment.domain.TestStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
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
    val description: String?,
    val createdByFullName: String?,
    val createdAt: OffsetDateTime?,
    val questionsCount: Long?
)

data class TopicDetailsDto(
    val id: UUID,
    val subjectId: UUID,
    val subjectName: String?,
    val name: String,
    val description: String?,
    val createdByFullName: String?,
    val createdAt: OffsetDateTime?
)

data class UpdateTopicDto(
    @field:NotBlank val name: String,
    val description: String? = null
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

// ============= SCHOOL CLASS DTO =============

data class SchoolClassDto(
    val id: UUID,
    val code: String,
    val classLevel: Int?
)

// ============= TEST DTOs =============

data class CreateTestDto(
    @field:NotNull val classLevelId: UUID,
    @field:NotNull val subjectId: UUID,
    val schoolClassIds: List<UUID> = emptyList(),
    @field:NotBlank val name: String,
    val description: String? = null,
    val durationSec: Int? = null,
    val allowedAttempts: Int? = null,
    val opensAt: OffsetDateTime? = null,
    val closesAt: OffsetDateTime? = null,
    val shuffleQuestions: Boolean = true,
    val shuffleChoices: Boolean = true,
    val passingPercent: BigDecimal? = null,
    val reviewPolicy: ReviewPolicy? = null
)

data class UpdateTestDto(
    @field:NotNull val classLevelId: UUID,
    @field:NotBlank val name: String,
    val schoolClassIds: List<UUID> = emptyList(),
    val description: String? = null,
    val durationSec: Int? = null,
    val allowedAttempts: Int? = null,
    val opensAt: OffsetDateTime? = null,
    val closesAt: OffsetDateTime? = null,
    val shuffleQuestions: Boolean = true,
    val shuffleChoices: Boolean = true,
    val passingPercent: BigDecimal? = null,
    val reviewPolicy: ReviewPolicy? = null
)

data class TestDto(
    val id: UUID,
    val classLevelId: UUID,
    val subjectId: UUID,
    val subjectNameRu: String?,
    val subjectNameKk: String?,
    val subjectNameEn: String?,
    val classLevel: Int?,
    val schoolClasses: List<SchoolClassDto>,
    val name: String,
    val description: String?,
    val durationSec: Int?,
    val maxScore: Int,
    val status: TestStatus,
    val shuffleQuestions: Boolean,
    val shuffleChoices: Boolean,
    val allowedAttempts: Int?,
    val opensAt: OffsetDateTime?,
    val closesAt: OffsetDateTime?,
    val passingPercent: BigDecimal?,
    val reviewPolicy: ReviewPolicy?
)

data class TestDetailDto(
    val id: UUID,
    val classLevelId: UUID,
    val subjectId: UUID,
    val subjectNameRu: String?,
    val subjectNameKk: String?,
    val subjectNameEn: String?,
    val classLevel: Int?,
    val schoolClasses: List<SchoolClassDto>,
    val name: String,
    val description: String?,
    val durationSec: Int?,
    val maxScore: Int,
    val status: TestStatus,
    val shuffleQuestions: Boolean,
    val shuffleChoices: Boolean,
    val allowedAttempts: Int?,
    val opensAt: OffsetDateTime?,
    val closesAt: OffsetDateTime?,
    val passingPercent: BigDecimal?,
    val reviewPolicy: ReviewPolicy?,
    val topics: List<TestTopicDto>
)

data class TestQuestionDto(
    val questionId: UUID,
    val text: String,
    val difficulty: Difficulty,
    val order: Int,
    val weight: Int,
    val choices: List<ChoiceDto>  // With isCorrect for teachers/admins
)

data class TestTopicDto(
    val topicId: UUID,
    val topicName: String,
    val topicDescription: String?,
    val questions: List<TestQuestionDto>
)

// ============= TEST QUESTION MANAGEMENT DTOs =============

data class TestQuestionItemDto(
    @field:NotNull val questionId: UUID,
    val weight: Int = 1,
    val order: Int
)

data class AddQuestionsToTestDto(
    @field:NotEmpty val questions: List<TestQuestionItemDto>
)

data class ReorderQuestionsDto(
    @field:NotEmpty val questions: List<TestQuestionItemDto>
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

// ============= NEW ANALYTICS DTOs (BACKEND-TASKS-4) =============

// Student Analytics DTOs

data class LastAttemptDto(
    val attemptId: UUID,
    val testId: UUID,
    val testName: String,
    val finishedAt: OffsetDateTime?
)

data class StudentAttemptSummaryDto(
    val testId: UUID,
    val testName: String,
    val subjectId: UUID,
    val subjectNameRu: String?,
    val subjectNameKk: String?,
    val subjectNameEn: String?,
    val attemptId: UUID,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val percent: BigDecimal,
    val strongTopics: List<String>,
    val weakTopics: List<String>,
    val attemptDate: OffsetDateTime?
)

data class TopicScoreDto(
    val topicId: UUID,
    val topicName: String,
    val total: Int,
    val correct: Int,
    val wrong: Int,
    val skipped: Int?,
    val percent: BigDecimal
)

data class QuestionDetailDto(
    val questionId: UUID,
    val text: String,
    val choiceId: UUID?,
    val isCorrect: Boolean?,
    val explanation: String?
)

// Teacher Analytics DTOs

data class ClassInfoDto(
    val classId: UUID,
    val className: String
)

data class ClassTestSummaryDto(
    val testId: UUID,
    val testName: String,
    val testDate: OffsetDateTime?,
    val avgPercent: BigDecimal,
    val medianPercent: BigDecimal?,
    val weakTopics: List<String>,
    val riskStudentsCount: Int
)

data class ClassTopicAnalyticsDto(
    val topicId: UUID,
    val topicName: String,
    val avgPercent: BigDecimal,
    val medianPercent: BigDecimal?,
    val studentsCount: Int
)

data class WorstQuestionDto(
    val questionId: UUID,
    val text: String,
    val answersCount: Int,
    val wrongCount: Int,
    val wrongPercent: BigDecimal
)

data class TopicDrillDownDto(
    val topWeakQuestions: List<WorstQuestionDto>
)
