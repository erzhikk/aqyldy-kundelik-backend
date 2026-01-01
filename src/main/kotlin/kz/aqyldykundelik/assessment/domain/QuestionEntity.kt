package kz.aqyldykundelik.assessment.domain

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "question")
class QuestionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "topic_id", nullable = false)
    var topicId: UUID,

    @Column(nullable = false, columnDefinition = "TEXT")
    var text: String,

    @Column(name = "media_id")
    var mediaId: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var difficulty: Difficulty = Difficulty.MEDIUM,

    @Column(columnDefinition = "TEXT")
    var explanation: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime? = null
) {
    @PrePersist
    fun prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now()
    }
}
