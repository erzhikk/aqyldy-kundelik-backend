package kz.aqyldykundelik.media.service

import kz.aqyldykundelik.config.MinioProperties
import kz.aqyldykundelik.media.api.dto.PhotoUploadResponseDto
import kz.aqyldykundelik.media.domain.MediaObjectStatus
import kz.aqyldykundelik.media.repo.MediaObjectRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.awt.image.BufferedImage
import java.security.MessageDigest
import java.util.*
import javax.imageio.ImageIO

@Service
class MediaUploadService(
    private val s3Client: S3Client,
    private val minioProperties: MinioProperties,
    private val mediaObjectRepository: MediaObjectRepository
) {

    companion object {
        const val MAX_FILE_SIZE = 5 * 1024 * 1024L  // 5 MB
        const val MIN_WIDTH = 256
        const val MIN_HEIGHT = 256
        const val MAX_WIDTH = 4000
        const val MAX_HEIGHT = 4000

        val ALLOWED_CONTENT_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "image/webp"  // Поддержка через TwelveMonkeys ImageIO
        )
    }

    /**
     * Загрузка фото через бэкенд (прямая загрузка в MinIO без presigned URL)
     */
    fun uploadPhoto(userId: UUID, file: MultipartFile): PhotoUploadResponseDto {
        // 1. Валидация content type
        if (file.contentType !in ALLOWED_CONTENT_TYPES) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid content type: ${file.contentType}. Allowed: ${ALLOWED_CONTENT_TYPES.joinToString()}"
            )
        }

        // 2. Валидация размера файла
        if (file.size > MAX_FILE_SIZE) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "File size ${file.size} exceeds maximum of $MAX_FILE_SIZE bytes"
            )
        }

        // 3. Чтение файла в память
        val fileBytes = file.bytes

        val image = readImage(fileBytes)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image file or corrupted")

        // 4. Валидация размеров изображения
        val width = image.width
        val height = image.height

        if (width < MIN_WIDTH || height < MIN_HEIGHT) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Image too small: ${width}x${height}. Minimum: ${MIN_WIDTH}x${MIN_HEIGHT}"
            )
        }

        if (width > MAX_WIDTH || height > MAX_HEIGHT) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Image too large: ${width}x${height}. Maximum: ${MAX_WIDTH}x${MAX_HEIGHT}"
            )
        }

        // 5. Генерация S3 key
        val fileExtension = getFileExtension(file.originalFilename ?: "photo.jpg")
        val s3Key = generateObjectKey(userId.toString(), fileExtension)

        // 6. Создание записи в БД со статусом UPLOADING
        val mediaObject = mediaObjectRepository.save(
            kz.aqyldykundelik.media.domain.MediaObjectEntity(
                userId = userId,
                s3Key = s3Key,
                contentType = file.contentType!!,
                status = MediaObjectStatus.UPLOADING
            )
        )

        try {
            // 7. Загрузка в MinIO
            val putRequest = PutObjectRequest.builder()
                .bucket(minioProperties.bucketName)
                .key(s3Key)
                .contentType(file.contentType)
                .contentLength(fileBytes.size.toLong())
                .build()

            s3Client.putObject(putRequest, RequestBody.fromBytes(fileBytes))

            // 8. Вычисление SHA256
            val sha256 = calculateSHA256(fileBytes)

            // 9. Обновление записи в БД - статус READY
            mediaObject.status = MediaObjectStatus.READY
            mediaObject.fileSize = fileBytes.size.toLong()
            mediaObject.width = width
            mediaObject.height = height
            mediaObject.sha256 = sha256
            mediaObjectRepository.save(mediaObject)

            return PhotoUploadResponseDto(
                mediaObjectId = mediaObject.id!!,
                key = s3Key,
                width = width,
                height = height,
                fileSize = fileBytes.size.toLong()
            )

        } catch (e: Exception) {
            // В случае ошибки - пометить как FAILED
            mediaObject.status = MediaObjectStatus.FAILED
            mediaObjectRepository.save(mediaObject)

            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to upload file: ${e.message}"
            )
        }
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
     * Прочитать изображение из байтов
     */
    private fun readImage(data: ByteArray): BufferedImage? {
        return try {
            ImageIO.read(data.inputStream())
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Вычислить SHA256 хэш
     */
    private fun calculateSHA256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
