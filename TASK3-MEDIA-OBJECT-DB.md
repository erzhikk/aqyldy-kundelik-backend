# ✅ Задача 3: БД и репозиторий media_object - ЗАВЕРШЕНА

## 🎯 Что реализовано

### 1. Flyway миграция V16

Создана миграция `V16__create_media_object.sql` с:

**Таблица media_object:**
- `id` (uuid) - первичный ключ с автогенерацией
- `user_id` (uuid) - внешний ключ на app_user с CASCADE DELETE
- `s3_key` (text) - уникальный ключ объекта в S3
- `content_type` (text) - MIME тип файла
- `file_size` (bigint) - размер файла в байтах (nullable)
- `status` (text) - статус загрузки с CHECK constraint
  - `UPLOADING` - файл в процессе загрузки
  - `CONFIRMED` - загрузка подтверждена
  - `DELETED` - мягкое удаление
- `created_at` (timestamptz) - дата создания записи
- `updated_at` (timestamptz) - дата обновления записи

**Индексы:**
- `idx_media_object_user_id` - по user_id для быстрого поиска файлов пользователя
- `idx_media_object_status` - по status для фильтрации по статусу

**Поле в app_user:**
- `photo_media_id` (uuid) - nullable внешний ключ на media_object
- Constraint: `fk_user_photo_media` с SET NULL при удалении
- Индекс: `idx_user_photo_media_id`

### 2. Entity - MediaObjectEntity

Создана JPA сущность с:
- Автогенерация UUID для id
- Enum `MediaObjectStatus` с тремя значениями
- **@Enumerated(EnumType.STRING)** - для сохранения enum как текст в БД
- @PrePersist и @PreUpdate lifecycle hooks для автоматического управления timestamps

```kotlin
enum class MediaObjectStatus {
    UPLOADING,
    CONFIRMED,
    DELETED
}
```

### 3. Repository - MediaObjectRepository

Spring Data JPA репозиторий с методами:
- `findByUserId(userId: UUID)` - все файлы пользователя
- `findByUserIdAndStatus(userId: UUID, status: MediaObjectStatus)` - файлы по статусу
- `findByS3Key(s3Key: String)` - поиск по S3 ключу
- `findByStatus(status: MediaObjectStatus)` - все файлы с определенным статусом

### 4. Service - MediaObjectService

Бизнес-логика для управления media_object:

**Создание:**
- `createUploading(userId, s3Key, contentType)` - создание записи со статусом UPLOADING

**Поиск:**
- `findById(id)` - по ID с выбросом 404 если не найдено
- `findByS3Key(s3Key)` - по S3 ключу
- `findByUserId(userId)` - все файлы пользователя
- `findByUserIdAndStatus(userId, status)` - файлы по статусу
- `findByStatus(status)` - все файлы с определенным статусом

**Обновление статуса:**
- `updateStatus(id, status)` - изменение статуса
- `confirmUpload(id, fileSize)` - подтверждение загрузки (UPLOADING → CONFIRMED)
- `confirmUploadByS3Key(s3Key, fileSize)` - подтверждение по S3 ключу
- `softDelete(id)` - мягкое удаление (статус → DELETED)

**Удаление:**
- `hardDelete(id)` - физическое удаление из БД

### 5. Интеграция с MediaPresignService

Обновлен `MediaPresignService.generatePhotoPresignUrl()`:
1. При запросе presigned URL создается запись в БД со статусом `UPLOADING`
2. В ответ добавлено поле `mediaObjectId` для последующего подтверждения
3. Инжектирован `MediaObjectService` через конструктор

### 6. Обновление DTO

`PhotoPresignResponseDto` теперь включает:
```kotlin
data class PhotoPresignResponseDto(
    val url: String,
    val key: String,
    val fields: Map<String, String>,
    val mediaObjectId: UUID  // NEW!
)
```

---

## 📁 Созданные/измененные файлы

### Новые файлы

```
src/main/resources/db/migration/
└── V16__create_media_object.sql           # Flyway миграция

src/main/kotlin/kz/aqyldykundelik/media/
├── domain/
│   └── MediaObjectEntity.kt               # Entity + Enum
├── repo/
│   └── MediaObjectRepository.kt           # Spring Data JPA repository
└── service/
    └── MediaObjectService.kt              # Бизнес-логика CRUD
```

### Измененные файлы

```
src/main/kotlin/kz/aqyldykundelik/media/
├── api/dto/
│   └── MediaDtos.kt                       # Добавлено поле mediaObjectId
└── service/
    └── MediaPresignService.kt             # Сохранение UPLOADING записи
```

---

## 🧪 Тестирование

### Сценарий 1: Создание presigned URL

**Запрос:**
```bash
curl -X POST http://localhost:8080/api/media/presign/photo \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "userId": "74700097-17b2-409c-84d1-087ccfa7561c",
    "contentType": "image/jpeg",
    "filename": "test-avatar.jpg"
  }'
```

**Ответ:**
```json
{
  "url": "http://localhost:9000/aq-media/users/.../photos/8649a17a-....jpg?X-Amz-...",
  "key": "users/74700097-17b2-409c-84d1-087ccfa7561c/photos/8649a17a-....jpg",
  "fields": {},
  "mediaObjectId": "1b5f491f-89ea-41e5-9b00-267dcc0e0df5"
}
```

### Сценарий 2: Проверка записи в БД

**Запрос:**
```sql
SELECT id, user_id, s3_key, content_type, status, file_size
FROM media_object
WHERE id = '1b5f491f-89ea-41e5-9b00-267dcc0e0df5';
```

**Результат:**
```
id                  | 1b5f491f-89ea-41e5-9b00-267dcc0e0df5
user_id             | 74700097-17b2-409c-84d1-087ccfa7561c
s3_key              | users/74700097-17b2-409c-84d1-087ccfa7561c/photos/8649a17a-....jpg
content_type        | image/jpeg
status              | UPLOADING
file_size           | NULL
```

✅ **Статус UPLOADING** - корректно!
✅ **file_size NULL** - ожидаемо, будет заполнено при подтверждении

---

## 🔄 Жизненный цикл media_object

### Этап 1: Запрос presigned URL
```
POST /api/media/presign/photo
  ↓
MediaPresignService.generatePhotoPresignUrl()
  ↓
MediaObjectService.createUploading()
  ↓
INSERT INTO media_object (status='UPLOADING', file_size=NULL)
  ↓
Возврат {url, key, mediaObjectId}
```

### Этап 2: Загрузка файла (клиент)
```
Frontend PUT request → MinIO
  ↓
Файл загружен в S3
```

### Этап 3: Подтверждение (будущая Task 4)
```
POST /api/media/photo/confirm
  ↓
MediaObjectService.confirmUpload(mediaObjectId, fileSize)
  ↓
UPDATE media_object SET status='CONFIRMED', file_size=...
  ↓
(опционально) UPDATE app_user SET photo_media_id=...
```

### Этап 4: Удаление (будущая Task 6)
```
DELETE /api/users/{id}/photo
  ↓
MediaObjectService.softDelete(mediaObjectId)
  ↓
UPDATE media_object SET status='DELETED'
```

---

## 🔧 Технические детали

### Enum vs String в PostgreSQL

**Проблема:** Hibernate по умолчанию использует `@Enumerated(EnumType.ORDINAL)`, что создает поле `smallint`, но Flyway миграция использует `text`.

**Решение:**
```kotlin
@Enumerated(EnumType.STRING)  // ← ОБЯЗАТЕЛЬНО!
@Column(nullable = false)
var status: MediaObjectStatus
```

### Каскадное удаление

При удалении пользователя:
```sql
ON DELETE CASCADE  -- media_object удаляется автоматически
```

При удалении media_object:
```sql
ON DELETE SET NULL  -- app_user.photo_media_id становится NULL
```

### Unique constraint на s3_key

Предотвращает дублирование записей для одного и того же файла в S3.

---

## 📊 Архитектура данных

```
┌─────────────┐           ┌──────────────────┐
│  app_user   │           │  media_object    │
├─────────────┤           ├──────────────────┤
│ id          │←─────┐    │ id               │
│ ...         │      │    │ user_id (FK) ────┼→ app_user.id
│ photo_media │      │    │ s3_key (UNIQUE)  │
│   _id (FK)  │──────┼───→│ content_type     │
└─────────────┘      │    │ file_size        │
                     │    │ status           │
                     │    │ created_at       │
                     │    │ updated_at       │
                     │    └──────────────────┘
                     │
              SET NULL    CASCADE DELETE
```

---

## ✅ Checklist

- [x] Flyway миграция V16 создана
- [x] Таблица media_object с корректными полями
- [x] Поле photo_media_id добавлено в app_user
- [x] Индексы для производительности
- [x] MediaObjectEntity с @Enumerated(STRING)
- [x] MediaObjectRepository с query methods
- [x] MediaObjectService с CRUD операциями
- [x] MediaPresignService интегрирован с MediaObjectService
- [x] PhotoPresignResponseDto содержит mediaObjectId
- [x] Приложение запускается без ошибок
- [x] Миграция применяется успешно
- [x] Эндпоинт создает запись со статусом UPLOADING
- [x] Запись в БД проверена

---

## 🎯 Следующие задачи

### Задача 4: Подтверждение загрузки
- [ ] Эндпоинт `POST /api/media/photo/confirm`
- [ ] Проверка существования файла в S3 (опционально)
- [ ] Обновление status: UPLOADING → CONFIRMED
- [ ] Получение размера файла из S3
- [ ] Обновление file_size
- [ ] Привязка к пользователю (app_user.photo_media_id)

### Задача 5: imgproxy интеграция
- [ ] Сервис для генерации подписанных imgproxy URLs
- [ ] Варианты: thumbnail (100x100), medium (300x300), large (600x600)
- [ ] Эндпоинт `GET /api/users/{id}/photo?size=medium`

### Задача 6: Удаление фото
- [ ] Эндпоинт `DELETE /api/users/{id}/photo`
- [ ] Soft delete: status → DELETED
- [ ] Удаление из S3 (опционально)
- [ ] Удаление из app_user.photo_media_id

---

## 🚀 Итого

**Статус:** ✅ **ЗАВЕРШЕНО**

Реализована полная persistence-слой для управления медиа-объектами:
- ✅ Структура БД с миграциями
- ✅ Entity с корректным маппингом enum
- ✅ Repository с удобными query methods
- ✅ Service с полным набором CRUD операций
- ✅ Интеграция с presigned URL - автоматическое создание UPLOADING записи
- ✅ mediaObjectId в ответе API для последующего подтверждения

**Готово к реализации Task 4: Подтверждение загрузки!**
