package kz.aqyldykundelik.assessment.api

import kz.aqyldykundelik.ai.service.AiCoachService
import kz.aqyldykundelik.assessment.api.dto.AiGeneratedDto
import kz.aqyldykundelik.assessment.api.dto.AiPlanRequestDto
import kz.aqyldykundelik.assessment.api.dto.AiTopicHelpRequestDto
import kz.aqyldykundelik.users.repo.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/api/student/ai")
class StudentAiCoachController(
    private val aiCoachService: AiCoachService,
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
    @PostMapping("/plan")
    fun generatePlan(
        @RequestBody request: AiPlanRequestDto,
        @RequestHeader("Accept-Language", required = false) acceptLanguage: String?,
        auth: Authentication
    ): AiGeneratedDto {
        val studentId = getStudentIdFromAuth(auth)
        val language = resolveLanguage(request.language, acceptLanguage)
        return aiCoachService.generatePlan(studentId, request.attemptId, language)
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/attempts/{attemptId}/topics/{topicId}/help")
    fun generateTopicHelp(
        @PathVariable attemptId: UUID,
        @PathVariable topicId: UUID,
        @RequestBody request: AiTopicHelpRequestDto,
        @RequestHeader("Accept-Language", required = false) acceptLanguage: String?,
        auth: Authentication
    ): AiGeneratedDto {
        val studentId = getStudentIdFromAuth(auth)
        val language = resolveLanguage(request.language, acceptLanguage)
        return aiCoachService.generateTopicHelp(
            studentId = studentId,
            attemptId = attemptId,
            topicId = topicId,
            language = language,
            mode = request.mode
        )
    }

    private fun resolveLanguage(requestLanguage: String?, acceptLanguage: String?): String? {
        val normalizedRequest = requestLanguage?.trim()?.lowercase()
        if (!normalizedRequest.isNullOrBlank()) {
            return normalizedRequest
        }
        val header = acceptLanguage?.split(",")?.firstOrNull()?.trim()?.lowercase()
        return header?.take(2)
    }
}
