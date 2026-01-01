package kz.aqyldykundelik.assessment.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "test_attempt")
class TestAttemptEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "test_id", nullable = false)
    var testId: UUID,

    @Column(name = "student_id", nullable = false)
    var studentId: UUID,

    @Column(name = "started_at", nullable = false)
    var startedAt: OffsetDateTime? = null,

    @Column(name = "finished_at")
    var finishedAt: OffsetDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: AttemptStatus = AttemptStatus.IN_PROGRESS,

    @Column(nullable = false)
    var score: Int = 0,

    @Column(nullable = false, precision = 5, scale = 2)
    var percent: BigDecimal = BigDecimal.ZERO
) {
    @PrePersist
    fun prePersist() {
        if (startedAt == null) startedAt = OffsetDateTime.now()
    }
}
