package kz.aqyldykundelik.media.service

import kz.aqyldykundelik.media.domain.MediaObjectEntity
import kz.aqyldykundelik.media.domain.MediaObjectStatus
import kz.aqyldykundelik.media.repo.MediaObjectRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Service
class MediaObjectService(
    private val mediaObjectRepository: MediaObjectRepository
) {

    /**
     * Create new media object with UPLOADING status
     */
    fun createUploading(userId: UUID, s3Key: String, contentType: String): MediaObjectEntity {
        val entity = MediaObjectEntity(
            userId = userId,
            s3Key = s3Key,
            contentType = contentType,
            status = MediaObjectStatus.UPLOADING
        )
        return mediaObjectRepository.save(entity)
    }

    /**
     * Find media object by ID
     */
    fun findById(id: UUID): MediaObjectEntity {
        return mediaObjectRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Media object not found") }
    }

    /**
     * Find media object by S3 key
     */
    fun findByS3Key(s3Key: String): MediaObjectEntity? {
        return mediaObjectRepository.findByS3Key(s3Key)
    }

    /**
     * Find all media objects for a user
     */
    fun findByUserId(userId: UUID): List<MediaObjectEntity> {
        return mediaObjectRepository.findByUserId(userId)
    }

    /**
     * Find media objects by user ID and status
     */
    fun findByUserIdAndStatus(userId: UUID, status: MediaObjectStatus): List<MediaObjectEntity> {
        return mediaObjectRepository.findByUserIdAndStatus(userId, status)
    }

    /**
     * Find all media objects with specific status
     */
    fun findByStatus(status: MediaObjectStatus): List<MediaObjectEntity> {
        return mediaObjectRepository.findByStatus(status)
    }

    /**
     * Update media object status
     */
    fun updateStatus(id: UUID, status: MediaObjectStatus): MediaObjectEntity {
        val entity = findById(id)
        entity.status = status
        return mediaObjectRepository.save(entity)
    }


    /**
     * Soft delete - set status to DELETED
     */
    fun softDelete(id: UUID): MediaObjectEntity {
        val entity = findById(id)
        entity.status = MediaObjectStatus.DELETED
        return mediaObjectRepository.save(entity)
    }

    /**
     * Hard delete - permanently remove from database
     */
    fun hardDelete(id: UUID) {
        if (!mediaObjectRepository.existsById(id)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Media object not found")
        }
        mediaObjectRepository.deleteById(id)
    }
}
