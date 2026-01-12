package kz.aqyldykundelik.assessment.api

import kz.aqyldykundelik.assessment.api.dto.*
import kz.aqyldykundelik.assessment.service.AnalyticsService
import kz.aqyldykundelik.users.repo.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.*

@RestController
@RequestMapping("/api/student/analytics")
class StudentAnalyticsController(
    private val analyticsService: AnalyticsService,
    private val userRepository: UserRepository
) {

    private fun getStudentIdFromAuth(auth: Authentication): UUID {
        val email = auth.name
        val user = userRepository.findByEmail(email)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")
        return user.id
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User ID is missing")
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/last-attempt")
    fun getLastAttempt(auth: Authentication): LastAttemptDto {
        val studentId = getStudentIdFromAuth(auth)
        return analyticsService.getLastAttempt(studentId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No graded attempts found")
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/attempts/{attemptId}/summary")
    fun getAttemptSummary(
        @PathVariable attemptId: UUID,
        auth: Authentication
    ): StudentAttemptSummaryDto {
        val studentId = getStudentIdFromAuth(auth)
        return analyticsService.getAttemptSummary(attemptId, studentId)
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/attempts/summary")
    fun getAttemptSummaries(auth: Authentication): List<StudentAttemptSummaryDto> {
        val studentId = getStudentIdFromAuth(auth)
        return analyticsService.getAttemptSummaries(studentId)
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/attempts/{attemptId}/topics")
    fun getAttemptTopics(
        @PathVariable attemptId: UUID,
        auth: Authentication
    ): List<TopicScoreDto> {
        val studentId = getStudentIdFromAuth(auth)
        return analyticsService.getAttemptTopics(attemptId, studentId)
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/attempts/{attemptId}/topics/{topicId}")
    fun getAttemptTopicDetails(
        @PathVariable attemptId: UUID,
        @PathVariable topicId: UUID,
        auth: Authentication
    ): List<QuestionDetailDto> {
        val studentId = getStudentIdFromAuth(auth)
        return analyticsService.getAttemptTopicDetails(attemptId, topicId, studentId)
    }
}
