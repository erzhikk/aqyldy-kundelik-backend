package kz.aqyldykundelik.assessment.service

import kz.aqyldykundelik.assessment.api.dto.*
import kz.aqyldykundelik.assessment.repo.*
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime
import java.util.*

@Service
class AnalyticsService(
    private val testAttemptRepository: TestAttemptRepository,
    private val attemptAnswerRepository: AttemptAnswerRepository,
    private val questionRepository: QuestionRepository,
    private val topicRepository: TopicRepository
) {

    fun getStudentTopicAnalytics(
        studentId: UUID,
        subjectId: UUID?,
        from: OffsetDateTime?,
        to: OffsetDateTime?
    ): List<StudentTopicAnalyticsDto> {
        // Get all graded attempts for the student with filters
        val attempts = testAttemptRepository.findGradedAttemptsForStudent(studentId, subjectId, from, to)

        if (attempts.isEmpty()) {
            return emptyList()
        }

        // Get all answers for these attempts
        val attemptIds = attempts.map { it.id!! }
        val allAnswers = attemptIds.flatMap { attemptAnswerRepository.findByAttemptId(it) }

        // Group answers by topic
        val topicStats = mutableMapOf<UUID, MutableMap<String, Any>>()

        allAnswers.forEach { answer ->
            val question = questionRepository.findById(answer.questionId).orElse(null) ?: return@forEach
            val topic = topicRepository.findById(question.topicId).orElse(null) ?: return@forEach

            val stats = topicStats.getOrPut(topic.id!!) {
                mutableMapOf(
                    "topicId" to topic.id!!,
                    "topicName" to topic.name,
                    "correct" to 0,
                    "total" to 0
                )
            }

            stats["total"] = (stats["total"] as Int) + 1
            if (answer.isCorrect == true) {
                stats["correct"] = (stats["correct"] as Int) + 1
            }
        }

        // Convert to DTOs
        return topicStats.values.map { stats ->
            val correct = stats["correct"] as Int
            val total = stats["total"] as Int
            val percent = if (total > 0) {
                BigDecimal(correct * 100.0 / total).setScale(2, RoundingMode.HALF_UP).toDouble()
            } else {
                0.0
            }

            StudentTopicAnalyticsDto(
                topicId = stats["topicId"] as UUID,
                topicName = stats["topicName"] as String,
                correct = correct,
                total = total,
                percent = percent
            )
        }.sortedByDescending { it.total }  // Sort by total questions answered
    }

    fun getClassTestAnalytics(classId: UUID, testId: UUID): ClassTestAnalyticsDto {
        // Get all graded attempts for the class on this test
        val attempts = testAttemptRepository.findGradedAttemptsByClassAndTest(classId, testId)

        if (attempts.isEmpty()) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "No graded attempts found for this class and test")
        }

        // Calculate average percent
        val avgPercent = attempts.mapNotNull { it.percent }
            .map { it.toDouble() }
            .average()
            .let { BigDecimal(it).setScale(2, RoundingMode.HALF_UP).toDouble() }

        // Calculate distribution
        val distribution = calculateDistribution(attempts)

        // Calculate weak topics
        val weakTopics = calculateWeakTopics(attempts)

        return ClassTestAnalyticsDto(
            avgPercent = avgPercent,
            distribution = distribution,
            topWeakTopics = weakTopics
        )
    }

    private fun calculateDistribution(attempts: List<kz.aqyldykundelik.assessment.domain.TestAttemptEntity>): List<ScoreDistributionDto> {
        val ranges = listOf(
            "0-20" to (0.0..20.0),
            "21-40" to (20.01..40.0),
            "41-60" to (40.01..60.0),
            "61-80" to (60.01..80.0),
            "81-100" to (80.01..100.0)
        )

        return ranges.map { (rangeName, range) ->
            val count = attempts.count { attempt ->
                val percent = attempt.percent.toDouble()
                percent in range
            }
            ScoreDistributionDto(range = rangeName, count = count)
        }
    }

    private fun calculateWeakTopics(attempts: List<kz.aqyldykundelik.assessment.domain.TestAttemptEntity>): List<WeakTopicDto> {
        // Get all answers for these attempts
        val attemptIds = attempts.map { it.id!! }
        val allAnswers = attemptIds.flatMap { attemptAnswerRepository.findByAttemptId(it) }

        // Group by topic and calculate average correctness
        val topicStats = mutableMapOf<UUID, MutableMap<String, Any>>()

        allAnswers.forEach { answer ->
            val question = questionRepository.findById(answer.questionId).orElse(null) ?: return@forEach
            val topic = topicRepository.findById(question.topicId).orElse(null) ?: return@forEach

            val stats = topicStats.getOrPut(topic.id!!) {
                mutableMapOf(
                    "topicId" to topic.id!!,
                    "topicName" to topic.name,
                    "correct" to 0,
                    "total" to 0
                )
            }

            stats["total"] = (stats["total"] as Int) + 1
            if (answer.isCorrect == true) {
                stats["correct"] = (stats["correct"] as Int) + 1
            }
        }

        // Convert to DTOs and sort by percent (ascending = weakest first)
        return topicStats.values.map { stats ->
            val correct = stats["correct"] as Int
            val total = stats["total"] as Int
            val percent = if (total > 0) {
                BigDecimal(correct * 100.0 / total).setScale(2, RoundingMode.HALF_UP).toDouble()
            } else {
                0.0
            }

            WeakTopicDto(
                topicId = stats["topicId"] as UUID,
                topicName = stats["topicName"] as String,
                avgPercent = percent
            )
        }.sortedBy { it.avgPercent }  // Sort by percent ascending (weakest first)
            .take(5)  // Top 5 weakest topics
    }
}
