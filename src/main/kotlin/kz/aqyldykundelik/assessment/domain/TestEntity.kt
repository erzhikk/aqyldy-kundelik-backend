package kz.aqyldykundelik.assessment.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "test")
class TestEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "subject_id", nullable = false)
    var subjectId: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    var name: String = "",

    @Column
    var description: String? = null,

    @Column(name = "duration_sec")
    var durationSec: Int? = null,

    @Column(name = "max_score", nullable = false)
    var maxScore: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: TestStatus = TestStatus.DRAFT,

    @Column(name = "shuffle_questions", nullable = false)
    var shuffleQuestions: Boolean = true,

    @Column(name = "shuffle_choices", nullable = false)
    var shuffleChoices: Boolean = true,

    @Column(name = "allowed_attempts")
    var allowedAttempts: Int? = null,

    @Column(name = "opens_at")
    var opensAt: OffsetDateTime? = null,

    @Column(name = "closes_at")
    var closesAt: OffsetDateTime? = null,

    @Column(name = "passing_percent", precision = 5, scale = 2)
    var passingPercent: BigDecimal? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "review_policy")
    var reviewPolicy: ReviewPolicy? = null,

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
