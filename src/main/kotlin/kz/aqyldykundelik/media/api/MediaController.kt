package kz.aqyldykundelik.media.api

import jakarta.validation.Valid
import kz.aqyldykundelik.media.api.dto.PhotoPresignRequestDto
import kz.aqyldykundelik.media.api.dto.PhotoPresignResponseDto
import kz.aqyldykundelik.media.api.dto.ReconcileResponseDto
import kz.aqyldykundelik.media.service.MediaPresignService
import kz.aqyldykundelik.media.service.MediaReconcileService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/media")
class MediaController(
    private val mediaPresignService: MediaPresignService,
    private val mediaReconcileService: MediaReconcileService
) {

    /**
     * Получить presigned URL для загрузки фото
     *
     * Эндпоинт генерирует presigned PUT URL для прямой загрузки изображения в MinIO.
     *
     * Ограничения:
     * - Максимальный размер: 5 MB
     * - Допустимые форматы: image/jpeg, image/png, image/webp
     * - Префикс ключа: users/{userId}/photos/
     *
     * @param request запрос с userId, contentType и filename
     * @return presigned URL, ключ объекта и поля формы
     */
    @PostMapping("/presign/photo")
    fun presignPhotoUpload(@Valid @RequestBody request: PhotoPresignRequestDto): PhotoPresignResponseDto {
        return mediaPresignService.generatePhotoPresignUrl(request)
    }

    /**
     * Reconcile (валидация) загруженного файла
     *
     * Dev-эндпоинт для ручной валидации загруженного изображения.
     * В production должен вызываться воркером из очереди.
     *
     * Процесс:
     * 1. Скачивает файл из S3 по ключу
     * 2. Валидирует MIME type
     * 3. Проверяет размеры изображения (256x256 - 4000x4000)
     * 4. Вычисляет SHA256
     * 5. Обновляет статус на READY или FAILED
     * 6. Удаляет файл из S3 при провале валидации
     *
     * @param key S3 ключ объекта (storage_key)
     * @return результат валидации
     */
    @PostMapping("/reconcile")
    fun reconcile(@RequestParam key: String): ReconcileResponseDto {
        val result = mediaReconcileService.reconcile(key)
        return ReconcileResponseDto(
            success = result.success,
            key = key,
            width = result.width,
            height = result.height,
            fileSize = result.fileSize,
            sha256 = result.sha256,
            reason = result.reason
        )
    }
}
