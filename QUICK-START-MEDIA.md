# 🚀 Quick Start: MinIO + imgproxy

Быстрый запуск инфраструктуры для аватаров за 3 минуты.

---

## 1️⃣ Запуск сервисов

```bash
docker-compose up -d
```

Проверка:
```bash
docker-compose ps
```

Должно быть:
- ✅ `aqyldy-minio` - **Up (healthy)**
- ✅ `aqyldy-imgproxy` - **Up (healthy)**
- ✅ `aqyldy-kundelik-backend-db-1` - **Up**

---

## 2️⃣ Установка MinIO Client

### Windows
```powershell
Invoke-WebRequest -Uri "https://dl.min.io/client/mc/release/windows-amd64/mc.exe" -OutFile "mc.exe"
```

### Linux
```bash
wget https://dl.min.io/client/mc/release/linux-amd64/mc
chmod +x mc
sudo mv mc /usr/local/bin/
```

### macOS
```bash
brew install minio/stable/mc
```

---

## 3️⃣ Инициализация MinIO

### Windows (PowerShell)
```powershell
cd scripts
.\init-minio.ps1
```

### Linux / macOS
```bash
cd scripts
chmod +x init-minio.sh
./init-minio.sh
```

---

## ✅ Готово!

Инфраструктура готова к использованию.

### Доступ

| Сервис | URL | Credentials |
|--------|-----|-------------|
| **MinIO Console** | http://localhost:9001 | `minioadmin` / `minioadmin123` |
| **MinIO API** | http://localhost:9000 | - |
| **imgproxy** | http://localhost:8081 | - |

### Учётные данные приложения

```
Access Key:  aqyldy-app
Secret Key:  aqyldy-secret-key-change-in-production
Bucket:      aq-media
```

⚠️ **Измените в production!**

---

## 🧪 Быстрый тест

```bash
# Проверить MinIO
mc ls local/aq-media/

# Загрузить тестовый файл
mc cp test.jpg local/aq-media/avatars/students/

# Проверить imgproxy
curl http://localhost:8081/health
```

---

## 📚 Подробная документация

Смотрите **MINIO-IMGPROXY-SETUP.md** для:
- Детальной настройки
- Генерации ключей безопасности
- Troubleshooting
- Production checklist

---

## 🔑 Генерация новых ключей imgproxy

### Linux / macOS / Git Bash
```bash
echo $(xxd -g 2 -l 64 -p /dev/random | tr -d '\n')
```

### PowerShell
```powershell
-join (1..64 | ForEach-Object { '{0:x2}' -f (Get-Random -Maximum 256) })
```

Обновите в `docker-compose.yml`:
```yaml
IMGPROXY_KEY: "YOUR_NEW_KEY"
IMGPROXY_SALT: "YOUR_NEW_SALT"
```

Затем:
```bash
docker-compose up -d imgproxy
```

---

## ❌ Остановка

```bash
# Остановить
docker-compose stop

# Остановить и удалить контейнеры
docker-compose down

# Удалить всё включая данные
docker-compose down -v
```

---

## 🆘 Помощь

**Проблемы с запуском?**
1. Проверьте логи: `docker-compose logs -f minio imgproxy`
2. Проверьте порты: `netstat -an | findstr "9000 9001 8081"`
3. Смотрите **MINIO-IMGPROXY-SETUP.md** раздел "Troubleshooting"

**Следующие шаги:**
- Интеграция с Spring Boot → см. следующую задачу
- Настройка presigned URLs
- Реализация загрузки аватаров
