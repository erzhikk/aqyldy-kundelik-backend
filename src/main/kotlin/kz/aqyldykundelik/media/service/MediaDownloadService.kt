package kz.aqyldykundelik.media.service

import kz.aqyldykundelik.config.MinioProperties
import kz.aqyldykundelik.media.domain.MediaObjectStatus
import kz.aqyldykundelik.media.repo.MediaObjectRepository
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import java.util.*

@Service
class MediaDownloadService(
    private val s3Client: S3Client,
    private val minioProperties: MinioProperties,
    private val mediaObjectRepository: MediaObjectRepository
) {

    /**
     * Получить файл изображения по mediaObjectId
     */
    fun downloadPhoto(mediaObjectId: UUID): PhotoDownloadResult {
        // Найти media object в БД
        val mediaObject = mediaObjectRepository.findById(mediaObjectId).orElse(null)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Media object not found")

        // Проверить статус - только READY
        if (mediaObject.status != MediaObjectStatus.READY) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Media object is not ready for download. Status: ${mediaObject.status}"
            )
        }

        // Скачать файл из MinIO
        val s3Key = mediaObject.s3Key
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "S3 key is null")

        try {
            val getObjectRequest = GetObjectRequest.builder()
                .bucket(minioProperties.bucketName)
                .key(s3Key)
                .build()

            val responseInputStream: ResponseInputStream<GetObjectResponse> = s3Client.getObject(getObjectRequest)
            val fileBytes = responseInputStream.readBytes()

            return PhotoDownloadResult(
                resource = ByteArrayResource(fileBytes),
                contentType = mediaObject.contentType ?: "application/octet-stream",
                contentLength = fileBytes.size.toLong()
            )

        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to download file from storage: ${e.message}"
            )
        }
    }
}

/**
 * Результат скачивания фото
 */
data class PhotoDownloadResult(
    val resource: Resource,
    val contentType: String,
    val contentLength: Long
)
