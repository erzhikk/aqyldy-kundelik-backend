# 🌐 MinIO CORS Configuration

## Проблема

При загрузке файлов с фронтенда (Angular на `http://localhost:4200`) в MinIO через presigned URL, браузер блокирует запросы из-за CORS (Cross-Origin Resource Sharing) политики.

**Типичная ошибка в консоли:**
```
Access to fetch at 'http://localhost:9000/aq-media/users/...' from origin 'http://localhost:4200'
has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
```

## Решение

Нужно настроить CORS политику для MinIO bucket `aq-media`.

---

## Вариант 1: Автоматическая настройка (при инициализации)

Если вы еще не инициализировали MinIO, используйте обновленные скрипты инициализации:

### Linux/Mac:
```bash
cd scripts
chmod +x init-minio.sh
./init-minio.sh
```

### Windows (PowerShell):
```powershell
cd scripts
.\init-minio.ps1
```

**Эти скрипты автоматически настроят CORS при создании bucket.**

---

## Вариант 2: Применить CORS к существующему bucket

Если MinIO уже инициализирован, используйте отдельный скрипт для настройки CORS:

### Linux/Mac:
```bash
cd scripts
chmod +x setup-minio-cors.sh
./setup-minio-cors.sh
```

### Windows (PowerShell):
```powershell
cd scripts
.\setup-minio-cors.ps1
```

---

## Вариант 3: Ручная настройка через MinIO Client

### 1. Установка MinIO Client (mc)

**Linux:**
```bash
wget https://dl.min.io/client/mc/release/linux-amd64/mc
chmod +x mc
sudo mv mc /usr/local/bin/
```

**Mac:**
```bash
brew install minio/stable/mc
```

**Windows:**
Скачайте [mc.exe](https://dl.min.io/client/mc/release/windows-amd64/mc.exe) и добавьте в PATH.

### 2. Подключение к MinIO

```bash
mc alias set local http://localhost:9000 minioadmin minioadmin123
```

### 3. Создание CORS конфигурации

Создайте файл `cors-config.json`:

```json
{
  "CORSRules": [
    {
      "AllowedOrigins": ["http://localhost:4200", "http://localhost:8080"],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
      "AllowedHeaders": ["*"],
      "ExposeHeaders": ["ETag", "Content-Length", "Content-Type"],
      "MaxAgeSeconds": 3600
    }
  ]
}
```

### 4. Применение CORS

```bash
mc anonymous set-json cors-config.json local/aq-media
```

### 5. Проверка

```bash
mc anonymous get-json local/aq-media
```

Должно вывести примененную CORS конфигурацию.

---

## CORS конфигурация

### Параметры

| Параметр | Значение | Описание |
|----------|----------|----------|
| **AllowedOrigins** | `http://localhost:4200`<br>`http://localhost:8080` | Разрешенные origins (фронтенд и бэкенд) |
| **AllowedMethods** | `GET, PUT, POST, DELETE, HEAD` | Разрешенные HTTP методы |
| **AllowedHeaders** | `*` | Все заголовки разрешены |
| **ExposeHeaders** | `ETag, Content-Length, Content-Type` | Заголовки доступные в ответе |
| **MaxAgeSeconds** | `3600` | Кэш preflight запросов (1 час) |

### Для production

Измените `AllowedOrigins` на ваши production URLs:

```json
{
  "CORSRules": [
    {
      "AllowedOrigins": [
        "https://yourdomain.com",
        "https://api.yourdomain.com"
      ],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
      "AllowedHeaders": ["*"],
      "ExposeHeaders": ["ETag", "Content-Length", "Content-Type"],
      "MaxAgeSeconds": 3600
    }
  ]
}
```

---

## Проверка работы CORS

### 1. Через DevTools

Откройте фронтенд (`http://localhost:4200`) и попробуйте загрузить файл.

В Network вкладке проверьте:

**Preflight OPTIONS запрос:**
```
Request URL: http://localhost:9000/aq-media/users/.../photos/file.jpg
Request Method: OPTIONS
Status Code: 200 OK

Response Headers:
  Access-Control-Allow-Origin: http://localhost:4200
  Access-Control-Allow-Methods: GET, PUT, POST, DELETE, HEAD
  Access-Control-Allow-Headers: *
```

**Основной PUT запрос:**
```
Request URL: http://localhost:9000/aq-media/users/.../photos/file.jpg
Request Method: PUT
Status Code: 200 OK

Response Headers:
  Access-Control-Allow-Origin: http://localhost:4200
  Access-Control-Expose-Headers: ETag, Content-Length, Content-Type
```

### 2. Через curl

**Preflight запрос:**
```bash
curl -X OPTIONS http://localhost:9000/aq-media/test \
  -H "Origin: http://localhost:4200" \
  -H "Access-Control-Request-Method: PUT" \
  -H "Access-Control-Request-Headers: Content-Type" \
  -v
```

Должны быть заголовки:
```
< Access-Control-Allow-Origin: http://localhost:4200
< Access-Control-Allow-Methods: GET, PUT, POST, DELETE, HEAD
< Access-Control-Allow-Headers: *
```

---

## Troubleshooting

### Ошибка: "mc: command not found"

**Решение:** Установите MinIO Client (см. раздел выше).

### Ошибка: "mc: Unable to initialize new alias"

**Решение:** Проверьте что MinIO запущен:
```bash
docker ps | grep minio
```

Если не запущен:
```bash
docker-compose up -d
```

### CORS все еще не работает

1. **Проверьте CORS конфигурацию:**
   ```bash
   mc anonymous get-json local/aq-media
   ```

2. **Перезапустите MinIO:**
   ```bash
   docker-compose restart minio
   ```

3. **Очистите кэш браузера** (Ctrl+Shift+Delete)

4. **Проверьте origin в запросе:**
   В DevTools → Network → Headers проверьте что `Origin` совпадает с `AllowedOrigins`.

### CORS работает локально, но не в production

Убедитесь что:
- В `AllowedOrigins` указан ваш production домен
- Используется `https://` (не `http://`)
- Нет `www.` несоответствий (`https://example.com` ≠ `https://www.example.com`)

---

## Docker Compose уже настроен

В `docker-compose.yml` MinIO уже настроен для приема CORS:

```yaml
minio:
  image: minio/minio:latest
  environment:
    MINIO_ROOT_USER: minioadmin
    MINIO_ROOT_PASSWORD: minioadmin123
  ports:
    - "9000:9000"   # API
    - "9001:9001"   # Console
  # CORS настраивается через mc, не через environment
```

**ВАЖНО:** CORS настраивается НЕ через environment variables, а через MinIO Client (mc) для конкретного bucket.

---

## Summary

✅ **Для новых инсталляций:** Используйте `init-minio.sh` / `init-minio.ps1` (CORS включен автоматически)

✅ **Для существующих:** Используйте `setup-minio-cors.sh` / `setup-minio-cors.ps1`

✅ **Для production:** Измените `AllowedOrigins` на ваши домены

❌ **Не нужно:** Изменять docker-compose.yml или environment variables

---

**После настройки CORS фронтенд сможет загружать файлы в MinIO без ошибок!** 🎉
