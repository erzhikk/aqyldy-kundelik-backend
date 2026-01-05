package kz.aqyldykundelik.assessment.service

import jakarta.transaction.Transactional
import kz.aqyldykundelik.assessment.api.dto.*
import kz.aqyldykundelik.assessment.domain.ChoiceEntity
import kz.aqyldykundelik.assessment.domain.QuestionEntity
import kz.aqyldykundelik.assessment.repo.ChoiceRepository
import kz.aqyldykundelik.assessment.repo.QuestionRepository
import kz.aqyldykundelik.common.PageDto
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Service
class QuestionService(
    private val questionRepository: QuestionRepository,
    private val choiceRepository: ChoiceRepository,
    private val messageSource: MessageSource
) {

    @Transactional
    fun create(dto: CreateQuestionDto): QuestionDto {
        // Валидация: ровно один isCorrect=true
        validateChoices(dto.choices)

        val entity = QuestionEntity(
            topicId = dto.topicId,
            text = dto.text,
            mediaId = dto.mediaId,
            difficulty = dto.difficulty,
            explanation = dto.explanation
        )
        val savedQuestion = questionRepository.save(entity)

        // Создаём choices
        val choices = dto.choices.mapIndexed { index, choiceDto ->
            ChoiceEntity(
                questionId = savedQuestion.id!!,
                text = choiceDto.text,
                isCorrect = choiceDto.isCorrect,
                order = choiceDto.order.takeIf { it > 0 } ?: index,
                mediaId = choiceDto.mediaId
            )
        }
        val savedChoices = choiceRepository.saveAll(choices)

        return savedQuestion.toDto(savedChoices)
    }

    @Transactional
    fun update(id: UUID, dto: UpdateQuestionDto): QuestionDto {
        // Валидация: ровно один isCorrect=true
        validateChoices(dto.choices)

        val question = questionRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found") }

        question.text = dto.text
        question.mediaId = dto.mediaId
        question.difficulty = dto.difficulty
        question.explanation = dto.explanation

        val savedQuestion = questionRepository.save(question)

        // Удаляем старые choices и создаём новые
        choiceRepository.deleteByQuestionId(id)

        val choices = dto.choices.mapIndexed { index, choiceDto ->
            ChoiceEntity(
                questionId = id,
                text = choiceDto.text,
                isCorrect = choiceDto.isCorrect,
                order = choiceDto.order.takeIf { it > 0 } ?: index,
                mediaId = choiceDto.mediaId
            )
        }
        val savedChoices = choiceRepository.saveAll(choices)

        return savedQuestion.toDto(savedChoices)
    }

    fun findById(id: UUID): QuestionDto {
        val question = questionRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found") }
        val choices = choiceRepository.findByQuestionId(id)
        return question.toDto(choices)
    }

    fun findWithFilters(subjectId: UUID?, topicId: UUID?, difficulty: kz.aqyldykundelik.assessment.domain.Difficulty?): List<QuestionDto> {
        val questions = questionRepository.findWithFilters(subjectId, topicId, difficulty)
        return questions.map { question ->
            val choices = choiceRepository.findByQuestionId(question.id!!)
            question.toDto(choices)
        }
    }

    fun findByTopicWithAnswers(
        topicId: UUID,
        search: String?,
        page: Int,
        size: Int,
        sort: String?
    ): PageDto<QuestionDto> {
        val pageable = PageRequest.of(page, size, parseSort(sort))
        val questionPage = if (search.isNullOrBlank()) {
            questionRepository.findByTopicId(topicId, pageable)
        } else {
            questionRepository.findByTopicIdAndTextContainingIgnoreCase(topicId, search, pageable)
        }

        val questionIds = questionPage.content.mapNotNull { it.id }
        val choicesByQuestionId = if (questionIds.isEmpty()) {
            emptyMap()
        } else {
            choiceRepository.findByQuestionIdIn(questionIds).groupBy { it.questionId }
        }

        val content = questionPage.content.map { question ->
            val choices = choicesByQuestionId[question.id].orEmpty().sortedBy { it.order }
            question.toDto(choices)
        }

        return PageDto(
            content = content,
            page = page,
            size = size,
            totalElements = questionPage.totalElements,
            totalPages = questionPage.totalPages
        )
    }

    @Transactional
    fun delete(id: UUID) {
        if (!questionRepository.existsById(id)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found")
        }
        // Choices удалятся автоматически через cascade
        questionRepository.deleteById(id)
    }

    private fun validateChoices(choices: List<CreateChoiceDto>) {
        val locale = LocaleContextHolder.getLocale()
        if (choices.size !in 4..5) {
            val message = messageSource.getMessage(
                "assessment.question.choices.count",
                arrayOf(choices.size),
                locale
            )
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
            )
        }
        val correctCount = choices.count { it.isCorrect }
        if (correctCount != 1) {
            val message = messageSource.getMessage(
                "assessment.question.choices.correct",
                arrayOf(correctCount),
                locale
            )
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
            )
        }
    }

    private fun QuestionEntity.toDto(choices: List<ChoiceEntity>) = QuestionDto(
        id = this.id!!,
        topicId = this.topicId,
        text = this.text,
        mediaId = this.mediaId,
        difficulty = this.difficulty,
        explanation = this.explanation,
        choices = choices.map { it.toDto() }
    )

    private fun ChoiceEntity.toDto() = ChoiceDto(
        id = this.id!!,
        text = this.text,
        isCorrect = this.isCorrect,
        order = this.order,
        mediaId = this.mediaId
    )

    private fun parseSort(sort: String?): Sort {
        if (sort.isNullOrBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt")
        }

        val parts = sort.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val property = parts.firstOrNull() ?: "createdAt"
        val direction = parts.getOrNull(1)?.lowercase() ?: "asc"

        val allowed = setOf("createdAt", "text", "difficulty")
        val safeProperty = if (allowed.contains(property)) property else "createdAt"
        val sortDirection = if (direction == "desc") Sort.Direction.DESC else Sort.Direction.ASC

        return Sort.by(sortDirection, safeProperty)
    }
}
