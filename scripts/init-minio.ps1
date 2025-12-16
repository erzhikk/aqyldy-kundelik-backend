# MinIO Initialization Script for Windows (PowerShell)
# Создаёт бакет, пользователя и политики для Aqyldy Kundelik

$ErrorActionPreference = "Stop"

Write-Host "🚀 Инициализация MinIO для Aqyldy Kundelik..." -ForegroundColor Green

# MinIO credentials
$MINIO_ENDPOINT = "http://localhost:9000"
$MINIO_ROOT_USER = "minioadmin"
$MINIO_ROOT_PASSWORD = "minioadmin123"

# Application credentials
$APP_ACCESS_KEY = "aqyldy-app"
$APP_SECRET_KEY = "aqyldy-secret-key-change-in-production"

$BUCKET_NAME = "aq-media"

# Проверка установки mc
if (-not (Get-Command mc -ErrorAction SilentlyContinue)) {
    Write-Host "⬇️  Установка MinIO Client..." -ForegroundColor Yellow
    Write-Host "Пожалуйста, скачайте mc.exe с https://dl.min.io/client/mc/release/windows-amd64/mc.exe" -ForegroundColor Red
    Write-Host "И поместите в PATH или в текущую директорию" -ForegroundColor Red
    exit 1
}

# Конфигурация mc
Write-Host "🔧 Настройка MinIO Client..." -ForegroundColor Cyan
mc alias set local $MINIO_ENDPOINT $MINIO_ROOT_USER $MINIO_ROOT_PASSWORD

# Создание бакета
Write-Host "📦 Создание приватного бакета '$BUCKET_NAME'..." -ForegroundColor Cyan
mc mb local/$BUCKET_NAME --ignore-existing

# Установка приватного доступа
Write-Host "🔒 Установка приватного доступа..." -ForegroundColor Cyan
mc anonymous set none local/$BUCKET_NAME

# Создание политики для приложения
Write-Host "📝 Создание политики доступа..." -ForegroundColor Cyan
$policyJson = @"
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
"@

$policyJson | Out-File -FilePath "$env:TEMP\aqyldy-app-policy.json" -Encoding UTF8
mc admin policy create local aqyldy-app-policy "$env:TEMP\aqyldy-app-policy.json"

# Создание пользователя для приложения
Write-Host "👤 Создание пользователя приложения..." -ForegroundColor Cyan
mc admin user add local $APP_ACCESS_KEY $APP_SECRET_KEY

# Привязка политики к пользователю
Write-Host "🔗 Привязка политики к пользователю..." -ForegroundColor Cyan
mc admin policy attach local aqyldy-app-policy --user=$APP_ACCESS_KEY

# Создание структуры папок
Write-Host "📁 Создание структуры папок..." -ForegroundColor Cyan
"placeholder" | mc pipe local/$BUCKET_NAME/avatars/.keep
"placeholder" | mc pipe local/$BUCKET_NAME/avatars/students/.keep
"placeholder" | mc pipe local/$BUCKET_NAME/avatars/staff/.keep

# Настройка CORS для фронтенда
Write-Host "🌐 Настройка CORS..." -ForegroundColor Cyan
$corsConfig = @"
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
"@

$corsConfig | Out-File -FilePath "$env:TEMP\cors-config.json" -Encoding UTF8
mc anonymous set-json "$env:TEMP\cors-config.json" local/$BUCKET_NAME

# Cleanup CORS config
Remove-Item -Path "$env:TEMP\cors-config.json" -ErrorAction SilentlyContinue

# Вывод информации
Write-Host ""
Write-Host "✅ MinIO успешно инициализирован!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Информация для подключения:" -ForegroundColor Yellow
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
Write-Host "MinIO Endpoint:     $MINIO_ENDPOINT"
Write-Host "MinIO Console UI:   http://localhost:9001"
Write-Host ""
Write-Host "Bucket Name:        $BUCKET_NAME"
Write-Host "Bucket Access:      Private (No public access)"
Write-Host ""
Write-Host "Application Credentials:"
Write-Host "  Access Key:       $APP_ACCESS_KEY"
Write-Host "  Secret Key:       $APP_SECRET_KEY"
Write-Host ""
Write-Host "⚠️  ВАЖНО: Измените эти учётные данные в production!" -ForegroundColor Red
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
Write-Host ""
Write-Host "🔍 Проверка:"
Write-Host "  mc ls local/$BUCKET_NAME"
Write-Host ""
Write-Host "🌐 MinIO Console: http://localhost:9001"
Write-Host "   Login: $MINIO_ROOT_USER / $MINIO_ROOT_PASSWORD"
Write-Host ""

# Cleanup
Remove-Item -Path "$env:TEMP\aqyldy-app-policy.json" -ErrorAction SilentlyContinue
