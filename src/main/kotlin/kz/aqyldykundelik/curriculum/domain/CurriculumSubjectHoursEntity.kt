package kz.aqyldykundelik.curriculum.domain

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "curriculum_subject_hours")
class CurriculumSubjectHoursEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID) var id: UUID? = null,
    @Column(name = "class_level_id", nullable = false) var classLevelId: UUID? = null,
    @Column(name = "subject_id", nullable = false) var subjectId: UUID? = null,
    @Column(name = "hours_per_week", nullable = false) var hoursPerWeek: Int = 0,
    @Column(name = "created_at") var createdAt: OffsetDateTime? = null,
    @Column(name = "updated_at") var updatedAt: OffsetDateTime? = null
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
