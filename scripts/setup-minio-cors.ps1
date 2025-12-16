# MinIO CORS Setup Script for Windows (PowerShell)
# Настройка CORS для существующего bucket

$ErrorActionPreference = "Stop"

Write-Host "🌐 Настройка CORS для MinIO bucket aq-media..." -ForegroundColor Green

# MinIO credentials
$MINIO_ENDPOINT = "http://localhost:9000"
$MINIO_ROOT_USER = "minioadmin"
$MINIO_ROOT_PASSWORD = "minioadmin123"
$BUCKET_NAME = "aq-media"

# Проверка установки mc
if (-not (Get-Command mc -ErrorAction SilentlyContinue)) {
    Write-Host "❌ MinIO Client (mc) не установлен!" -ForegroundColor Red
    Write-Host "Скачайте mc.exe с https://dl.min.io/client/mc/release/windows-amd64/mc.exe" -ForegroundColor Yellow
    exit 1
}

# Конфигурация mc
Write-Host "🔧 Подключение к MinIO..." -ForegroundColor Cyan
mc alias set local $MINIO_ENDPOINT $MINIO_ROOT_USER $MINIO_ROOT_PASSWORD

# Создание CORS конфигурации
Write-Host "📝 Создание CORS конфигурации..." -ForegroundColor Cyan
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

$corsConfigPath = "$env:TEMP\cors-config.json"
$corsConfig | Out-File -FilePath $corsConfigPath -Encoding UTF8

# Применение CORS
Write-Host "✅ Применение CORS к bucket '$BUCKET_NAME'..." -ForegroundColor Cyan
mc anonymous set-json $corsConfigPath local/$BUCKET_NAME

# Cleanup
Remove-Item -Path $corsConfigPath -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "✅ CORS успешно настроен!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 CORS конфигурация:" -ForegroundColor Yellow
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
Write-Host "Allowed Origins:  http://localhost:4200, http://localhost:8080"
Write-Host "Allowed Methods:  GET, PUT, POST, DELETE, HEAD"
Write-Host "Allowed Headers:  * (все)"
Write-Host "Expose Headers:   ETag, Content-Length, Content-Type"
Write-Host "Max Age:          3600 секунд (1 час)"
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
Write-Host ""
Write-Host "🧪 Проверка:" -ForegroundColor Yellow
Write-Host "   Попробуйте загрузить файл из фронтенда (http://localhost:4200)"
Write-Host "   CORS ошибки должны исчезнуть."
Write-Host ""
