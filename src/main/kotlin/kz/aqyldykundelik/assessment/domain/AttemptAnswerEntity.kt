package kz.aqyldykundelik.assessment.domain

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "attempt_answer")
class AttemptAnswerEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "attempt_id", nullable = false)
    var attemptId: UUID,

    @Column(name = "question_id", nullable = false)
    var questionId: UUID,

    @Column(name = "choice_id", nullable = false)
    var choiceId: UUID,

    @Column(name = "is_correct")
    var isCorrect: Boolean? = null,

    @Column(name = "score_delta", nullable = false)
    var scoreDelta: Int = 0
)
