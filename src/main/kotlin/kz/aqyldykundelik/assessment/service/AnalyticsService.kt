package kz.aqyldykundelik.assessment.service

import kz.aqyldykundelik.assessment.api.dto.*
import kz.aqyldykundelik.assessment.repo.*
import kz.aqyldykundelik.timetable.repo.SubjectRepository
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
    private val topicRepository: TopicRepository,
    private val testRepository: TestRepository,
    private val classRepository: kz.aqyldykundelik.classes.repo.ClassRepository,
    private val subjectRepository: SubjectRepository
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

    // ============= NEW ANALYTICS METHODS (BACKEND-TASKS-4) =============

    // Student Analytics Methods

    fun getLastAttempt(studentId: UUID): LastAttemptDto? {
        val attempt = testAttemptRepository.findLastGradedAttemptByStudentId(studentId)
            ?: return null

        val test = testRepository.findById(attempt.testId).orElse(null)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found")

        return LastAttemptDto(
            attemptId = attempt.id!!,
            testId = attempt.testId,
            testName = test.name,
            finishedAt = attempt.finishedAt
        )
    }

    fun getAttemptSummary(attemptId: UUID, studentId: UUID): StudentAttemptSummaryDto {
        val attempt = testAttemptRepository.findById(attemptId).orElse(null)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found")

        if (attempt.studentId != studentId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
        }

        if (attempt.status != kz.aqyldykundelik.assessment.domain.AttemptStatus.GRADED) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Attempt is not graded yet")
        }

        return buildAttemptSummary(attempt)
    }

    fun getAttemptSummaries(studentId: UUID): List<StudentAttemptSummaryDto> {
        val attempts = testAttemptRepository.findByStudentIdAndStatusOrderByFinishedAtDesc(
            studentId,
            kz.aqyldykundelik.assessment.domain.AttemptStatus.GRADED
        )

        return attempts.map { attempt -> buildAttemptSummary(attempt) }
    }

    fun getAttemptTopics(attemptId: UUID, studentId: UUID): List<TopicScoreDto> {
        val attempt = testAttemptRepository.findById(attemptId).orElse(null)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found")

        if (attempt.studentId != studentId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
        }

        return calculateTopicScoresForAttempt(attemptId)
    }

    fun getAttemptTopicDetails(attemptId: UUID, topicId: UUID, studentId: UUID): List<QuestionDetailDto> {
        val attempt = testAttemptRepository.findById(attemptId).orElse(null)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found")

        if (attempt.studentId != studentId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
        }

        val answers = attemptAnswerRepository.findByAttemptId(attemptId)

        return answers.mapNotNull { answer ->
            val question = questionRepository.findById(answer.questionId).orElse(null) ?: return@mapNotNull null
            if (question.topicId != topicId) return@mapNotNull null

            QuestionDetailDto(
                questionId = question.id!!,
                text = question.text,
                choiceId = answer.choiceId,
                isCorrect = answer.isCorrect,
                explanation = question.explanation
            )
        }
    }

    private fun calculateTopicScoresForAttempt(attemptId: UUID): List<TopicScoreDto> {
        val answers = attemptAnswerRepository.findByAttemptId(attemptId)
        val topicStats = mutableMapOf<UUID, MutableMap<String, Any>>()

        answers.forEach { answer ->
            val question = questionRepository.findById(answer.questionId).orElse(null) ?: return@forEach
            val topic = topicRepository.findById(question.topicId).orElse(null) ?: return@forEach

            val stats = topicStats.getOrPut(topic.id!!) {
                mutableMapOf(
                    "topicId" to topic.id!!,
                    "topicName" to topic.name,
                    "correct" to 0,
                    "wrong" to 0,
                    "skipped" to 0,
                    "total" to 0
                )
            }

            stats["total"] = (stats["total"] as Int) + 1
            when (answer.isCorrect) {
                true -> stats["correct"] = (stats["correct"] as Int) + 1
                false -> stats["wrong"] = (stats["wrong"] as Int) + 1
                null -> stats["skipped"] = (stats["skipped"] as Int) + 1
            }
        }

        return topicStats.values.map { stats ->
            val correct = stats["correct"] as Int
            val total = stats["total"] as Int
            val percent = if (total > 0) {
                BigDecimal(correct * 100.0 / total).setScale(2, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }

            TopicScoreDto(
                topicId = stats["topicId"] as UUID,
                topicName = stats["topicName"] as String,
                total = total,
                correct = correct,
                wrong = stats["wrong"] as Int,
                skipped = stats["skipped"] as Int,
                percent = percent
            )
        }.sortedBy { it.percent }  // Sort by percent ascending (weakest first)
    }

    private fun buildAttemptSummary(attempt: kz.aqyldykundelik.assessment.domain.TestAttemptEntity): StudentAttemptSummaryDto {
        val test = testRepository.findById(attempt.testId).orElse(null)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found")
        val subject = subjectRepository.findById(test.subjectId).orElse(null)

        val answers = attemptAnswerRepository.findByAttemptId(attempt.id!!)
        val totalQuestions = answers.size
        val correctAnswers = answers.count { it.isCorrect == true }
        val wrongAnswers = answers.count { it.isCorrect == false }

        val topicScores = calculateTopicScoresForAttempt(attempt.id!!)
        val strongTopics = topicScores.filter { it.percent >= BigDecimal(75) }.map { it.topicName }
        val weakTopics = topicScores.filter { it.percent < BigDecimal(50) }.map { it.topicName }

        return StudentAttemptSummaryDto(
            testId = attempt.testId,
            testName = test.name,
            subjectId = test.subjectId,
            subjectNameRu = subject?.nameRu,
            subjectNameKk = subject?.nameKk,
            subjectNameEn = subject?.nameEn,
            attemptId = attempt.id!!,
            totalQuestions = totalQuestions,
            correctAnswers = correctAnswers,
            wrongAnswers = wrongAnswers,
            percent = attempt.percent,
            strongTopics = strongTopics,
            weakTopics = weakTopics,
            attemptDate = attempt.finishedAt
        )
    }

    // Teacher Analytics Methods

    fun getClassesList(teacherId: UUID?): List<ClassInfoDto> {
        val classes = if (teacherId != null) {
            classRepository.findByClassTeacherIdOrderByGradeAndLetter(teacherId, org.springframework.data.domain.Pageable.unpaged()).content
        } else {
            classRepository.findAllOrderByGradeAndLetter()
        }

        return classes.map { classEntity ->
            ClassInfoDto(
                classId = classEntity.id!!,
                className = classEntity.code ?: "N/A"
            )
        }
    }

    fun getClassLastTestSummary(classId: UUID): ClassTestSummaryDto? {
        val lastAttempt = testAttemptRepository.findLastGradedAttemptByClassId(classId)
            ?: return null

        val testId = lastAttempt.testId
        val test = testRepository.findById(testId).orElse(null)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found")

        // Get latest attempts for each student on this test
        val attempts = testAttemptRepository.findLatestGradedAttemptsByClassAndTest(classId, testId)

        if (attempts.isEmpty()) {
            return null
        }

        // Calculate average and median percent
        val percents = attempts.map { it.percent.toDouble() }.sorted()
        val avgPercent = BigDecimal(percents.average()).setScale(2, RoundingMode.HALF_UP)
        val medianPercent = if (percents.isNotEmpty()) {
            val middle = percents.size / 2
            val median = if (percents.size % 2 == 0) {
                (percents[middle - 1] + percents[middle]) / 2.0
            } else {
                percents[middle]
            }
            BigDecimal(median).setScale(2, RoundingMode.HALF_UP)
        } else {
            null
        }

        // Calculate weak topics
        val topicScores = calculateClassTopicScores(classId, testId)
        val weakTopics = topicScores.filter { it.avgPercent < BigDecimal(50) }.map { it.topicName }

        // Count risk students (below 40%)
        val riskStudentsCount = attempts.count { it.percent < BigDecimal(40) }

        return ClassTestSummaryDto(
            testId = testId,
            testName = test.name,
            testDate = lastAttempt.finishedAt,
            avgPercent = avgPercent,
            medianPercent = medianPercent,
            weakTopics = weakTopics,
            riskStudentsCount = riskStudentsCount
        )
    }

    fun getClassTestTopics(classId: UUID, testId: UUID): List<ClassTopicAnalyticsDto> {
        return calculateClassTopicScores(classId, testId)
    }

    fun getClassTestTopicDetails(classId: UUID, testId: UUID, topicId: UUID): TopicDrillDownDto {
        val attempts = testAttemptRepository.findLatestGradedAttemptsByClassAndTest(classId, testId)

        if (attempts.isEmpty()) {
            return TopicDrillDownDto(topWeakQuestions = emptyList())
        }

        val attemptIds = attempts.mapNotNull { it.id }
        val allAnswers = attemptIds.flatMap { attemptAnswerRepository.findByAttemptId(it) }

        // Group by question and calculate wrong percentage
        val questionStats = mutableMapOf<UUID, MutableMap<String, Any>>()

        allAnswers.forEach { answer ->
            val question = questionRepository.findById(answer.questionId).orElse(null) ?: return@forEach
            if (question.topicId != topicId) return@forEach

            val stats = questionStats.getOrPut(question.id!!) {
                mutableMapOf(
                    "questionId" to question.id!!,
                    "text" to question.text,
                    "answersCount" to 0,
                    "wrongCount" to 0
                )
            }

            stats["answersCount"] = (stats["answersCount"] as Int) + 1
            if (answer.isCorrect == false) {
                stats["wrongCount"] = (stats["wrongCount"] as Int) + 1
            }
        }

        // Convert to DTOs and sort by wrong percentage
        val worstQuestions = questionStats.values.map { stats ->
            val answersCount = stats["answersCount"] as Int
            val wrongCount = stats["wrongCount"] as Int
            val wrongPercent = if (answersCount > 0) {
                BigDecimal(wrongCount * 100.0 / answersCount).setScale(2, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }

            WorstQuestionDto(
                questionId = stats["questionId"] as UUID,
                text = stats["text"] as String,
                answersCount = answersCount,
                wrongCount = wrongCount,
                wrongPercent = wrongPercent
            )
        }.sortedByDescending { it.wrongPercent }
            .take(10)  // Top 10 worst questions

        return TopicDrillDownDto(topWeakQuestions = worstQuestions)
    }

    private fun calculateClassTopicScores(classId: UUID, testId: UUID): List<ClassTopicAnalyticsDto> {
        val attempts = testAttemptRepository.findLatestGradedAttemptsByClassAndTest(classId, testId)

        if (attempts.isEmpty()) {
            return emptyList()
        }

        // Calculate per-student per-topic scores
        val studentTopicScores = mutableMapOf<UUID, MutableMap<UUID, MutableMap<String, Any>>>()

        attempts.forEach { attempt ->
            val answers = attemptAnswerRepository.findByAttemptId(attempt.id!!)
            val topicScores = mutableMapOf<UUID, MutableMap<String, Any>>()

            answers.forEach { answer ->
                val question = questionRepository.findById(answer.questionId).orElse(null) ?: return@forEach
                val topic = topicRepository.findById(question.topicId).orElse(null) ?: return@forEach

                val stats = topicScores.getOrPut(topic.id!!) {
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

            studentTopicScores[attempt.studentId] = topicScores
        }

        // Aggregate across students
        val topicAggregates = mutableMapOf<UUID, MutableMap<String, Any>>()

        studentTopicScores.values.forEach { studentTopics ->
            studentTopics.values.forEach { topicStats ->
                val topicId = topicStats["topicId"] as UUID
                val topicName = topicStats["topicName"] as String
                val correct = topicStats["correct"] as Int
                val total = topicStats["total"] as Int
                val studentPercent = if (total > 0) correct.toDouble() / total else 0.0

                val agg = topicAggregates.getOrPut(topicId) {
                    mutableMapOf(
                        "topicId" to topicId,
                        "topicName" to topicName,
                        "percentages" to mutableListOf<Double>(),
                        "studentsCount" to 0
                    )
                }

                @Suppress("UNCHECKED_CAST")
                (agg["percentages"] as MutableList<Double>).add(studentPercent)
                agg["studentsCount"] = (agg["studentsCount"] as Int) + 1
            }
        }

        // Calculate average and median
        return topicAggregates.values.map { agg ->
            @Suppress("UNCHECKED_CAST")
            val percentages = (agg["percentages"] as List<Double>).sorted()
            val avgPercent = BigDecimal(percentages.average() * 100.0).setScale(2, RoundingMode.HALF_UP)
            val medianPercent = if (percentages.isNotEmpty()) {
                val middle = percentages.size / 2
                val median = if (percentages.size % 2 == 0) {
                    (percentages[middle - 1] + percentages[middle]) / 2.0
                } else {
                    percentages[middle]
                }
                BigDecimal(median * 100.0).setScale(2, RoundingMode.HALF_UP)
            } else {
                null
            }

            ClassTopicAnalyticsDto(
                topicId = agg["topicId"] as UUID,
                topicName = agg["topicName"] as String,
                avgPercent = avgPercent,
                medianPercent = medianPercent,
                studentsCount = agg["studentsCount"] as Int
            )
        }.sortedBy { it.avgPercent }  // Sort by average percent ascending (weakest first)
    }
}
