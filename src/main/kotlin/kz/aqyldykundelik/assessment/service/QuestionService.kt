package kz.aqyldykundelik.assessment.service

import jakarta.transaction.Transactional
import kz.aqyldykundelik.assessment.api.dto.*
import kz.aqyldykundelik.assessment.domain.ChoiceEntity
import kz.aqyldykundelik.assessment.domain.QuestionEntity
import kz.aqyldykundelik.assessment.repo.ChoiceRepository
import kz.aqyldykundelik.assessment.repo.QuestionRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Service
class QuestionService(
    private val questionRepository: QuestionRepository,
    private val choiceRepository: ChoiceRepository
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

    @Transactional
    fun delete(id: UUID) {
        if (!questionRepository.existsById(id)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found")
        }
        // Choices удалятся автоматически через cascade
        questionRepository.deleteById(id)
    }

    private fun validateChoices(choices: List<CreateChoiceDto>) {
        val correctCount = choices.count { it.isCorrect }
        if (correctCount != 1) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Exactly one choice must be marked as correct, but found $correctCount"
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
}
