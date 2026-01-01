package kz.aqyldykundelik.media.service

import kz.aqyldykundelik.config.MinioProperties
import kz.aqyldykundelik.media.api.dto.PhotoPresignRequestDto
import kz.aqyldykundelik.media.api.dto.PhotoPresignResponseDto
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.net.URL
import java.time.Duration
import java.util.*

@Service
class MediaPresignService(
    private val s3Presigner: S3Presigner,
    private val minioProperties: MinioProperties,
    private val mediaObjectService: MediaObjectService
) {

    companion object {
        const val MAX_FILE_SIZE = 5 * 1024 * 1024L  // 5 MB
        const val PRESIGN_DURATION_MINUTES = 15L
        val ALLOWED_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/webp")
    }

    /**
     * Генерация presigned POST URL для загрузки фото
     *
     * AWS SDK v2 не поддерживает presigned POST напрямую (только PUT).
     * Для полноценного presigned POST с политиками нужно использовать низкоуровневый API.
     * Пока реализуем через presigned PUT, который проще и безопаснее.
     *
     * Для presigned POST нужно будет добавить дополнительную логику генерации policy и signature.
     */
    fun generatePhotoPresignUrl(request: PhotoPresignRequestDto): PhotoPresignResponseDto {
        // Валидация content type
        if (request.contentType !in ALLOWED_CONTENT_TYPES) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Content type ${request.contentType} is not allowed. Allowed: ${ALLOWED_CONTENT_TYPES.joinToString()}"
            )
        }

        // Генерация уникального ключа
        val fileExtension = getFileExtension(request.filename)
        val userId = UUID.fromString(request.userId)
        val objectKey = generateObjectKey(request.userId, fileExtension)

        // Сохраняем запись в БД со статусом UPLOADING
        val mediaObject = mediaObjectService.createUploading(
            userId = userId,
            s3Key = objectKey,
            contentType = request.contentType
        )

        // Создание presigned PUT URL (более простая альтернатива POST)
        val putObjectRequest = PutObjectRequest.builder()
            .bucket(minioProperties.bucketName)
            .key(objectKey)
            .contentType(request.contentType)
            .build()

        val presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(PRESIGN_DURATION_MINUTES))
            .putObjectRequest(putObjectRequest)
            .build()

        val presignedRequest: PresignedPutObjectRequest = s3Presigner.presignPutObject(presignRequest)

        // Преобразуем URL из virtual-hosted style в path-style для MinIO
        val originalUrl = presignedRequest.url().toString()
        val pathStyleUrl = convertToPathStyle(originalUrl, minioProperties.bucketName, minioProperties.endpoint)

        // Формирование ответа
        // Для PUT request fields будут пустыми, т.к. это не POST form
        return PhotoPresignResponseDto(
            url = pathStyleUrl,
            key = objectKey,
            fields = emptyMap(),  // Для PUT не нужны дополнительные поля
            mediaObjectId = mediaObject.id!!  // ID записи в БД
        )
    }


    /**
     * Генерация ключа объекта в формате: users/{userId}/photos/{uuid}.{ext}
     */
    private fun generateObjectKey(userId: String, extension: String): String {
        val uniqueId = UUID.randomUUID().toString()
        return "users/${userId}/photos/${uniqueId}.${extension}"
    }

    /**
     * Получение расширения файла
     */
    private fun getFileExtension(filename: String): String {
        return filename.substringAfterLast('.', "").lowercase()
            .takeIf { it.isNotEmpty() }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename: no extension")
    }


    /**
     * Конвертация URL из virtual-hosted style в path-style
     *
     * Из: http://bucket.localhost:9000/key?params
     * В: http://localhost:9000/bucket/key?params
     */
    private fun convertToPathStyle(url: String, bucketName: String, endpoint: String): String {
        // Парсим URL
        val urlObj = URL(url)
        val host = urlObj.host
        val path = urlObj.path
        val query = if (urlObj.query != null) "?${urlObj.query}" else ""

        // Если хост начинается с bucket name - это virtual-hosted style
        if (host.startsWith("$bucketName.")) {
            // Убираем bucket name из хоста
            val endpointUrl = URL(endpoint)
            val newHost = endpointUrl.host
            val port = if (endpointUrl.port != -1) ":${endpointUrl.port}" else ""

            // Формируем path-style URL
            return "${endpointUrl.protocol}://$newHost$port/$bucketName$path$query"
        }

        // Если уже path-style - возвращаем как есть
        return url
    }
}
