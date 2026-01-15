package kz.aqyldykundelik.ai.client

import org.springframework.stereotype.Component

@Component
class StubAiClient : AiClient {
    override fun generate(request: AiGenerateRequest): AiGenerateResult {
        val lowerUser = request.user.lowercase()
        val content = when {
            lowerUser.contains("type: plan") ->
                "Weak topics: topic_a, topic_b; Plan: Day1 - Day7; Rules: r1, r2; Checks: q1, q2, q3."
            lowerUser.contains("type: topic_help") ->
                "Mistakes: m1; Explanation: e1; Examples: ex1, ex2; Practice: q1, q2, q3, q4, q5."
            else ->
                "Plan: Day1 - Day7; Practice: q1, q2, q3."
        }

        return AiGenerateResult(
            content = content,
            provider = "stub",
            model = request.model
        )
    }
}
