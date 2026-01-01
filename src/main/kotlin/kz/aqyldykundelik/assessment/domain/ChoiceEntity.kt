package kz.aqyldykundelik.assessment.domain

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "choice")
class ChoiceEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "question_id", nullable = false)
    var questionId: UUID,

    @Column(nullable = false, columnDefinition = "TEXT")
    var text: String,

    @Column(name = "is_correct", nullable = false)
    var isCorrect: Boolean,

    @Column(name = "\"order\"", nullable = false)
    var order: Int = 0,

    @Column(name = "media_id")
    var mediaId: UUID? = null
)
