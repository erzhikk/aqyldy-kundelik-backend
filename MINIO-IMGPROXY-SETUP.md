# MinIO + imgproxy Setup Guide

Инфраструктура для загрузки и обработки аватаров студентов и сотрудников.

---

## 📦 Что включено

### MinIO
- **Object Storage** для хранения изображений
- Приватный бакет `aq-media`
- S3-совместимый API
- Web Console для управления

### imgproxy
- Обработка изображений на лету (resize, crop, format conversion)
- Подписанные URL для безопасности
- Интеграция с MinIO
- Автоматическая оптимизация

---

## 🚀 Быстрый старт

### 1. Запуск сервисов

```bash
# Запустить все сервисы (PostgreSQL, MinIO, imgproxy)
docker-compose up -d

# Проверить статус
docker-compose ps
```

**Ожидаемый результат:**
```
NAME                IMAGE                        STATUS
aqyldy-kundelik-backend-db-1   postgres:16        Up
aqyldy-minio       minio/minio:latest           Up (healthy)
aqyldy-imgproxy    darthsim/imgproxy:latest     Up (healthy)
```

### 2. Установка MinIO Client (mc)

#### Windows (PowerShell)
```powershell
# Скачать mc.exe
Invoke-WebRequest -Uri "https://dl.min.io/client/mc/release/windows-amd64/mc.exe" -OutFile "mc.exe"

# Переместить в PATH (опционально)
Move-Item mc.exe C:\Windows\System32\mc.exe
```

#### Linux / macOS
```bash
# Linux
wget https://dl.min.io/client/mc/release/linux-amd64/mc
chmod +x mc
sudo mv mc /usr/local/bin/

# macOS (Homebrew)
brew install minio/stable/mc
```

### 3. Инициализация MinIO

#### Windows (PowerShell)
```powershell
cd scripts
.\init-minio.ps1
```

#### Linux / macOS
```bash
cd scripts
chmod +x init-minio.sh
./init-minio.sh
```

**Что делает скрипт:**
1. ✅ Создаёт приватный бакет `aq-media`
2. ✅ Создаёт пользователя приложения `aqyldy-app`
3. ✅ Настраивает политику доступа
4. ✅ Создаёт структуру папок:
   ```
   aq-media/
   └── avatars/
       ├── students/
       └── staff/
   ```

---

## 🔑 Учётные данные

### MinIO Root (Администратор)
```
Endpoint:  http://localhost:9000
Console:   http://localhost:9001
Username:  minioadmin
Password:  minioadmin123
```

### MinIO Application User
```
Access Key:  aqyldy-app
Secret Key:  aqyldy-secret-key-change-in-production
```

### imgproxy Keys
```
IMGPROXY_KEY:  943b421c9eb07c830af81030552c86009268de4e532ba2ee2eab8247c6da0881
IMGPROXY_SALT: 520f986b998545b4785e0defbc4f3c1203f22de2374a3d53cb7a7fe9fea309c5
```

⚠️ **ВАЖНО:** В production обязательно измените все ключи!

---

## 🔧 Генерация новых ключей imgproxy

### Для Linux / macOS / Git Bash
```bash
echo $(xxd -g 2 -l 64 -p /dev/random | tr -d '\n')
```

### Для PowerShell
```powershell
-join (1..64 | ForEach-Object { '{0:x2}' -f (Get-Random -Maximum 256) })
```

### Онлайн генератор
```bash
# Использовать OpenSSL
openssl rand -hex 64
```

После генерации обновите переменные в `docker-compose.yml`:
```yaml
IMGPROXY_KEY: "YOUR_NEW_KEY"
IMGPROXY_SALT: "YOUR_NEW_SALT"
```

---

## 🌐 Доступ к сервисам

| Сервис | URL | Описание |
|--------|-----|----------|
| **MinIO API** | http://localhost:9000 | S3-совместимый API |
| **MinIO Console** | http://localhost:9001 | Web UI для управления |
| **imgproxy** | http://localhost:8081 | Image processing API |
| **PostgreSQL** | localhost:5432 | База данных |

---

## 📝 Ручная настройка (альтернатива скрипту)

### Шаг 1: Настройка alias
```bash
mc alias set local http://localhost:9000 minioadmin minioadmin123
```

### Шаг 2: Создание бакета
```bash
mc mb local/aq-media
mc anonymous set none local/aq-media
```

### Шаг 3: Создание политики

Создайте файл `aqyldy-app-policy.json`:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": [
        "arn:aws:s3:::aq-media/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::aq-media"
      ]
    }
  ]
}
```

Примените политику:
```bash
mc admin policy create local aqyldy-app-policy aqyldy-app-policy.json
```

### Шаг 4: Создание пользователя
```bash
mc admin user add local aqyldy-app aqyldy-secret-key-change-in-production
mc admin policy attach local aqyldy-app-policy --user=aqyldy-app
```

### Шаг 5: Создание структуры папок
```bash
echo "placeholder" | mc pipe local/aq-media/avatars/.keep
echo "placeholder" | mc pipe local/aq-media/avatars/students/.keep
echo "placeholder" | mc pipe local/aq-media/avatars/staff/.keep
```

---

## 🧪 Тестирование

### Проверка MinIO

```bash
# Проверить список бакетов
mc ls local/

# Проверить содержимое бакета
mc ls local/aq-media/

# Загрузить тестовый файл
mc cp test-image.jpg local/aq-media/avatars/students/

# Скачать файл
mc cp local/aq-media/avatars/students/test-image.jpg ./downloaded.jpg

# Удалить файл
mc rm local/aq-media/avatars/students/test-image.jpg
```

### Проверка imgproxy

```bash
# Health check
curl http://localhost:8081/health

# Проверить подключение к MinIO (нужно загрузить тестовое изображение сначала)
# Формат URL: /insecure/rs:fill:300:300/plain/s3://aq-media/avatars/students/test.jpg
curl -I http://localhost:8081/insecure/rs:fill:300:300/plain/s3://aq-media/avatars/students/test.jpg
```

**Примечание:** В production вместо `/insecure/` используйте подписанные URL!

---

## 🔒 Безопасность

### Требования к изображениям

В `docker-compose.yml` настроены следующие ограничения:

```yaml
IMGPROXY_MAX_SRC_FILE_SIZE: "5242880"  # 5 MB максимум
IMGPROXY_MAX_SRC_RESOLUTION: "16000000"  # 4000x4000 пикселей максимум
IMGPROXY_ALLOWED_SOURCES: "s3://aq-media/"  # Только наш бакет
```

### Минимальные требования (проверяются в приложении)
- **Минимальное разрешение:** 256×256
- **Максимальное разрешение:** 4000×4000
- **Разрешённые форматы:** JPEG, PNG, WebP
- **Максимальный размер:** 5 MB
- **Только статические изображения** (без анимации)

### Приватный доступ

✅ Бакет настроен как **приватный**
✅ Публичный доступ **заблокирован**
✅ Доступ только через **presigned URLs**

---

## 📊 Мониторинг

### Логи MinIO
```bash
docker logs aqyldy-minio -f
```

### Логи imgproxy
```bash
docker logs aqyldy-imgproxy -f
```

### Статистика бакета
```bash
mc admin info local
mc du local/aq-media/
```

---

## 🔄 Обновление и перезапуск

### Перезапуск сервисов
```bash
# Перезапустить MinIO
docker-compose restart minio

# Перезапустить imgproxy
docker-compose restart imgproxy

# Перезапустить всё
docker-compose restart
```

### Обновление образов
```bash
# Скачать новые версии
docker-compose pull

# Пересоздать контейнеры
docker-compose up -d
```

---

## 🗑️ Очистка

### Удалить все файлы из бакета
```bash
mc rm --recursive --force local/aq-media/avatars/
```

### Удалить бакет
```bash
mc rb --force local/aq-media
```

### Остановить и удалить контейнеры
```bash
docker-compose down

# С удалением volumes (данные будут потеряны!)
docker-compose down -v
```

---

## 🐛 Troubleshooting

### MinIO не запускается

```bash
# Проверить логи
docker logs aqyldy-minio

# Проверить порты
netstat -an | findstr 9000
netstat -an | findstr 9001

# Пересоздать контейнер
docker-compose up -d --force-recreate minio
```

### imgproxy не может подключиться к MinIO

```bash
# Проверить network
docker network inspect aqyldy-kundelik-backend_aqyldy-network

# Проверить переменные окружения
docker exec aqyldy-imgproxy env | grep MINIO
docker exec aqyldy-imgproxy env | grep S3

# Тест подключения
docker exec aqyldy-imgproxy wget -O- http://minio:9000/minio/health/live
```

### Ошибка "mc: command not found"

Установите MinIO Client (см. раздел "Установка MinIO Client")

### Ошибка "Access Denied"

```bash
# Проверить политику пользователя
mc admin user info local aqyldy-app

# Переприкрепить политику
mc admin policy attach local aqyldy-app-policy --user=aqyldy-app
```

---

## 📚 Дополнительные ресурсы

- [MinIO Documentation](https://min.io/docs/minio/linux/index.html)
- [imgproxy Documentation](https://docs.imgproxy.net/)
- [MinIO Client Guide](https://min.io/docs/minio/linux/reference/minio-mc.html)
- [imgproxy Signing URLs](https://docs.imgproxy.net/signing_the_url)

---

## ✅ Checklist перед production

- [ ] Изменить MinIO root credentials
- [ ] Изменить application credentials
- [ ] Сгенерировать новые IMGPROXY_KEY и IMGPROXY_SALT
- [ ] Настроить HTTPS для MinIO
- [ ] Настроить HTTPS для imgproxy
- [ ] Настроить backup бакета
- [ ] Настроить мониторинг
- [ ] Настроить лимиты ресурсов в docker-compose
- [ ] Включить логирование в файлы
- [ ] Настроить retention policy для старых файлов
