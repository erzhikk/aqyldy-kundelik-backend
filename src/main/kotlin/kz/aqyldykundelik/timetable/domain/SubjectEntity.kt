package kz.aqyldykundelik.timetable.domain
import jakarta.persistence.*
import kz.aqyldykundelik.classlevels.domain.ClassLevelEntity
import java.time.OffsetDateTime
import java.util.*

@Entity @Table(name = "subject")
class SubjectEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID) var id: UUID? = null,
    @Column(name = "name_ru", nullable = false) var nameRu: String? = null,
    @Column(name = "name_kk", nullable = false) var nameKk: String? = null,
    @Column(name = "name_en", nullable = false) var nameEn: String? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_level_id", nullable = false)
    var classLevel: ClassLevelEntity? = null,
    @Column(name = "created_at") var createdAt: OffsetDateTime? = null,
    @Column(name = "updated_at") var updatedAt: OffsetDateTime? = null,
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
