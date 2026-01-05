package kz.aqyldykundelik.assessment.api

import jakarta.validation.Valid
import kz.aqyldykundelik.assessment.api.dto.CreateQuestionDto
import kz.aqyldykundelik.assessment.api.dto.QuestionDto
import kz.aqyldykundelik.assessment.api.dto.UpdateQuestionDto
import kz.aqyldykundelik.assessment.domain.Difficulty
import kz.aqyldykundelik.assessment.service.QuestionService
import kz.aqyldykundelik.common.PageDto
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/assess")
class QuestionChoicesController(
    private val questionService: QuestionService
) {

    // ============= QUESTION ENDPOINTS =============

    @PreAuthorize("hasRole('ADMIN_ASSESSMENT') or hasRole('TEACHER') or hasRole('SUPER_ADMIN')")
    @GetMapping("/questions")
    fun getQuestions(
        @RequestParam(required = false) subjectId: UUID?,
        @RequestParam(required = false) topicId: UUID?,
        @RequestParam(required = false) difficulty: Difficulty?
    ): List<QuestionDto> {
        return questionService.findWithFilters(subjectId, topicId, difficulty)
    }

    @PreAuthorize("hasRole('ADMIN_ASSESSMENT') or hasRole('TEACHER') or hasRole('SUPER_ADMIN')")
    @GetMapping("/topics/{topicId}/questions")
    fun getTopicQuestions(
        @PathVariable topicId: UUID,
        @RequestParam(required = false) search: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) sort: String?
    ): PageDto<QuestionDto> {
        return questionService.findByTopicWithAnswers(topicId, search, page, size, sort)
    }

    @PreAuthorize("hasRole('ADMIN_ASSESSMENT') or hasRole('TEACHER') or hasRole('SUPER_ADMIN')")
    @PostMapping("/questions")
    fun createQuestion(@Valid @RequestBody dto: CreateQuestionDto): QuestionDto {
        return questionService.create(dto)
    }

    @PreAuthorize("hasRole('ADMIN_ASSESSMENT') or hasRole('TEACHER') or hasRole('SUPER_ADMIN')")
    @PutMapping("/questions/{id}")
    fun updateQuestion(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: UpdateQuestionDto
    ): QuestionDto {
        return questionService.update(id, dto)
    }

    @PreAuthorize("hasRole('ADMIN_ASSESSMENT') or hasRole('TEACHER') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/questions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteQuestion(@PathVariable id: UUID) {
        questionService.delete(id)
    }
}
