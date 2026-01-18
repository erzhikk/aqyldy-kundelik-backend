package kz.aqyldykundelik.schedule.domain

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "class_schedule")
class ClassScheduleEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID) var id: UUID? = null,
    @Column(name = "class_id", nullable = false, unique = true) var classId: UUID? = null,
    @Column(nullable = false) var status: String = "DRAFT",
    @Column(name = "days_per_week", nullable = false) var daysPerWeek: Int = 5,
    @Column(name = "lessons_per_day", nullable = false) var lessonsPerDay: Int = 7,
    @Column(name = "created_at") var createdAt: OffsetDateTime? = null,
    @Column(name = "updated_at") var updatedAt: OffsetDateTime? = null
) {
    companion object {
        const val STATUS_DRAFT = "DRAFT"
        const val STATUS_ACTIVE = "ACTIVE"
    }

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
