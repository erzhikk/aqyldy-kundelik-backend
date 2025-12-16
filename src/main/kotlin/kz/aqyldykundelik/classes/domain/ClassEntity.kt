package kz.aqyldykundelik.classes.domain

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "school_class")
class ClassEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID) var id: UUID? = null,
    @Column(nullable = false, length = 3) var code: String? = null,
    @Column(name = "class_teacher_id") var classTeacherId: UUID? = null,
    @Column(name = "lang_type", nullable = false, length = 3) var langType: String? = null,
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
