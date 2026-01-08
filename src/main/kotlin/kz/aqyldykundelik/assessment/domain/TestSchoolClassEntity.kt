package kz.aqyldykundelik.assessment.domain

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "test_school_class")
class TestSchoolClassEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "test_id", nullable = false)
    var testId: UUID,

    @Column(name = "school_class_id", nullable = false)
    var schoolClassId: UUID,

    @Column(name = "assigned_at", nullable = false)
    var assignedAt: OffsetDateTime? = null
) {
    @PrePersist
    fun prePersist() {
        if (assignedAt == null) assignedAt = OffsetDateTime.now()
    }
}
