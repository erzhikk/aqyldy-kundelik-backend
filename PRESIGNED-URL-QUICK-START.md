# 🚀 Quick Start: Presigned URL для загрузки фото

## 📌 Эндпоинт

```
POST /api/media/presign/photo
```

## 🔑 Авторизация

Требуется JWT Bearer Token

## 📥 Запрос

```json
{
  "userId": "74700097-17b2-409c-84d1-087ccfa7561c",
  "contentType": "image/jpeg",
  "filename": "avatar.jpg"
}
```

## 📤 Ответ

```json
{
  "url": "http://localhost:9000/aq-media/users/.../photos/....jpg?X-Amz-...",
  "key": "users/74700097-17b2-409c-84d1-087ccfa7561c/photos/uuid.jpg",
  "fields": {}
}
```

## 📝 Допустимые значения

**Content-Type:**
- `image/jpeg`
- `image/png`
- `image/webp`

**Filename:**
- Формат: `[a-zA-Z0-9._-]+\.(jpg|jpeg|png|webp)`
- Примеры: `avatar.jpg`, `photo_1.png`, `user-image.webp`

## 🔧 Ограничения

- Максимальный размер: **5 MB**
- Срок действия URL: **15 минут**
- Префикс ключа: `users/{userId}/photos/`

---

## 💻 Примеры

### cURL

```bash
# 1. Получить токен
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@local","password":"admin123"}' \
  | jq -r '.accessToken')

# 2. Получить presigned URL
PRESIGNED=$(curl -s -X POST http://localhost:8080/api/media/presign/photo \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "userId": "74700097-17b2-409c-84d1-087ccfa7561c",
    "contentType": "image/jpeg",
    "filename": "avatar.jpg"
  }')

echo $PRESIGNED | jq .

# 3. Загрузить файл
URL=$(echo $PRESIGNED | jq -r '.url')
curl -X PUT "$URL" \
  -H "Content-Type: image/jpeg" \
  --data-binary @avatar.jpg
```

### JavaScript / TypeScript

```typescript
async function uploadAvatar(userId: string, file: File) {
  // 1. Получить presigned URL
  const presignRes = await fetch('/api/media/presign/photo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${accessToken}`
    },
    body: JSON.stringify({
      userId,
      contentType: file.type,
      filename: file.name
    })
  });

  const { url, key } = await presignRes.json();

  // 2. Загрузить файл в MinIO
  await fetch(url, {
    method: 'PUT',
    headers: {
      'Content-Type': file.type
    },
    body: file
  });

  // 3. Вернуть ключ для сохранения
  return key;
}

// Использование
const fileInput = document.querySelector('input[type="file"]');
const file = fileInput.files[0];
const key = await uploadAvatar(userId, file);
console.log('Uploaded:', key);
```

### React Hook

```typescript
import { useState } from 'react';

export function usePhotoUpload() {
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const upload = async (userId: string, file: File) => {
    setUploading(true);
    setError(null);

    try {
      // Получить presigned URL
      const presignRes = await fetch('/api/media/presign/photo', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
        },
        body: JSON.stringify({
          userId,
          contentType: file.type,
          filename: file.name
        })
      });

      if (!presignRes.ok) {
        throw new Error('Failed to get presigned URL');
      }

      const { url, key } = await presignRes.json();

      // Загрузить файл
      const uploadRes = await fetch(url, {
        method: 'PUT',
        headers: {
          'Content-Type': file.type
        },
        body: file
      });

      if (!uploadRes.ok) {
        throw new Error('Failed to upload file');
      }

      return key;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Upload failed');
      throw err;
    } finally {
      setUploading(false);
    }
  };

  return { upload, uploading, error };
}

// Использование в компоненте
function AvatarUpload({ userId }: { userId: string }) {
  const { upload, uploading, error } = usePhotoUpload();

  const handleChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      const key = await upload(userId, file);
      alert(`Uploaded: ${key}`);
    } catch (err) {
      console.error('Upload error:', err);
    }
  };

  return (
    <div>
      <input
        type="file"
        accept="image/jpeg,image/png,image/webp"
        onChange={handleChange}
        disabled={uploading}
      />
      {uploading && <p>Uploading...</p>}
      {error && <p style={{ color: 'red' }}>{error}</p>}
    </div>
  );
}
```

---

## ⚠️ Важные моменты

### 1. Content-Type ОБЯЗАТЕЛЕН

При загрузке файла через presigned URL, Content-Type **MUST** совпадать:

```typescript
// ❌ НЕПРАВИЛЬНО - Content-Type не совпадает
const presigned = await getPresignedUrl({
  contentType: 'image/jpeg',  // Указали JPEG
  ...
});

await fetch(presigned.url, {
  method: 'PUT',
  headers: {
    'Content-Type': 'image/png'  // ❌ Загружаем PNG - ОШИБКА!
  },
  body: file
});

// ✅ ПРАВИЛЬНО - Content-Type совпадает
const presigned = await getPresignedUrl({
  contentType: file.type,  // Берем из файла
  ...
});

await fetch(presigned.url, {
  method: 'PUT',
  headers: {
    'Content-Type': file.type  // ✅ Тот же самый тип
  },
  body: file
});
```

### 2. Время жизни URL

Presigned URL действителен **15 минут** с момента генерации.

Если загрузка не началась в течение 15 минут - нужно запросить новый URL.

### 3. Проверка размера на клиенте

```typescript
const MAX_SIZE = 5 * 1024 * 1024; // 5 MB

if (file.size > MAX_SIZE) {
  alert('File is too large. Maximum size is 5 MB.');
  return;
}
```

### 4. Проверка типа файла

```typescript
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

if (!ALLOWED_TYPES.includes(file.type)) {
  alert('Invalid file type. Only JPEG, PNG, WebP allowed.');
  return;
}
```

---

## 🐛 Troubleshooting

### Ошибка: "Signature does not match"

**Причина:** Content-Type не совпадает

**Решение:** Убедитесь что Content-Type в PUT запросе совпадает с указанным при генерации URL

### Ошибка: "Request has expired"

**Причина:** Прошло больше 15 минут с момента генерации URL

**Решение:** Запросите новый presigned URL

### Ошибка: "Access Denied"

**Причина:** Неверные credentials или bucket не существует

**Решение:**
1. Проверьте настройки в `application-dev.yml`
2. Убедитесь что бакет `aq-media` создан
3. Проверьте access key и secret key

### Ошибка: "File too large"

**Причина:** Файл больше 5 MB

**Решение:** Проверьте размер файла на клиенте перед загрузкой

---

## 📚 Документация

Для подробной информации см.:
- **TASK2-PRESIGNED-POST.md** - Полная документация задачи 2
- **MINIO-IMGPROXY-SETUP.md** - Настройка инфраструктуры
- **Swagger UI**: http://localhost:8080/swagger-ui.html

---

## ✅ Checklist фронтенда

- [ ] Валидация размера файла (≤ 5 MB)
- [ ] Валидация типа файла (JPEG/PNG/WebP)
- [ ] Отображение прогресса загрузки
- [ ] Обработка ошибок
- [ ] Показ превью изображения
- [ ] Сохранение `key` из ответа для дальнейшего использования
