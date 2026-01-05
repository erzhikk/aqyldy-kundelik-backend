package kz.aqyldykundelik.assessment.api

import jakarta.validation.Valid
import kz.aqyldykundelik.assessment.api.dto.CreateTopicDto
import kz.aqyldykundelik.assessment.api.dto.TopicDetailsDto
import kz.aqyldykundelik.assessment.api.dto.TopicDto
import kz.aqyldykundelik.assessment.api.dto.UpdateTopicDto
import kz.aqyldykundelik.assessment.service.TopicService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/assess")
class TopicController(
    private val topicService: TopicService
) {

    // ============= TOPIC ENDPOINTS =============

    @PreAuthorize("hasRole('ADMIN_ASSESSMENT') or hasRole('TEACHER') or hasRole('SUPER_ADMIN')")
    @PostMapping("/topics")
    fun createTopic(@Valid @RequestBody dto: CreateTopicDto): TopicDto {
        return topicService.create(dto)
    }

    @PreAuthorize("hasRole('ADMIN_ASSESSMENT') or hasRole('TEACHER') or hasRole('SUPER_ADMIN')")
    @GetMapping("/topics")
    fun getTopics(
        @RequestParam(required = false) subjectId: UUID?,
        @RequestParam(required = false) q: String?
    ): List<TopicDto> {
        return topicService.findAll(subjectId, q)
    }

    @PreAuthorize("hasRole('ADMIN_ASSESSMENT') or hasRole('TEACHER') or hasRole('SUPER_ADMIN')")
    @GetMapping("/topics/{id}")
    fun getTopicDetails(@PathVariable id: UUID): TopicDetailsDto {
        return topicService.findDetails(id)
    }

    @PreAuthorize("hasRole('ADMIN_ASSESSMENT') or hasRole('TEACHER') or hasRole('SUPER_ADMIN')")
    @PutMapping("/topics/{id}")
    fun updateTopic(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: UpdateTopicDto
    ): TopicDto {
        return topicService.update(id, dto)
    }

    @PreAuthorize("hasRole('ADMIN_ASSESSMENT') or hasRole('TEACHER') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/topics/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTopic(@PathVariable id: UUID) {
        topicService.delete(id)
    }
}
