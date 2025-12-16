#!/bin/bash

# MinIO Initialization Script
# Создаёт бакет, пользователя и политики для Aqyldy Kundelik

set -e

echo "🚀 Инициализация MinIO для Aqyldy Kundelik..."

# MinIO credentials
MINIO_ENDPOINT="http://localhost:9000"
MINIO_ROOT_USER="minioadmin"
MINIO_ROOT_PASSWORD="minioadmin123"

# Application credentials
APP_ACCESS_KEY="aqyldy-app"
APP_SECRET_KEY="aqyldy-secret-key-change-in-production"

BUCKET_NAME="aq-media"

# Установка mc (MinIO Client) если не установлен
if ! command -v mc &> /dev/null; then
    echo "⬇️  Установка MinIO Client..."
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        wget https://dl.min.io/client/mc/release/linux-amd64/mc
        chmod +x mc
        sudo mv mc /usr/local/bin/
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        brew install minio/stable/mc
    else
        echo "❌ Пожалуйста, установите mc вручную: https://min.io/docs/minio/linux/reference/minio-mc.html"
        exit 1
    fi
fi

# Конфигурация mc
echo "🔧 Настройка MinIO Client..."
mc alias set local $MINIO_ENDPOINT $MINIO_ROOT_USER $MINIO_ROOT_PASSWORD

# Создание бакета
echo "📦 Создание приватного бакета '$BUCKET_NAME'..."
mc mb local/$BUCKET_NAME --ignore-existing

# Установка приватного доступа (block public access)
echo "🔒 Установка приватного доступа..."
mc anonymous set none local/$BUCKET_NAME

# Создание политики для приложения
echo "📝 Создание политики доступа..."
cat > /tmp/aqyldy-app-policy.json << 'EOF'
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
EOF

mc admin policy create local aqyldy-app-policy /tmp/aqyldy-app-policy.json

# Создание пользователя для приложения
echo "👤 Создание пользователя приложения..."
mc admin user add local $APP_ACCESS_KEY $APP_SECRET_KEY

# Привязка политики к пользователю
echo "🔗 Привязка политики к пользователю..."
mc admin policy attach local aqyldy-app-policy --user=$APP_ACCESS_KEY

# Создание структуры папок
echo "📁 Создание структуры папок..."
echo "placeholder" | mc pipe local/$BUCKET_NAME/avatars/.keep
echo "placeholder" | mc pipe local/$BUCKET_NAME/avatars/students/.keep
echo "placeholder" | mc pipe local/$BUCKET_NAME/avatars/staff/.keep

# Настройка CORS для фронтенда
echo "🌐 Настройка CORS..."
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

mc anonymous set-json /tmp/cors-config.json local/$BUCKET_NAME

# Cleanup CORS config
rm -f /tmp/cors-config.json

# Вывод информации
echo ""
echo "✅ MinIO успешно инициализирован!"
echo ""
echo "📋 Информация для подключения:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "MinIO Endpoint:     $MINIO_ENDPOINT"
echo "MinIO Console UI:   http://localhost:9001"
echo ""
echo "Bucket Name:        $BUCKET_NAME"
echo "Bucket Access:      Private (No public access)"
echo ""
echo "Application Credentials:"
echo "  Access Key:       $APP_ACCESS_KEY"
echo "  Secret Key:       $APP_SECRET_KEY"
echo ""
echo "⚠️  ВАЖНО: Измените эти учётные данные в production!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "🔍 Проверка:"
echo "  mc ls local/$BUCKET_NAME"
echo ""
echo "🌐 MinIO Console: http://localhost:9001"
echo "   Login: $MINIO_ROOT_USER / $MINIO_ROOT_PASSWORD"
echo ""

# Cleanup
rm -f /tmp/aqyldy-app-policy.json
