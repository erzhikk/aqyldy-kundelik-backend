package kz.aqyldykundelik.assessment.service

import kz.aqyldykundelik.assessment.api.dto.CreateTopicDto
import kz.aqyldykundelik.assessment.api.dto.TopicDto
import kz.aqyldykundelik.assessment.domain.TopicEntity
import kz.aqyldykundelik.assessment.repo.TopicRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Service
class TopicService(
    private val topicRepository: TopicRepository
) {

    fun create(dto: CreateTopicDto): TopicDto {
        val entity = TopicEntity(
            subjectId = dto.subjectId,
            name = dto.name,
            description = dto.description
        )
        val saved = topicRepository.save(entity)
        return saved.toDto()
    }

    fun findAll(subjectId: UUID?, query: String?): List<TopicDto> {
        val topics = when {
            subjectId != null && query != null ->
                topicRepository.findBySubjectIdAndNameContainingIgnoreCase(subjectId, query)
            subjectId != null ->
                topicRepository.findBySubjectId(subjectId)
            else ->
                topicRepository.findAll()
        }
        return topics.map { it.toDto() }
    }

    fun findById(id: UUID): TopicDto {
        val topic = topicRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found") }
        return topic.toDto()
    }

    private fun TopicEntity.toDto() = TopicDto(
        id = this.id!!,
        subjectId = this.subjectId,
        name = this.name,
        description = this.description
    )
}
