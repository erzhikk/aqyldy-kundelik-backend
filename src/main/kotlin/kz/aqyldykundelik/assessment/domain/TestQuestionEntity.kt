package kz.aqyldykundelik.assessment.domain

import jakarta.persistence.*
import java.io.Serializable
import java.util.*

@Entity
@Table(name = "test_question")
@IdClass(TestQuestionId::class)
class TestQuestionEntity(
    @Id
    @Column(name = "test_id", nullable = false)
    var testId: UUID,

    @Id
    @Column(name = "question_id", nullable = false)
    var questionId: UUID,

    @Column(name = "\"order\"", nullable = false)
    var order: Int = 0,

    @Column(nullable = false)
    var weight: Int = 1
)

data class TestQuestionId(
    var testId: UUID? = null,
    var questionId: UUID? = null
) : Serializable
