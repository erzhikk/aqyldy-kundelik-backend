package kz.aqyldykundelik.assessment.domain

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "test")
class TestEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "subject_id", nullable = false)
    var subjectId: UUID,

    @Column(nullable = false)
    var name: String,

    @Column
    var grade: Int? = null,

    @Column(name = "duration_sec")
    var durationSec: Int? = null,

    @Column(name = "max_score")
    var maxScore: Int? = null,

    @Column(name = "is_published", nullable = false)
    var isPublished: Boolean = false,

    @Column(name = "shuffle_questions", nullable = false)
    var shuffleQuestions: Boolean = true,

    @Column(name = "shuffle_choices", nullable = false)
    var shuffleChoices: Boolean = true,

    @Column(name = "allowed_attempts")
    var allowedAttempts: Int? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime? = null
) {
    @PrePersist
    fun prePersist() {
        val now = OffsetDateTime.now()
        if (createdAt == null) createdAt = now
        if (updatedAt == null) updatedAt = now
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = OffsetDateTime.now()
    }
}
