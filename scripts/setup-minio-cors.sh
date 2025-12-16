#!/bin/bash

# MinIO CORS Setup Script
# Настройка CORS для существующего bucket

set -e

echo "🌐 Настройка CORS для MinIO bucket aq-media..."

# MinIO credentials
MINIO_ENDPOINT="http://localhost:9000"
MINIO_ROOT_USER="minioadmin"
MINIO_ROOT_PASSWORD="minioadmin123"
BUCKET_NAME="aq-media"

# Проверка установки mc
if ! command -v mc &> /dev/null; then
    echo "❌ MinIO Client (mc) не установлен!"
    echo "Установите mc: https://min.io/docs/minio/linux/reference/minio-mc.html"
    exit 1
fi

# Конфигурация mc
echo "🔧 Подключение к MinIO..."
mc alias set local $MINIO_ENDPOINT $MINIO_ROOT_USER $MINIO_ROOT_PASSWORD

# Создание CORS конфигурации
echo "📝 Создание CORS конфигурации..."
cat > /tmp/cors-config.json << 'EOF'
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
EOF

# Применение CORS
echo "✅ Применение CORS к bucket '$BUCKET_NAME'..."
mc anonymous set-json /tmp/cors-config.json local/$BUCKET_NAME

# Cleanup
rm -f /tmp/cors-config.json

echo ""
echo "✅ CORS успешно настроен!"
echo ""
echo "📋 CORS конфигурация:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Allowed Origins:  http://localhost:4200, http://localhost:8080"
echo "Allowed Methods:  GET, PUT, POST, DELETE, HEAD"
echo "Allowed Headers:  * (все)"
echo "Expose Headers:   ETag, Content-Length, Content-Type"
echo "Max Age:          3600 секунд (1 час)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "🧪 Проверка:"
echo "   Попробуйте загрузить файл из фронтенда (http://localhost:4200)"
echo "   CORS ошибки должны исчезнуть."
echo ""
