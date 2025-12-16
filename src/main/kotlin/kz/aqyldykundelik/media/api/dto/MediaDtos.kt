package kz.aqyldykundelik.media.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import java.util.*

/**
 * Запрос на получение presigned POST URL для загрузки фото
 */
data class PhotoPresignRequestDto(
    @field:NotBlank
    val userId: String,  // UUID пользователя

    @field:NotBlank
    @field:Pattern(regexp = "^image/(jpeg|png|webp)$", message = "Only JPEG, PNG, WebP are allowed")
    val contentType: String,  // image/jpeg, image/png, image/webp

    @field:NotBlank
    @field:Pattern(regexp = "^[a-zA-Z0-9._-]+\\.(jpg|jpeg|png|webp)$", message = "Invalid filename")
    val filename: String  // Имя файла
)

/**
 * Ответ с presigned POST URL и полями для формы
 */
data class PhotoPresignResponseDto(
    val url: String,  // URL для POST запроса
    val key: String,  // Ключ объекта в S3
    val fields: Map<String, String>,  // Поля для формы (policy, signature и т.д.)
    val mediaObjectId: UUID  // ID записи в БД (для подтверждения загрузки)
)

/**
 * Ответ reconcile (валидация загруженного файла)
 */
data class ReconcileResponseDto(
    val success: Boolean,
    val key: String,
    val width: Int? = null,
    val height: Int? = null,
    val fileSize: Long? = null,
    val sha256: String? = null,
    val reason: String? = null
)
