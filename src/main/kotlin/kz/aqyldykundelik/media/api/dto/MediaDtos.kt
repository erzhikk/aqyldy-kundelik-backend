package kz.aqyldykundelik.media.api.dto

import java.util.*

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
