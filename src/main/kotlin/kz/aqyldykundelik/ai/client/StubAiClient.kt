package kz.aqyldykundelik.ai.client

import com.fasterxml.jackson.databind.ObjectMapper
import kz.aqyldykundelik.ai.dto.AiPlanResponseDto
import kz.aqyldykundelik.ai.dto.AiTopicHelpResponseDto
import kz.aqyldykundelik.ai.dto.DayPlanDto
import kz.aqyldykundelik.ai.dto.ExampleDto
import kz.aqyldykundelik.ai.dto.PracticeDto
import kz.aqyldykundelik.ai.dto.SelfCheckDto
import kz.aqyldykundelik.ai.dto.TopicRefDto
import kz.aqyldykundelik.ai.dto.WeakTopicDto
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@ConditionalOnProperty(name = ["ai.provider"], havingValue = "stub", matchIfMissing = true)
class StubAiClient(
    private val objectMapper: ObjectMapper
) : AiClient {
    override fun generate(request: AiGenerateRequest): AiGenerateResult {
        val payload = when (request.type) {
            kz.aqyldykundelik.ai.domain.AiGeneratedType.PLAN -> AiPlanResponseDto(
                weakTopics = listOf(
                    WeakTopicDto(
                        topicId = UUID(0L, 1L),
                        topicName = "Логарифмы",
                        accuracy = 0.42,
                        mainMistake = "Ошибки в свойствах логарифмов"
                    )
                ),
                weeklyPlan = listOf(
                    DayPlanDto(
                        day = 1,
                        focus = "Свойства логарифмов",
                        actions = listOf("Повторить определения", "Разобрать 2 примера"),
                        timeMinutes = 15
                    )
                ),
                rules = listOf("log(a*b)=log(a)+log(b)", "log(a/b)=log(a)-log(b)"),
                selfCheck = listOf(SelfCheckDto(question = "Чему равен log(100)?", answer = "2"))
            )
            kz.aqyldykundelik.ai.domain.AiGeneratedType.TOPIC_HELP -> AiTopicHelpResponseDto(
                topic = TopicRefDto(
                    topicId = UUID(0L, 1L),
                    topicName = "Логарифмы"
                ),
                mainError = "Неправильное применение свойств логарифмов",
                explanation = "При умножении аргументов логарифмы складываются.",
                examples = listOf(
                    ExampleDto(question = "log(2*8)", solution = "log(2)+log(8)=1+3=4")
                ),
                practice = listOf(
                    PracticeDto(question = "log(10*100)", answer = "3")
                )
            )
        }

        return AiGenerateResult(
            content = objectMapper.writeValueAsString(payload),
            provider = "stub",
            model = request.model
        )
    }
}
