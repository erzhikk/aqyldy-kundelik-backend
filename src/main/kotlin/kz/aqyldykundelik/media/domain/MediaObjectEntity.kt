package kz.aqyldykundelik.media.domain

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "media_object")
class MediaObjectEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID) var id: UUID? = null,
    @Column(name = "user_id", nullable = false) var userId: UUID? = null,
    @Column(name = "s3_key", nullable = false, unique = true) var s3Key: String? = null,
    @Column(name = "content_type", nullable = false) var contentType: String? = null,
    @Column(name = "file_size") var fileSize: Long? = null,
    @Column var width: Int? = null,
    @Column var height: Int? = null,
    @Column var sha256: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false) var status: MediaObjectStatus = MediaObjectStatus.UPLOADING,
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

enum class MediaObjectStatus {
    UPLOADING,  // Файл в процессе загрузки
    READY,      // Файл успешно загружен и провалидирован
    FAILED,     // Валидация не прошла
    DELETED     // Мягкое удаление
}
