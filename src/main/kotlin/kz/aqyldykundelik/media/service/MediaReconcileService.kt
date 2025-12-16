package kz.aqyldykundelik.media.service

import kz.aqyldykundelik.config.MinioProperties
import kz.aqyldykundelik.media.domain.MediaObjectStatus
import kz.aqyldykundelik.media.repo.MediaObjectRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import javax.imageio.ImageIO

@Service
class MediaReconcileService(
    private val s3Client: S3Client,
    private val minioProperties: MinioProperties,
    private val mediaObjectRepository: MediaObjectRepository
) {
    private val logger = LoggerFactory.getLogger(MediaReconcileService::class.java)

    companion object {
        const val MIN_WIDTH = 256
        const val MIN_HEIGHT = 256
        const val MAX_WIDTH = 4000
        const val MAX_HEIGHT = 4000
        const val MAX_FILE_SIZE = 5 * 1024 * 1024L  // 5 MB

        val ALLOWED_CONTENT_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "image/webp"
        )
    }

    /**
     * Reconcile (валидация) загруженного файла
     *
     * 1. Скачивает файл из S3
     * 2. Валидирует MIME type
     * 3. Проверяет размеры изображения
     * 4. Вычисляет SHA256
     * 5. Обновляет статус READY или FAILED
     */
    fun reconcile(s3Key: String): ReconcileResult {
        logger.info("Starting reconciliation for key: $s3Key")

        // Найти запись в БД
        val mediaObject = mediaObjectRepository.findByS3Key(s3Key)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Media object not found for key: $s3Key")

        if (mediaObject.status != MediaObjectStatus.UPLOADING) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Cannot reconcile media object with status ${mediaObject.status}. Expected UPLOADING."
            )
        }

        try {
            // 1. Скачать файл из S3
            logger.debug("Downloading object from S3: $s3Key")
            val objectData = downloadFromS3(s3Key)

            // Проверка размера файла
            if (objectData.size > MAX_FILE_SIZE) {
                logger.warn("File size exceeded: ${objectData.size} > $MAX_FILE_SIZE")
                markAsFailed(mediaObject.id!!, "File size exceeded maximum of $MAX_FILE_SIZE bytes")
                deleteFromS3(s3Key)
                return ReconcileResult(
                    success = false,
                    reason = "File too large: ${objectData.size} bytes"
                )
            }

            // 2. Вычислить SHA256
            val sha256 = calculateSHA256(objectData)
            logger.debug("Calculated SHA256: $sha256")

            // 3. Прочитать изображение и извлечь метаданные
            val image = readImage(objectData)
                ?: run {
                    logger.warn("Failed to read image from bytes")
                    markAsFailed(mediaObject.id!!, "Invalid image format or corrupted file")
                    deleteFromS3(s3Key)
                    return ReconcileResult(
                        success = false,
                        reason = "Invalid image format or corrupted file"
                    )
                }

            val width = image.width
            val height = image.height
            logger.debug("Image dimensions: ${width}x${height}")

            // 4. Валидация размеров
            if (width < MIN_WIDTH || height < MIN_HEIGHT) {
                logger.warn("Image too small: ${width}x${height} < ${MIN_WIDTH}x${MIN_HEIGHT}")
                markAsFailed(mediaObject.id!!, "Image too small: minimum ${MIN_WIDTH}x${MIN_HEIGHT}")
                deleteFromS3(s3Key)
                return ReconcileResult(
                    success = false,
                    reason = "Image too small: ${width}x${height}"
                )
            }

            if (width > MAX_WIDTH || height > MAX_HEIGHT) {
                logger.warn("Image too large: ${width}x${height} > ${MAX_WIDTH}x${MAX_HEIGHT}")
                markAsFailed(mediaObject.id!!, "Image too large: maximum ${MAX_WIDTH}x${MAX_HEIGHT}")
                deleteFromS3(s3Key)
                return ReconcileResult(
                    success = false,
                    reason = "Image too large: ${width}x${height}"
                )
            }

            // 5. Валидация MIME type
            if (mediaObject.contentType !in ALLOWED_CONTENT_TYPES) {
                logger.warn("Invalid content type: ${mediaObject.contentType}")
                markAsFailed(mediaObject.id!!, "Invalid content type: ${mediaObject.contentType}")
                deleteFromS3(s3Key)
                return ReconcileResult(
                    success = false,
                    reason = "Invalid content type: ${mediaObject.contentType}"
                )
            }

            // 6. Обновить запись в БД - статус READY
            mediaObject.status = MediaObjectStatus.READY
            mediaObject.fileSize = objectData.size.toLong()
            mediaObject.width = width
            mediaObject.height = height
            mediaObject.sha256 = sha256
            mediaObjectRepository.save(mediaObject)

            logger.info("Successfully reconciled object: $s3Key (${width}x${height}, ${objectData.size} bytes)")

            return ReconcileResult(
                success = true,
                width = width,
                height = height,
                fileSize = objectData.size.toLong(),
                sha256 = sha256
            )

        } catch (e: Exception) {
            logger.error("Error during reconciliation for key $s3Key", e)
            markAsFailed(mediaObject.id!!, "Reconciliation error: ${e.message}")

            // Попытка удалить файл из S3
            try {
                deleteFromS3(s3Key)
            } catch (deleteError: Exception) {
                logger.error("Failed to delete object from S3: $s3Key", deleteError)
            }

            return ReconcileResult(
                success = false,
                reason = "Internal error: ${e.message}"
            )
        }
    }

    /**
     * Скачать файл из S3
     */
    private fun downloadFromS3(s3Key: String): ByteArray {
        val getObjectRequest = GetObjectRequest.builder()
            .bucket(minioProperties.bucketName)
            .key(s3Key)
            .build()

        val responseInputStream: ResponseInputStream<GetObjectResponse> = s3Client.getObject(getObjectRequest)
        return responseInputStream.readBytes()
    }

    /**
     * Вычислить SHA256 хэш
     */
    private fun calculateSHA256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Прочитать изображение из байтов
     */
    private fun readImage(data: ByteArray): BufferedImage? {
        return try {
            ImageIO.read(data.inputStream())
        } catch (e: Exception) {
            logger.error("Failed to read image", e)
            null
        }
    }

    /**
     * Пометить объект как FAILED
     */
    private fun markAsFailed(mediaObjectId: java.util.UUID, reason: String) {
        try {
            val mediaObject = mediaObjectRepository.findById(mediaObjectId).orElse(null)
            if (mediaObject != null) {
                mediaObject.status = MediaObjectStatus.FAILED
                mediaObjectRepository.save(mediaObject)
                logger.info("Marked media object $mediaObjectId as FAILED: $reason")
            }
        } catch (e: Exception) {
            logger.error("Failed to mark media object as FAILED", e)
        }
    }

    /**
     * Удалить объект из S3
     */
    private fun deleteFromS3(s3Key: String) {
        try {
            val deleteRequest = DeleteObjectRequest.builder()
                .bucket(minioProperties.bucketName)
                .key(s3Key)
                .build()
            s3Client.deleteObject(deleteRequest)
            logger.info("Deleted object from S3: $s3Key")
        } catch (e: Exception) {
            logger.error("Failed to delete object from S3: $s3Key", e)
            throw e
        }
    }
}

/**
 * Результат reconcile операции
 */
data class ReconcileResult(
    val success: Boolean,
    val width: Int? = null,
    val height: Int? = null,
    val fileSize: Long? = null,
    val sha256: String? = null,
    val reason: String? = null
)
