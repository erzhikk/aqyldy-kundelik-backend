package kz.aqyldykundelik.assessment.service

import kz.aqyldykundelik.assessment.api.dto.CreateTopicDto
import kz.aqyldykundelik.assessment.api.dto.TopicDetailsDto
import kz.aqyldykundelik.assessment.api.dto.TopicDto
import kz.aqyldykundelik.assessment.api.dto.UpdateTopicDto
import kz.aqyldykundelik.assessment.domain.TopicEntity
import kz.aqyldykundelik.assessment.repo.QuestionRepository
import kz.aqyldykundelik.assessment.repo.TopicListProjection
import kz.aqyldykundelik.assessment.repo.TopicRepository
import kz.aqyldykundelik.security.SecurityUtils
import kz.aqyldykundelik.timetable.repo.SubjectRepository
import kz.aqyldykundelik.users.repo.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.ZoneOffset
import java.util.*

@Service
class TopicService(
    private val topicRepository: TopicRepository,
    private val questionRepository: QuestionRepository,
    private val userRepository: UserRepository,
    private val securityUtils: SecurityUtils,
    private val subjectRepository: SubjectRepository
) {

    fun create(dto: CreateTopicDto): TopicDto {
        val currentUserId = securityUtils.currentUserIdOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated")

        val entity = TopicEntity(
            subjectId = dto.subjectId,
            name = dto.name,
            description = dto.description
        )
        entity.createdByUserId = currentUserId
        val saved = topicRepository.save(entity)

        // Get current user's full name
        val createdByFullName = userRepository.findById(currentUserId)
            .map { it.fullName }
            .orElse(null)

        // Return TopicDto with questionsCount = 0 for new topic
        return TopicDto(
            id = saved.id!!,
            subjectId = saved.subjectId,
            name = saved.name,
            description = saved.description,
            createdByFullName = createdByFullName,
            createdAt = saved.createdAt,
            questionsCount = 0
        )
    }

    fun findAll(subjectId: UUID?, query: String?): List<TopicDto> {
        val projections = topicRepository.findTopicList(subjectId, query)
        return projections.map { it.toDto() }
    }

    fun findById(id: UUID): TopicDto {
        val topic = topicRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found") }
        val questionsCount = questionRepository.countByTopicId(id)
        val createdByFullName = topic.createdByUserId?.let { userId ->
            userRepository.findById(userId)
                .map { it.fullName }
                .orElse(null)
        }
        return TopicDto(
            id = topic.id!!,
            subjectId = topic.subjectId,
            name = topic.name,
            description = topic.description,
            createdByFullName = createdByFullName,
            createdAt = topic.createdAt,
            questionsCount = questionsCount
        )
    }

    fun findDetails(id: UUID): TopicDetailsDto {
        val topic = topicRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found") }
        val createdByFullName = topic.createdByUserId?.let { userId ->
            userRepository.findById(userId)
                .map { it.fullName }
                .orElse(null)
        }
        val subjectName = subjectRepository.findById(topic.subjectId)
            .map { it.nameRu }
            .orElse(null)

        return TopicDetailsDto(
            id = topic.id!!,
            subjectId = topic.subjectId,
            subjectName = subjectName,
            name = topic.name,
            description = topic.description,
            createdByFullName = createdByFullName,
            createdAt = topic.createdAt
        )
    }

    fun update(id: UUID, dto: UpdateTopicDto): TopicDto {
        val topic = topicRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found") }

        topic.name = dto.name
        topic.description = dto.description

        val currentUserId = securityUtils.currentUserIdOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated")

        val questionsCount = questionRepository.countByTopicId(id)
        val updatedUser = userRepository.findById(currentUserId).orElseThrow()

        topic.createdByUserId = updatedUser.id

        val saved = topicRepository.save(topic)

        return TopicDto(
            id = saved.id!!,
            subjectId = saved.subjectId,
            name = saved.name,
            description = saved.description,
            createdByFullName = updatedUser.fullName,
            createdAt = saved.createdAt,
            questionsCount = questionsCount
        )
    }

    fun delete(id: UUID) {
        val topic = topicRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found") }

        val questionsCount = questionRepository.countByTopicId(id)
        if (questionsCount > 0) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "TOPIC_HAS_QUESTIONS"
            )
        }

        topicRepository.delete(topic)
    }

    private fun TopicListProjection.toDto() = TopicDto(
        id = this.getId(),
        subjectId = this.getSubjectId(),
        name = this.getName(),
        description = this.getDescription(),
        createdByFullName = this.getCreatedByFullName(),
        createdAt = this.getCreatedAt()?.atOffset(ZoneOffset.UTC),
        questionsCount = this.getQuestionsCount()
    )
}
