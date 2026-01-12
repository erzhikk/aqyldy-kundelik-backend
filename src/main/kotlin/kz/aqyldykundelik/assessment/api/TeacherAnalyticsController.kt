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
@RequestMapping("/api/teacher/analytics")
class TeacherAnalyticsController(
    private val analyticsService: AnalyticsService,
    private val userRepository: UserRepository
) {

    private fun getUserIdFromAuth(auth: Authentication): UUID {
        val email = auth.name
        val user = userRepository.findByEmail(email)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")
        return user.id
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User ID is missing")
    }

    private fun isAdmin(auth: Authentication): Boolean {
        return auth.authorities.any {
            it.authority == "ROLE_ADMIN_ASSESSMENT" ||
                    it.authority == "ROLE_SUPER_ADMIN"
        }
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN_ASSESSMENT') or hasRole('SUPER_ADMIN')")
    @GetMapping("/classes")
    fun getClassesList(auth: Authentication): List<ClassInfoDto> {
        val teacherId = if (isAdmin(auth)) {
            null  // Admin sees all classes
        } else {
            getUserIdFromAuth(auth)  // Teacher sees only their classes
        }
        return analyticsService.getClassesList(teacherId)
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN_ASSESSMENT') or hasRole('SUPER_ADMIN')")
    @GetMapping("/classes/{classId}/last-test/summary")
    fun getClassLastTestSummary(
        @PathVariable classId: UUID,
        auth: Authentication
    ): ClassTestSummaryDto {
        // TODO: Add authorization check - teacher can only access their classes
        return analyticsService.getClassLastTestSummary(classId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No graded attempts found for this class")
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN_ASSESSMENT') or hasRole('SUPER_ADMIN')")
    @GetMapping("/classes/{classId}/tests/{testId}/topics")
    fun getClassTestTopics(
        @PathVariable classId: UUID,
        @PathVariable testId: UUID,
        auth: Authentication
    ): List<ClassTopicAnalyticsDto> {
        // TODO: Add authorization check - teacher can only access their classes
        return analyticsService.getClassTestTopics(classId, testId)
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN_ASSESSMENT') or hasRole('SUPER_ADMIN')")
    @GetMapping("/classes/{classId}/tests/{testId}/topics/{topicId}")
    fun getClassTestTopicDetails(
        @PathVariable classId: UUID,
        @PathVariable testId: UUID,
        @PathVariable topicId: UUID,
        auth: Authentication
    ): TopicDrillDownDto {
        // TODO: Add authorization check - teacher can only access their classes
        return analyticsService.getClassTestTopicDetails(classId, testId, topicId)
    }
}
