package kz.aqyldykundelik.assessment.api

import jakarta.validation.Valid
import kz.aqyldykundelik.assessment.api.dto.*
import kz.aqyldykundelik.assessment.service.AnalyticsService
import kz.aqyldykundelik.assessment.service.AttemptService
import kz.aqyldykundelik.assessment.service.AuditService
import kz.aqyldykundelik.assessment.service.TestService
import kz.aqyldykundelik.common.PageDto
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.util.*

@RestController
@RequestMapping("/api/assess")
class AssessmentController(
    private val testService: TestService,
    private val attemptService: AttemptService,
    private val analyticsService: AnalyticsService,
    private val auditService: AuditService
) {
    private val logger = LoggerFactory.getLogger(AssessmentController::class.java)

    private fun getStudentIdFromAuth(auth: Authentication): UUID {
        val userId = auth.name  // JWT subject = userId
        return try {
            UUID.fromString(userId)
        } catch (e: IllegalArgumentException) {
            logger.error("Failed to parse userId from JWT. auth.name = '$userId'", e)
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user ID format in token")
        }
    }

    // ============= TEST ENDPOINTS =============

    @PreAuthorize("hasRole('ADMIN_ASSESSMENT') or hasRole('TEACHER') or hasRole('SUPER_ADMIN')")
    @PostMapping("/tests")
    fun createTest(@Valid @RequestBody dto: CreateTestDto): TestDto {
        return testService.create(dto)
    }

    @PreAuthorize("hasRole('ADMIN_ASSESSMENT') or hasRole('TEACHER') or hasRole('SUPER_ADMIN')")
    @PutMapping("/tests/{id}")
    fun updateTest(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: UpdateTestDto
    ): TestDto {
        return testService.update(id, dto)
    }

    @PreAuthorize("hasRole('ADMIN_ASSESSMENT') or hasRole('TEACHER') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/tests/{id}")
    fun deleteTest(@PathVariable id: UUID) {
        testService.delete(id)
    }

    @PreAuthorize("hasRole('ADMIN_ASSESSMENT') or hasRole('TEACHER') or hasRole('SUPER_ADMIN')")
    @GetMapping("/tests")
    fun getTests(
        @RequestParam(required = false) subjectId: UUID?,
        @RequestParam(required = false) classLevelId: UUID?,
        @RequestParam(required = false) status: kz.aqyldykundelik.assessment.domain.TestStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PageDto<TestDto> {
        return testService.findAll(subjectId, classLevelId, status, page, size)
    }

    @PreAuthorize("hasRole('ADMIN_ASSESSMENT') or hasRole('TEACHER') or hasRole('SUPER_ADMIN')")
    @GetMapping("/tests/{id}")
    fun getTestDetail(@PathVariable id: UUID): TestDetailDto {
        return testService.findById(id)
    }

    @PreAuthorize("hasRole('ADMIN_ASSESSMENT') or hasRole('TEACHER') or hasRole('SUPER_ADMIN')")
    @PostMapping("/tests/{id}/questions")
    fun addQuestionsToTest(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: AddQuestionsToTestDto
    ): TestDto {
        return testService.addQuestions(id, dto)
    }

    @PreAuthorize("hasRole('ADMIN_ASSESSMENT') or hasRole('TEACHER') or hasRole('SUPER_ADMIN')")
    @PutMapping("/tests/{id}/questions/reorder")
    fun reorderQuestions(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: ReorderQuestionsDto
    ): TestDto {
        return testService.reorderQuestions(id, dto)
    }

    @PreAuthorize("hasRole('ADMIN_ASSESSMENT') or hasRole('TEACHER') or hasRole('SUPER_ADMIN')")
    @PostMapping("/tests/{id}/publish")
    fun publishTest(@PathVariable id: UUID): TestDto {
        return testService.publish(id)
    }

    @PreAuthorize("hasRole('ADMIN_ASSESSMENT') or hasRole('TEACHER') or hasRole('SUPER_ADMIN')")
    @PostMapping("/tests/{id}/clone")
    fun cloneTest(@PathVariable id: UUID): TestDto {
        return testService.clone(id)
    }

    // ============= STUDENT ATTEMPT ENDPOINTS =============

    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    @PostMapping("/tests/{id}/attempts")
    fun startAttempt(
        @PathVariable id: UUID,
        auth: Authentication
    ): StartAttemptResponseDto {
        val studentId = getStudentIdFromAuth(auth)
        return attemptService.startAttempt(id, studentId)
    }

    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    @PostMapping("/attempts/{attemptId}/answers")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun saveAnswers(
        @PathVariable attemptId: UUID,
        @Valid @RequestBody dto: SaveAnswersDto,
        auth: Authentication
    ) {
        val studentId = getStudentIdFromAuth(auth)
        attemptService.saveAnswers(attemptId, studentId, dto)
    }

    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    @PostMapping("/attempts/{attemptId}/submit")
    fun submitAttempt(
        @PathVariable attemptId: UUID,
        auth: Authentication
    ): SubmitAttemptResponseDto {
        val studentId = getStudentIdFromAuth(auth)
        return attemptService.submitAttempt(attemptId, studentId)
    }

    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    @GetMapping("/attempts/{attemptId}/result")
    fun getAttemptResult(
        @PathVariable attemptId: UUID,
        auth: Authentication
    ): AttemptResultDto {
        val studentId = getStudentIdFromAuth(auth)
        return attemptService.getAttemptResult(attemptId, studentId)
    }

    // ============= ANALYTICS ENDPOINTS =============

    @PreAuthorize("hasRole('ADMIN_ASSESSMENT') or hasRole('TEACHER') or hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    @GetMapping("/analytics/student/{studentId}/topics")
    fun getStudentTopicAnalytics(
        @PathVariable studentId: UUID,
        @RequestParam(required = false) subjectId: UUID?,
        @RequestParam(required = false) from: OffsetDateTime?,
        @RequestParam(required = false) to: OffsetDateTime?,
        auth: Authentication
    ): List<StudentTopicAnalyticsDto> {
        logger.debug("getStudentTopicAnalytics called. auth.name='${auth.name}', authorities=${auth.authorities.map { it.authority }}")

        // Students can only view their own analytics
        if (auth.authorities.any { it.authority == "ROLE_STUDENT" }) {
            val currentUserId = getStudentIdFromAuth(auth)
            if (currentUserId != studentId) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "Students can only view their own analytics")
            }
        }
        // Teachers, admins can view any student's analytics without validation

        return analyticsService.getStudentTopicAnalytics(studentId, subjectId, from, to)
    }

    @PreAuthorize("hasRole('ADMIN_ASSESSMENT') or hasRole('TEACHER') or hasRole('SUPER_ADMIN')")
    @GetMapping("/analytics/class/{classId}/tests/{testId}")
    fun getClassTestAnalytics(
        @PathVariable classId: UUID,
        @PathVariable testId: UUID
    ): ClassTestAnalyticsDto {
        return analyticsService.getClassTestAnalytics(classId, testId)
    }

    // ============= TEST ENDPOINT FOR LOGGING =============

    @GetMapping("/test-logging")
    fun testLogging(): Map<String, String> {
        logger.trace("TRACE level log message")
        logger.debug("DEBUG level log message")
        logger.info("INFO level log message")
        logger.warn("WARN level log message")
        logger.error("ERROR level log message")

        // Test audit logging
        auditService.log(
            eventType = "TEST_LOG",
            entityType = "TEST",
            entityId = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            metadata = mapOf("test" to "logging", "timestamp" to System.currentTimeMillis())
        )

        return mapOf(
            "message" to "Logging test complete. Check console and logs/audit.log",
            "trace" to "TRACE level log message",
            "debug" to "DEBUG level log message",
            "info" to "INFO level log message",
            "warn" to "WARN level log message",
            "error" to "ERROR level log message"
        )
    }
}
