package kz.aqyldykundelik.assessment.service

import jakarta.transaction.Transactional
import kz.aqyldykundelik.assessment.api.dto.*
import kz.aqyldykundelik.assessment.domain.TestEntity
import kz.aqyldykundelik.assessment.domain.TestQuestionEntity
import kz.aqyldykundelik.assessment.domain.TestQuestionId
import kz.aqyldykundelik.assessment.repo.*
import kz.aqyldykundelik.common.PageDto
import org.springframework.data.domain.PageRequest
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
            classLevelId = dto.classLevelId,
            name = dto.name,
            description = dto.description,
            durationSec = dto.durationSec,
            maxScore = 0,  // Will be calculated when questions are added
            shuffleQuestions = dto.shuffleQuestions,
            shuffleChoices = dto.shuffleChoices,
            allowedAttempts = dto.allowedAttempts,
            opensAt = dto.opensAt,
            closesAt = dto.closesAt,
            passingPercent = dto.passingPercent,
            reviewPolicy = dto.reviewPolicy
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
                "classLevelId" to (saved.classLevelId?.toString() ?: "null"),
                "status" to saved.status.name
            )
        )

        return saved.toDto()
    }

    @Transactional
    fun update(id: UUID, dto: UpdateTestDto): TestDto {
        val test = testRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found") }

        // For published tests, only allow updating certain fields
        if (test.status == kz.aqyldykundelik.assessment.domain.TestStatus.PUBLISHED) {
            // Check if user is trying to change restricted fields
            if (test.classLevelId != dto.classLevelId ||
                test.durationSec != dto.durationSec ||
                test.allowedAttempts != dto.allowedAttempts ||
                test.opensAt != dto.opensAt ||
                test.closesAt != dto.closesAt) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot modify classLevelId, durationSec, allowedAttempts, opensAt, closesAt for published test"
                )
            }
            // Only allow updating these fields for published tests
            test.name = dto.name
            test.description = dto.description
            test.shuffleQuestions = dto.shuffleQuestions
            test.shuffleChoices = dto.shuffleChoices
            test.passingPercent = dto.passingPercent
            test.reviewPolicy = dto.reviewPolicy
        } else {
            // For draft tests, allow updating all fields
            test.name = dto.name
            test.classLevelId = dto.classLevelId
            test.description = dto.description
            test.durationSec = dto.durationSec
            test.shuffleQuestions = dto.shuffleQuestions
            test.shuffleChoices = dto.shuffleChoices
            test.allowedAttempts = dto.allowedAttempts
            test.opensAt = dto.opensAt
            test.closesAt = dto.closesAt
            test.passingPercent = dto.passingPercent
            test.reviewPolicy = dto.reviewPolicy
        }

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

    fun findAll(
        subjectId: UUID?,
        classLevelId: UUID?,
        status: kz.aqyldykundelik.assessment.domain.TestStatus?,
        page: Int,
        size: Int
    ): PageDto<TestDto> {
        val tests = when {
            subjectId != null && status != null ->
                testRepository.findBySubjectIdAndStatus(subjectId, status)
            subjectId != null ->
                testRepository.findBySubjectId(subjectId)
            classLevelId != null ->
                testRepository.findByClassLevelId(classLevelId)
            status != null ->
                testRepository.findByStatus(status)
            else ->
                testRepository.findAll()
        }

        val testDtos = tests.map { it.toDto() }
        val totalElements = testDtos.size.toLong()
        val totalPages = if (size > 0) ((totalElements + size - 1) / size).toInt() else 1

        // Apply pagination in memory
        val startIndex = page * size
        val endIndex = minOf(startIndex + size, testDtos.size)
        val content = if (startIndex < testDtos.size) {
            testDtos.subList(startIndex, endIndex)
        } else {
            emptyList()
        }

        return PageDto(
            content = content,
            page = page,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages
        )
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
                choices = choices.sortedBy { it.order }.map {
                    AttemptChoiceDto(
                        id = it.id!!,
                        text = it.text,
                        order = it.order,
                        mediaId = it.mediaId
                    )
                }
            )
        }

        return TestDetailDto(
            id = test.id!!,
            subjectId = test.subjectId,
            classLevelId = test.classLevelId,
            name = test.name,
            description = test.description,
            durationSec = test.durationSec,
            maxScore = test.maxScore,
            status = test.status,
            shuffleQuestions = test.shuffleQuestions,
            shuffleChoices = test.shuffleChoices,
            allowedAttempts = test.allowedAttempts,
            opensAt = test.opensAt,
            closesAt = test.closesAt,
            passingPercent = test.passingPercent,
            reviewPolicy = test.reviewPolicy,
            questions = questionDtos
        )
    }

    @Transactional
    fun addQuestions(testId: UUID, dto: AddQuestionsToTestDto): TestDto {
        val test = testRepository.findById(testId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found") }

        if (test.status == kz.aqyldykundelik.assessment.domain.TestStatus.PUBLISHED) {
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

        if (test.status == kz.aqyldykundelik.assessment.domain.TestStatus.PUBLISHED) {
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

        if (test.status == kz.aqyldykundelik.assessment.domain.TestStatus.PUBLISHED) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Test is already published")
        }

        // Validate: test has questions and maxScore > 0
        val questionCount = testQuestionRepository.countByTestId(id)
        if (questionCount == 0L) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot publish test without questions")
        }
        if (test.maxScore <= 0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot publish test with maxScore <= 0")
        }

        // Validate: time window (opensAt < closesAt if both are set)
        if (test.opensAt != null && test.closesAt != null && test.opensAt!! >= test.closesAt!!) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "opensAt must be before closesAt"
            )
        }

        // Validate: allowedAttempts >= 1 (if set)
        if (test.allowedAttempts != null && test.allowedAttempts!! < 1) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "allowedAttempts must be at least 1"
            )
        }

        // Validate: all questions are valid
        val testQuestions = testQuestionRepository.findByTestIdOrderByOrderAsc(id)
        testQuestions.forEach { tq ->
            val question = questionRepository.findById(tq.questionId)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Question ${tq.questionId} not found") }

            // Check question belongs to correct subject
            val topic = topicRepository.findById(question.topicId)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found") }
            if (topic.subjectId != test.subjectId) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Question ${tq.questionId} belongs to a different subject"
                )
            }

            // Check exactly one correct choice
            val choices = choiceRepository.findByQuestionId(tq.questionId)
            val correctCount = choices.count { it.isCorrect }
            if (correctCount != 1) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Question ${tq.questionId} must have exactly one correct choice, but has $correctCount"
                )
            }
        }

        test.status = kz.aqyldykundelik.assessment.domain.TestStatus.PUBLISHED
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
                "maxScore" to saved.maxScore
            )
        )

        return saved.toDto()
    }

    private fun TestEntity.toDto() = TestDto(
        id = this.id!!,
        subjectId = this.subjectId,
        classLevelId = this.classLevelId,
        name = this.name,
        description = this.description,
        durationSec = this.durationSec,
        maxScore = this.maxScore,
        status = this.status,
        shuffleQuestions = this.shuffleQuestions,
        shuffleChoices = this.shuffleChoices,
        allowedAttempts = this.allowedAttempts,
        opensAt = this.opensAt,
        closesAt = this.closesAt,
        passingPercent = this.passingPercent,
        reviewPolicy = this.reviewPolicy
    )

    @Transactional
    fun clone(id: UUID): TestDto {
        val sourceTest = testRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found") }

        // Create new test with DRAFT status
        val clonedTest = TestEntity(
            subjectId = sourceTest.subjectId,
            classLevelId = sourceTest.classLevelId,
            name = "${sourceTest.name} (Copy)",
            description = sourceTest.description,
            durationSec = sourceTest.durationSec,
            maxScore = sourceTest.maxScore,
            shuffleQuestions = sourceTest.shuffleQuestions,
            shuffleChoices = sourceTest.shuffleChoices,
            allowedAttempts = sourceTest.allowedAttempts,
            opensAt = sourceTest.opensAt,
            closesAt = sourceTest.closesAt,
            passingPercent = sourceTest.passingPercent,
            reviewPolicy = sourceTest.reviewPolicy
        )
        val savedTest = testRepository.save(clonedTest)

        // Copy all test questions
        val sourceQuestions = testQuestionRepository.findByTestIdOrderByOrderAsc(id)
        val clonedQuestions = sourceQuestions.map { sq ->
            TestQuestionEntity(
                testId = savedTest.id!!,
                questionId = sq.questionId,
                order = sq.order,
                weight = sq.weight
            )
        }
        testQuestionRepository.saveAll(clonedQuestions)

        // Audit log
        auditService.log(
            eventType = "TEST_CLONED",
            entityType = AuditService.ENTITY_TEST,
            entityId = savedTest.id!!,
            userId = getCurrentUserId(),
            metadata = mapOf(
                "sourceTestId" to id.toString(),
                "sourceTestName" to sourceTest.name,
                "newTestName" to savedTest.name,
                "questionCount" to clonedQuestions.size
            )
        )

        return savedTest.toDto()
    }

    private fun getCurrentUserId(): UUID? {
        return try {
            val auth = SecurityContextHolder.getContext().authentication
            auth?.name?.let { UUID.fromString(it) }
        } catch (e: Exception) {
            null
        }
    }
}
