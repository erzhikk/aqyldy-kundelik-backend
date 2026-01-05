package kz.aqyldykundelik.assessment.domain

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "topic")
class TopicEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "subject_id", nullable = false)
    var subjectId: UUID,

    @Column(nullable = false)
    var name: String,

    @Column
    var description: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime? = null,

    @Column(name = "created_by_user_id")
    var createdByUserId: UUID? = null
) {
    @PrePersist
    fun prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now()
    }
}
