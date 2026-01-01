package kz.aqyldykundelik.media.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import java.util.*

/**
 * Запрос на генерацию presigned URL для загрузки фото
 */
data class PhotoPresignRequestDto(
    val userId: String,  // UUID пользователя

    @field:Pattern(
        regexp = "image/(jpeg|png|webp)",
        message = "Content type must be image/jpeg, image/png, or image/webp"
    )
    val contentType: String,  // MIME тип файла

    @field:NotBlank
    @field:Pattern(
        regexp = "[a-zA-Z0-9._-]+\\.(jpg|jpeg|png|webp)",
        message = "Filename must contain only alphanumeric characters and have valid image extension"
    )
    val filename: String  // Имя файла с расширением
)

/**
 * Ответ с presigned URL для загрузки фото
 */
data class PhotoPresignResponseDto(
    val url: String,  // Presigned URL для PUT запроса
    val key: String,  // S3 ключ файла (users/{userId}/photos/{uuid}.ext)
    val fields: Map<String, String>,  // Дополнительные поля для POST form (пустой для PUT)
    val mediaObjectId: UUID  // ID медиа объекта в БД
)

/**
 * Ответ на прямую загрузку фото через бэкенд
 */
data class PhotoUploadResponseDto(
    val mediaObjectId: UUID,  // ID медиа объекта для привязки к пользователю
    val key: String,          // S3 ключ файла
    val width: Int,           // Ширина изображения
    val height: Int,          // Высота изображения
    val fileSize: Long        // Размер файла в байтах
)
