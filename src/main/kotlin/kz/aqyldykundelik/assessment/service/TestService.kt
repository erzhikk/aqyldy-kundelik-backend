package kz.aqyldykundelik.assessment.service

import jakarta.transaction.Transactional
import kz.aqyldykundelik.assessment.api.dto.*
import kz.aqyldykundelik.assessment.domain.TestEntity
import kz.aqyldykundelik.assessment.domain.TestQuestionEntity
import kz.aqyldykundelik.assessment.domain.TestQuestionId
import kz.aqyldykundelik.assessment.repo.*
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Service
class TestService(
    private val testRepository: TestRepository,
    private val testQuestionRepository: TestQuestionRepository,
    private val questionRepository: QuestionRepository,
    private val choiceRepository: ChoiceRepository,
    private val topicRepository: TopicRepository,
    private val testAttemptRepository: TestAttemptRepository,
    private val auditService: AuditService
) {

    fun create(dto: CreateTestDto): TestDto {
        val entity = TestEntity(
            subjectId = dto.subjectId,
            name = dto.name,
            grade = dto.grade,
            durationSec = dto.durationSec,
            maxScore = 0,  // Will be calculated when questions are added
            shuffleQuestions = dto.shuffleQuestions,
            shuffleChoices = dto.shuffleChoices,
            allowedAttempts = dto.allowedAttempts
        )
        val saved = testRepository.save(entity)

        // Audit log
        auditService.log(
            eventType = AuditService.TEST_CREATED,
            entityType = AuditService.ENTITY_TEST,
            entityId = saved.id!!,
            userId = getCurrentUserId(),
            metadata = mapOf(
                "testName" to saved.name,
                "subjectId" to saved.subjectId.toString(),
                "grade" to (saved.grade ?: "null")
            )
        )

        return saved.toDto()
    }

    @Transactional
    fun update(id: UUID, dto: UpdateTestDto): TestDto {
        val test = testRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found") }

        if (test.isPublished) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot update published test")
        }

        test.name = dto.name
        test.grade = dto.grade
        test.durationSec = dto.durationSec
        test.shuffleQuestions = dto.shuffleQuestions
        test.shuffleChoices = dto.shuffleChoices
        test.allowedAttempts = dto.allowedAttempts

        val saved = testRepository.save(test)
        return saved.toDto()
    }

    @Transactional
    fun delete(id: UUID) {
        val test = testRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found") }

        // Check if there are any attempts
        val attemptCount = testAttemptRepository.countByTestId(id)
        if (attemptCount > 0) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Cannot delete test with existing attempts"
            )
        }

        // Delete test questions (cascade will handle it, but explicit for clarity)
        testQuestionRepository.deleteByTestId(id)
        testRepository.deleteById(id)
    }

    fun findAll(subjectId: UUID?, grade: Int?, published: Boolean?): List<TestDto> {
        val tests = when {
            subjectId != null && published != null ->
                testRepository.findBySubjectIdAndIsPublished(subjectId, published)
            subjectId != null ->
                testRepository.findBySubjectId(subjectId)
            grade != null ->
                testRepository.findByGrade(grade)
            published != null ->
                testRepository.findByIsPublished(published)
            else ->
                testRepository.findAll()
        }
        return tests.map { it.toDto() }
    }

    fun findById(id: UUID): TestDetailDto {
        val test = testRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found") }

        val testQuestions = testQuestionRepository.findByTestIdOrderByOrderAsc(id)

        val questionDtos = testQuestions.map { tq ->
            val question = questionRepository.findById(tq.questionId)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found") }
            val choices = choiceRepository.findByQuestionId(tq.questionId)

            TestQuestionDto(
                questionId = question.id!!,
                text = question.text,
                difficulty = question.difficulty,
                order = tq.order,
                weight = tq.weight,
                choices = choices.map {
                    ChoiceDto(
                        id = it.id!!,
                        text = it.text,
                        isCorrect = it.isCorrect,  // Will be hidden by controller for students
                        order = it.order,
                        mediaId = it.mediaId
                    )
                }
            )
        }

        return TestDetailDto(
            id = test.id!!,
            subjectId = test.subjectId,
            name = test.name,
            grade = test.grade,
            durationSec = test.durationSec,
            maxScore = test.maxScore,
            isPublished = test.isPublished,
            shuffleQuestions = test.shuffleQuestions,
            shuffleChoices = test.shuffleChoices,
            allowedAttempts = test.allowedAttempts,
            questions = questionDtos
        )
    }

    @Transactional
    fun addQuestions(testId: UUID, dto: AddQuestionsToTestDto): TestDto {
        val test = testRepository.findById(testId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found") }

        if (test.isPublished) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot modify published test")
        }

        // Validate: all questions must belong to topics with the same subjectId as test
        dto.items.forEach { item ->
            val question = questionRepository.findById(item.questionId)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Question ${item.questionId} not found") }

            val topic = topicRepository.findById(question.topicId)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found") }

            if (topic.subjectId != test.subjectId) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Question ${item.questionId} belongs to a different subject"
                )
            }
        }

        // Create test-question associations
        val testQuestions = dto.items.map {
            TestQuestionEntity(
                testId = testId,
                questionId = it.questionId,
                order = it.order,
                weight = it.weight
            )
        }
        testQuestionRepository.saveAll(testQuestions)

        // Recalculate maxScore
        val maxScore = dto.items.sumOf { it.weight }
        test.maxScore = maxScore
        val saved = testRepository.save(test)

        return saved.toDto()
    }

    @Transactional
    fun reorderQuestions(testId: UUID, dto: ReorderQuestionsDto): TestDto {
        val test = testRepository.findById(testId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found") }

        if (test.isPublished) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot modify published test")
        }

        // Update order and weight for each question
        dto.items.forEach { item ->
            val testQuestion = testQuestionRepository.findById(
                TestQuestionId(testId, item.questionId)
            ).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Test question not found") }

            testQuestion.order = item.order
            testQuestion.weight = item.weight
            testQuestionRepository.save(testQuestion)
        }

        // Recalculate maxScore
        val maxScore = dto.items.sumOf { it.weight }
        test.maxScore = maxScore
        val saved = testRepository.save(test)

        return saved.toDto()
    }

    @Transactional
    fun publish(id: UUID): TestDto {
        val test = testRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found") }

        if (test.isPublished) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Test is already published")
        }

        // Validate test has questions
        val questionCount = testQuestionRepository.countByTestId(id)
        if (questionCount == 0L) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot publish test without questions")
        }

        test.isPublished = true
        val saved = testRepository.save(test)

        // Audit log
        auditService.log(
            eventType = AuditService.TEST_PUBLISHED,
            entityType = AuditService.ENTITY_TEST,
            entityId = saved.id!!,
            userId = getCurrentUserId(),
            metadata = mapOf(
                "testName" to saved.name,
                "questionCount" to questionCount,
                "maxScore" to (saved.maxScore ?: 0)
            )
        )

        return saved.toDto()
    }

    private fun TestEntity.toDto() = TestDto(
        id = this.id!!,
        subjectId = this.subjectId,
        name = this.name,
        grade = this.grade,
        durationSec = this.durationSec,
        maxScore = this.maxScore,
        isPublished = this.isPublished,
        shuffleQuestions = this.shuffleQuestions,
        shuffleChoices = this.shuffleChoices,
        allowedAttempts = this.allowedAttempts
    )

    private fun getCurrentUserId(): UUID? {
        return try {
            val auth = SecurityContextHolder.getContext().authentication
            auth?.name?.let { UUID.fromString(it) }
        } catch (e: Exception) {
            null
        }
    }
}
