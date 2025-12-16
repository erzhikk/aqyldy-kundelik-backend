package kz.aqyldykundelik.media.api

import kz.aqyldykundelik.media.api.dto.PhotoUploadResponseDto
import kz.aqyldykundelik.media.service.MediaUploadService
import kz.aqyldykundelik.media.service.MediaDownloadService
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.util.*

@RestController
@RequestMapping("/api/media")
class MediaController(
    private val mediaUploadService: MediaUploadService,
    private val mediaDownloadService: MediaDownloadService
) {

    /**
     * Прямая загрузка фото через бэкенд (обходит CORS проблему)
     *
     * Эндпоинт принимает файл от фронта, загружает в MinIO и валидирует.
     * Возвращает mediaObjectId для последующего использования.
     *
     * @param userId UUID пользователя
     * @param file Файл изображения (multipart/form-data)
     * @return Информация о загруженном файле
     */
    @PostMapping("/upload/photo")
    fun uploadPhoto(
        @RequestParam("userId") userId: String,
        @RequestParam("file") file: MultipartFile
    ): PhotoUploadResponseDto {
        val userUuid = try {
            UUID.fromString(userId)
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid userId format")
        }

        return mediaUploadService.uploadPhoto(userUuid, file)
    }

    /**
     * Получить изображение по ID медиа объекта (проксирование из MinIO)
     *
     * Этот эндпоинт решает CORS проблему - фронтенд запрашивает изображение у бэкенда,
     * а бэкенд получает его из MinIO и отдает фронтенду.
     */
    @GetMapping("/photo/{mediaObjectId}")
    fun getPhoto(@PathVariable mediaObjectId: String): ResponseEntity<Resource> {
        val mediaUuid = try {
            UUID.fromString(mediaObjectId)
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid mediaObjectId format")
        }

        val result = mediaDownloadService.downloadPhoto(mediaUuid)

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(result.contentType))
            .contentLength(result.contentLength)
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000") // Кэш на 1 год
            .body(result.resource)
    }
}
