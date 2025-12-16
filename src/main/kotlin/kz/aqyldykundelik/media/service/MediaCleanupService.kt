package kz.aqyldykundelik.media.service

import kz.aqyldykundelik.config.MinioProperties
import kz.aqyldykundelik.media.domain.MediaObjectStatus
import kz.aqyldykundelik.media.repo.MediaObjectRepository
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import java.util.*

@Service
class MediaCleanupService(
    private val s3Client: S3Client,
    private val minioProperties: MinioProperties,
    private val mediaObjectRepository: MediaObjectRepository
) {

    /**
     * Удалить медиа объект из хранилища и пометить как DELETED в БД
     */
    fun deleteMediaObject(mediaObjectId: UUID) {
        val mediaObject = mediaObjectRepository.findById(mediaObjectId).orElse(null)
            ?: return // Объект уже удален или не существует

        // Удалить файл из MinIO
        mediaObject.s3Key?.let { s3Key ->
            try {
                val deleteRequest = DeleteObjectRequest.builder()
                    .bucket(minioProperties.bucketName)
                    .key(s3Key)
                    .build()

                s3Client.deleteObject(deleteRequest)
            } catch (e: Exception) {
                // Логируем ошибку, но не бросаем исключение
                // Файл мог быть уже удален вручную
            }
        }

        // Пометить запись как DELETED в БД
        mediaObject.status = MediaObjectStatus.DELETED
        mediaObject.s3Key = null // Очищаем ссылку на удаленный файл
        mediaObjectRepository.save(mediaObject)
    }
}
