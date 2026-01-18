package kz.aqyldykundelik.classlevels.domain

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "class_level")
class ClassLevelEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID) var id: UUID? = null,
    @Column(nullable = false, unique = true) var level: Int? = null,
    @Column(name = "name_ru", nullable = false) var nameRu: String? = null,
    @Column(name = "name_kk", nullable = false) var nameKk: String? = null,
    @Column(name = "max_lessons_per_day", nullable = false) var maxLessonsPerDay: Int = 7,
    @Column(name = "days_per_week", nullable = false) var daysPerWeek: Int = 5,
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
