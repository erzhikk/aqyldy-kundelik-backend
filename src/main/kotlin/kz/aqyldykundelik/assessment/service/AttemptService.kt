package kz.aqyldykundelik.assessment.service

import jakarta.transaction.Transactional
import kz.aqyldykundelik.assessment.api.dto.*
import kz.aqyldykundelik.assessment.domain.*
import kz.aqyldykundelik.assessment.repo.*
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime
import java.util.*

@Service
class AttemptService(
    private val testRepository: TestRepository,
    private val testQuestionRepository: TestQuestionRepository,
    private val questionRepository: QuestionRepository,
    private val choiceRepository: ChoiceRepository,
    private val topicRepository: TopicRepository,
    private val testAttemptRepository: TestAttemptRepository,
    private val attemptAnswerRepository: AttemptAnswerRepository,
    private val auditService: AuditService
) {

    @Transactional
    fun startAttempt(testId: UUID, studentId: UUID): StartAttemptResponseDto {
        val test = testRepository.findById(testId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found") }

        // Проверка: тест должен быть опубликован
        if (test.status != kz.aqyldykundelik.assessment.domain.TestStatus.PUBLISHED) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Test is not published")
        }

        // Проверка: лимит попыток
        test.allowedAttempts?.let { limit ->
            val existingAttempts = testAttemptRepository.findByStudentIdAndTestId(studentId, testId)
            if (existingAttempts.size >= limit) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Maximum number of attempts ($limit) reached"
                )
            }
        }

        // Создаём новую попытку
        val attempt = TestAttemptEntity(
            testId = testId,
            studentId = studentId,
            status = AttemptStatus.IN_PROGRESS
        )
        val savedAttempt = testAttemptRepository.save(attempt)

        // Audit log
        auditService.log(
            eventType = AuditService.ATTEMPT_STARTED,
            entityType = AuditService.ENTITY_ATTEMPT,
            entityId = savedAttempt.id!!,
            userId = studentId,
            metadata = mapOf(
                "testId" to testId.toString(),
                "testName" to test.name,
                "durationSec" to (test.durationSec ?: "unlimited")
            )
        )

        // Получаем вопросы теста
        val testQuestions = testQuestionRepository.findByTestIdOrderByOrderAsc(testId)

        val questionDtos = testQuestions.map { tq ->
            val question = questionRepository.findById(tq.questionId)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found") }

            val choices = choiceRepository.findByQuestionId(tq.questionId)

            // Применяем shuffle для choices если нужно
            val orderedChoices = if (test.shuffleChoices) {
                choices.shuffled()
            } else {
                choices.sortedBy { it.order }
            }

            AttemptQuestionDto(
                id = question.id!!,
                text = question.text,
                mediaId = question.mediaId,
                choices = orderedChoices.map { choice ->
                    AttemptChoiceDto(
                        id = choice.id!!,
                        text = choice.text,
                        order = choice.order,
                        mediaId = choice.mediaId
                        // НЕТ isCorrect - скрыто от студента!
                    )
                }
            )
        }

        // Применяем shuffle для вопросов если нужно
        val orderedQuestions = if (test.shuffleQuestions) {
            questionDtos.shuffled()
        } else {
            questionDtos
        }

        return StartAttemptResponseDto(
            attemptId = savedAttempt.id!!,
            testId = testId,
            durationSec = test.durationSec,
            questions = orderedQuestions
        )
    }

    @Transactional
    fun saveAnswers(attemptId: UUID, studentId: UUID, dto: SaveAnswersDto) {
        val attempt = testAttemptRepository.findById(attemptId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found") }

        // Проверка: попытка принадлежит студенту
        if (attempt.studentId != studentId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
        }

        // Проверка: попытка в статусе IN_PROGRESS
        if (attempt.status != AttemptStatus.IN_PROGRESS) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Attempt is already submitted")
        }

        // Проверка: таймер (если есть)
        attempt.startedAt?.let { startedAt ->
            val test = testRepository.findById(attempt.testId).orElseThrow()
            test.durationSec?.let { duration ->
                val elapsed = OffsetDateTime.now().toEpochSecond() - startedAt.toEpochSecond()
                if (elapsed > duration) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Time limit exceeded")
                }
            }
        }

        // Сохраняем ответы (заменяем существующие)
        dto.answers.forEach { answer ->
            // Проверяем, есть ли уже ответ на этот вопрос
            val existing = attemptAnswerRepository.findByAttemptId(attemptId)
                .firstOrNull { it.questionId == answer.questionId }

            if (existing != null) {
                // Обновляем существующий ответ
                existing.choiceId = answer.choiceId
                attemptAnswerRepository.save(existing)
            } else {
                // Создаём новый ответ
                val answerEntity = AttemptAnswerEntity(
                    attemptId = attemptId,
                    questionId = answer.questionId,
                    choiceId = answer.choiceId
                )
                attemptAnswerRepository.save(answerEntity)
            }
        }
    }

    @Transactional
    fun submitAttempt(attemptId: UUID, studentId: UUID): SubmitAttemptResponseDto {
        val attempt = testAttemptRepository.findById(attemptId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found") }

        // Проверка: попытка принадлежит студенту
        if (attempt.studentId != studentId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
        }

        // Проверка: попытка в статусе IN_PROGRESS
        if (attempt.status != AttemptStatus.IN_PROGRESS) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Attempt is already submitted")
        }

        // Проверка: таймер (если есть)
        attempt.startedAt?.let { startedAt ->
            val test = testRepository.findById(attempt.testId).orElseThrow()
            test.durationSec?.let { duration ->
                val elapsed = OffsetDateTime.now().toEpochSecond() - startedAt.toEpochSecond()
                if (elapsed > duration) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Time limit exceeded")
                }
            }
        }

        // Получаем тест
        val test = testRepository.findById(attempt.testId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found") }

        // Получаем все вопросы теста
        val testQuestions = testQuestionRepository.findByTestIdOrderByOrderAsc(attempt.testId)

        // Получаем ответы студента
        val studentAnswers = attemptAnswerRepository.findByAttemptId(attemptId)

        var totalScore = 0

        // Оцениваем каждый ответ
        testQuestions.forEach { tq ->
            val studentAnswer = studentAnswers.firstOrNull { it.questionId == tq.questionId }

            if (studentAnswer != null) {
                // Получаем правильный ответ
                val choices = choiceRepository.findByQuestionId(tq.questionId)
                val correctChoice = choices.firstOrNull { it.isCorrect }

                val isCorrect = correctChoice?.id == studentAnswer.choiceId
                val scoreDelta = if (isCorrect) tq.weight else 0

                // Обновляем ответ
                studentAnswer.isCorrect = isCorrect
                studentAnswer.scoreDelta = scoreDelta
                attemptAnswerRepository.save(studentAnswer)

                totalScore += scoreDelta
            }
        }

        // Вычисляем процент
        val maxScore = test.maxScore ?: 0
        val percent = if (maxScore > 0) {
            BigDecimal(totalScore * 100.0 / maxScore).setScale(2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        // Обновляем попытку
        attempt.status = AttemptStatus.GRADED
        attempt.finishedAt = OffsetDateTime.now()
        attempt.score = totalScore
        attempt.percent = percent
        val savedAttempt = testAttemptRepository.save(attempt)

        // Audit log
        auditService.log(
            eventType = AuditService.ATTEMPT_COMPLETED,
            entityType = AuditService.ENTITY_ATTEMPT,
            entityId = savedAttempt.id!!,
            userId = studentId,
            metadata = mapOf(
                "testId" to test.id.toString(),
                "testName" to test.name,
                "score" to totalScore,
                "percent" to percent.toDouble(),
                "maxScore" to maxScore
            )
        )

        return SubmitAttemptResponseDto(
            score = totalScore,
            percent = percent.toDouble(),
            status = "GRADED"
        )
    }

    fun getAttemptResult(attemptId: UUID, studentId: UUID): AttemptResultDto {
        val attempt = testAttemptRepository.findById(attemptId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found") }

        // Проверка: попытка принадлежит студенту
        if (attempt.studentId != studentId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
        }

        // Проверка: попытка должна быть завершена
        if (attempt.status != AttemptStatus.GRADED) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Attempt is not graded yet")
        }

        // Получаем ответы студента
        val studentAnswers = attemptAnswerRepository.findByAttemptId(attemptId)

        // Группируем по темам
        val topicStats = mutableMapOf<UUID, MutableMap<String, Any>>()

        studentAnswers.forEach { answer ->
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

        val byTopics = topicStats.values.map { stats ->
            val correct = stats["correct"] as Int
            val total = stats["total"] as Int
            val percent = if (total > 0) (correct * 100.0 / total) else 0.0

            TopicResultDto(
                topicId = stats["topicId"] as UUID,
                topicName = stats["topicName"] as String,
                correct = correct,
                total = total,
                percent = BigDecimal(percent).setScale(2, RoundingMode.HALF_UP).toDouble()
            )
        }

        // Формируем разбор (review)
        val review = studentAnswers.map { answer ->
            val question = questionRepository.findById(answer.questionId).orElseThrow()
            val choices = choiceRepository.findByQuestionId(answer.questionId)
            val correctChoice = choices.first { it.isCorrect }

            QuestionReviewDto(
                questionId = question.id!!,
                text = question.text,
                correct = answer.isCorrect ?: false,
                yourAnswer = answer.choiceId,
                correctAnswer = correctChoice.id!!,
                explanation = question.explanation
            )
        }

        return AttemptResultDto(
            score = attempt.score,
            percent = attempt.percent.toDouble(),
            status = attempt.status.name,
            byTopics = byTopics,
            reviewAllowed = true,  // Можно добавить логику разрешений
            review = review
        )
    }
}
