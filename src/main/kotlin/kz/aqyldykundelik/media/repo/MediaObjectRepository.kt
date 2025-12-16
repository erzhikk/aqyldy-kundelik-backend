package kz.aqyldykundelik.media.repo

import kz.aqyldykundelik.media.domain.MediaObjectEntity
import kz.aqyldykundelik.media.domain.MediaObjectStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface MediaObjectRepository : JpaRepository<MediaObjectEntity, UUID> {
    fun findByUserId(userId: UUID): List<MediaObjectEntity>
    fun findByUserIdAndStatus(userId: UUID, status: MediaObjectStatus): List<MediaObjectEntity>
    fun findByS3Key(s3Key: String): MediaObjectEntity?
    fun findByStatus(status: MediaObjectStatus): List<MediaObjectEntity>
}
