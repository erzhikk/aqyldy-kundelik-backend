# 📋 Задачи для фронтенда (Angular)

## 🎯 Контекст

Backend уже реализовал следующее:
1. ✅ MinIO + imgproxy запущены в Docker Compose
2. ✅ POST `/api/media/presign/photo` - получение presigned URL для загрузки
3. ✅ POST `/api/media/reconcile?key=...` - валидация загруженного изображения
4. ✅ База данных `media_object` с полями: id, user_id, s3_key, width, height, sha256, status

## 📦 Доступные API эндпоинты

### 1. Получение presigned URL

**Эндпоинт:** `POST /api/media/presign/photo`

**Headers:**
```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request Body:**
```json
{
  "userId": "74700097-17b2-409c-84d1-087ccfa7561c",
  "contentType": "image/jpeg",
  "filename": "avatar.jpg"
}
```

**Response:**
```json
{
  "url": "http://localhost:9000/aq-media/users/.../photos/uuid.jpg?X-Amz-...",
  "key": "users/74700097-17b2-409c-84d1-087ccfa7561c/photos/uuid.jpg",
  "fields": {},
  "mediaObjectId": "1b5f491f-89ea-41e5-9b00-267dcc0e0df5"
}
```

### 2. Загрузка файла в MinIO

**Метод:** `PUT {presigned_url}`

**Headers:**
```
Content-Type: {тот же contentType что в presign запросе}
```

**Body:** бинарные данные файла

### 3. Валидация загруженного файла

**Эндпоинт:** `POST /api/media/reconcile?key={s3_key}`

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Response:**
```json
{
  "success": true,
  "key": "users/.../photos/uuid.jpg",
  "width": 300,
  "height": 300,
  "fileSize": 15423,
  "sha256": "a1b2c3...",
  "reason": null
}
```

### 4. Получение imgproxy URL для отображения

**Формат:** `http://localhost:8081/{signature}/{processing_options}/{encoded_source_url}`

Детали в Задаче 6.

---

# 📝 Задача 5: Angular компонент UploadAvatar

## Описание

Создать переиспользуемый Angular компонент для загрузки аватара пользователя с полным циклом:
1. Выбор файла через `<input type="file">`
2. Клиентская валидация (размер, тип, разрешение)
3. Предпросмотр выбранного изображения
4. Получение presigned URL
5. Загрузка в MinIO
6. Вызов валидации (reconcile)
7. Отображение статуса загрузки

## Требования

### 1. Клиентская валидация

**До загрузки проверить:**
- ✅ Тип файла: только `image/jpeg`, `image/png`, `image/webp`
- ✅ Размер файла: максимум 5 MB
- ✅ Разрешение изображения: минимум 256×256, максимум 4000×4000
- ✅ Формат: только статические изображения (не анимированные GIF)

**Показывать понятные ошибки:**
```
❌ "Файл слишком большой. Максимум 5 МБ"
❌ "Неподдерживаемый формат. Используйте JPEG, PNG или WebP"
❌ "Изображение слишком маленькое. Минимум 256×256 пикселей"
❌ "Изображение слишком большое. Максимум 4000×4000 пикселей"
```

### 2. Структура компонента

**Файл:** `src/app/shared/components/upload-avatar/upload-avatar.component.ts`

**Inputs:**
```typescript
@Input() userId!: string;              // ID пользователя
@Input() currentAvatarKey?: string;    // Текущий ключ аватара (для preview)
@Input() size: 'small' | 'medium' | 'large' = 'medium';  // Размер preview
```

**Outputs:**
```typescript
@Output() uploadSuccess = new EventEmitter<string>();  // Испускает s3_key
@Output() uploadError = new EventEmitter<string>();    // Испускает текст ошибки
```

**State:**
```typescript
interface UploadState {
  file: File | null;
  previewUrl: string | null;
  uploading: boolean;
  progress: number;
  status: 'idle' | 'validating' | 'uploading' | 'reconciling' | 'success' | 'error';
  errorMessage: string | null;
  uploadedKey: string | null;
}
```

### 3. UI компонента

```html
<div class="upload-avatar">
  <!-- Превью аватара -->
  <div class="avatar-preview" [class.size-{{ size }}]>
    <img
      *ngIf="state.previewUrl || currentAvatarKey"
      [src]="state.previewUrl || getImgproxyUrl(currentAvatarKey!)"
      alt="Avatar"
    />
    <div *ngIf="!state.previewUrl && !currentAvatarKey" class="avatar-placeholder">
      <mat-icon>person</mat-icon>
    </div>

    <!-- Оверлей при загрузке -->
    <div *ngIf="state.uploading" class="upload-overlay">
      <mat-spinner diameter="40"></mat-spinner>
      <span>{{ state.progress }}%</span>
    </div>
  </div>

  <!-- Input для выбора файла -->
  <input
    #fileInput
    type="file"
    accept="image/jpeg,image/png,image/webp"
    (change)="onFileSelected($event)"
    style="display: none"
  />

  <!-- Кнопки -->
  <div class="actions">
    <button
      mat-raised-button
      color="primary"
      (click)="fileInput.click()"
      [disabled]="state.uploading"
    >
      Выбрать фото
    </button>

    <button
      *ngIf="state.file"
      mat-raised-button
      color="accent"
      (click)="upload()"
      [disabled]="state.uploading"
    >
      Загрузить
    </button>
  </div>

  <!-- Статусы -->
  <div class="status-message">
    <mat-progress-bar
      *ngIf="state.uploading"
      mode="determinate"
      [value]="state.progress"
    ></mat-progress-bar>

    <p *ngIf="state.status === 'validating'" class="info">
      Проверка изображения...
    </p>

    <p *ngIf="state.status === 'uploading'" class="info">
      Загрузка {{ state.progress }}%
    </p>

    <p *ngIf="state.status === 'reconciling'" class="info">
      Валидация изображения...
    </p>

    <p *ngIf="state.status === 'success'" class="success">
      ✓ Изображение успешно загружено
    </p>

    <p *ngIf="state.status === 'error'" class="error">
      ✗ {{ state.errorMessage }}
    </p>
  </div>
</div>
```

### 4. Логика загрузки

**Метод `onFileSelected(event)`:**
```typescript
async onFileSelected(event: Event) {
  const input = event.target as HTMLInputElement;
  if (!input.files || input.files.length === 0) return;

  const file = input.files[0];

  // 1. Валидация типа
  if (!this.isValidFileType(file)) {
    this.showError('Неподдерживаемый формат. Используйте JPEG, PNG или WebP');
    return;
  }

  // 2. Валидация размера файла
  if (file.size > 5 * 1024 * 1024) {
    this.showError('Файл слишком большой. Максимум 5 МБ');
    return;
  }

  // 3. Проверка разрешения изображения
  const dimensions = await this.getImageDimensions(file);
  if (dimensions.width < 256 || dimensions.height < 256) {
    this.showError('Изображение слишком маленькое. Минимум 256×256 пикселей');
    return;
  }
  if (dimensions.width > 4000 || dimensions.height > 4000) {
    this.showError('Изображение слишком большое. Максимум 4000×4000 пикселей');
    return;
  }

  // 4. Создать preview
  this.state.file = file;
  this.state.previewUrl = await this.createPreviewUrl(file);
  this.state.status = 'idle';
}
```

**Метод `upload()`:**
```typescript
async upload() {
  if (!this.state.file) return;

  try {
    this.state.uploading = true;
    this.state.status = 'uploading';
    this.state.progress = 0;
    this.state.errorMessage = null;

    // 1. Получить presigned URL
    const presignResponse = await this.mediaService.getPresignedUrl({
      userId: this.userId,
      contentType: this.state.file.type,
      filename: this.state.file.name
    });

    // 2. Загрузить файл в MinIO
    this.state.progress = 25;
    await this.uploadToMinio(
      presignResponse.url,
      this.state.file,
      this.state.file.type
    );

    // 3. Вызвать reconcile для валидации
    this.state.status = 'reconciling';
    this.state.progress = 75;

    const reconcileResult = await this.mediaService.reconcile(presignResponse.key);

    if (!reconcileResult.success) {
      throw new Error(reconcileResult.reason || 'Ошибка валидации изображения');
    }

    // 4. Успех
    this.state.status = 'success';
    this.state.progress = 100;
    this.state.uploadedKey = presignResponse.key;

    this.uploadSuccess.emit(presignResponse.key);

  } catch (error) {
    this.state.status = 'error';
    this.state.errorMessage = error.message || 'Ошибка загрузки';
    this.uploadError.emit(this.state.errorMessage);
  } finally {
    this.state.uploading = false;
  }
}
```

**Вспомогательные методы:**
```typescript
private getImageDimensions(file: File): Promise<{width: number, height: number}> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    const url = URL.createObjectURL(file);

    img.onload = () => {
      URL.revokeObjectURL(url);
      resolve({ width: img.width, height: img.height });
    };

    img.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error('Не удалось загрузить изображение'));
    };

    img.src = url;
  });
}

private createPreviewUrl(file: File): Promise<string> {
  return new Promise((resolve) => {
    const reader = new FileReader();
    reader.onload = (e) => resolve(e.target?.result as string);
    reader.readAsDataURL(file);
  });
}

private async uploadToMinio(url: string, file: File, contentType: string): Promise<void> {
  const response = await fetch(url, {
    method: 'PUT',
    headers: {
      'Content-Type': contentType
    },
    body: file
  });

  if (!response.ok) {
    throw new Error(`Ошибка загрузки в MinIO: ${response.statusText}`);
  }
}
```

### 5. MediaService

**Файл:** `src/app/core/services/media.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, firstValueFrom } from 'rxjs';
import { environment } from '@env/environment';

interface PresignRequest {
  userId: string;
  contentType: string;
  filename: string;
}

interface PresignResponse {
  url: string;
  key: string;
  fields: Record<string, string>;
  mediaObjectId: string;
}

interface ReconcileResponse {
  success: boolean;
  key: string;
  width?: number;
  height?: number;
  fileSize?: number;
  sha256?: string;
  reason?: string;
}

@Injectable({ providedIn: 'root' })
export class MediaService {
  private readonly apiUrl = `${environment.apiUrl}/api/media`;

  constructor(private http: HttpClient) {}

  async getPresignedUrl(request: PresignRequest): Promise<PresignResponse> {
    return firstValueFrom(
      this.http.post<PresignResponse>(`${this.apiUrl}/presign/photo`, request)
    );
  }

  async reconcile(key: string): Promise<ReconcileResponse> {
    return firstValueFrom(
      this.http.post<ReconcileResponse>(`${this.apiUrl}/reconcile`, null, {
        params: { key }
      })
    );
  }
}
```

### 6. Интеграция в формы

**В форме создания/редактирования студента:**

```html
<form [formGroup]="studentForm" (ngSubmit)="onSubmit()">
  <!-- Существующие поля формы -->
  <mat-form-field>
    <input matInput formControlName="fullName" placeholder="ФИО" />
  </mat-form-field>

  <mat-form-field>
    <input matInput formControlName="email" placeholder="Email" />
  </mat-form-field>

  <!-- Компонент загрузки аватара -->
  <app-upload-avatar
    [userId]="studentForm.get('id')?.value"
    [currentAvatarKey]="studentForm.get('photoKey')?.value"
    (uploadSuccess)="onAvatarUploaded($event)"
    (uploadError)="onAvatarError($event)"
  ></app-upload-avatar>

  <button mat-raised-button color="primary" type="submit">
    Сохранить
  </button>
</form>
```

**В компоненте:**
```typescript
onAvatarUploaded(s3Key: string) {
  this.studentForm.patchValue({ photoKey: s3Key });
  this.snackBar.open('Аватар успешно загружен', 'OK', { duration: 3000 });
}

onAvatarError(error: string) {
  this.snackBar.open(`Ошибка: ${error}`, 'OK', { duration: 5000 });
}
```

### 7. Стили

```scss
.upload-avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 16px;

  .avatar-preview {
    position: relative;
    border-radius: 50%;
    overflow: hidden;
    border: 2px solid #e0e0e0;

    &.size-small { width: 64px; height: 64px; }
    &.size-medium { width: 128px; height: 128px; }
    &.size-large { width: 256px; height: 256px; }

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .avatar-placeholder {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #f5f5f5;
      color: #9e9e9e;

      mat-icon {
        font-size: 48px;
        width: 48px;
        height: 48px;
      }
    }

    .upload-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.6);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      color: white;
      gap: 8px;
    }
  }

  .actions {
    display: flex;
    gap: 8px;
  }

  .status-message {
    width: 100%;
    text-align: center;

    .info { color: #2196f3; }
    .success { color: #4caf50; }
    .error { color: #f44336; }
  }
}
```

---

# 📝 Задача 6: Отдача картинок через imgproxy

## Описание

Создать утилиту для генерации подписанных imgproxy URLs и интегрировать в компоненты для отображения аватаров.

## Требования

### 1. ImgproxyService

**Файл:** `src/app/core/services/imgproxy.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { environment } from '@env/environment';
import * as CryptoJS from 'crypto-js';

interface ImgproxyOptions {
  width?: number;
  height?: number;
  fit?: 'fill' | 'cover' | 'contain';
  format?: 'webp' | 'jpeg' | 'png';
  quality?: number;
}

@Injectable({ providedIn: 'root' })
export class ImgproxyService {
  private readonly baseUrl = environment.imgproxyUrl; // 'http://localhost:8081'
  private readonly key = environment.imgproxyKey;     // из docker-compose
  private readonly salt = environment.imgproxySalt;   // из docker-compose

  /**
   * Генерирует подписанный imgproxy URL
   *
   * @param s3Key Ключ объекта в S3 (например, 'users/xxx/photos/yyy.jpg')
   * @param options Опции обработки изображения
   * @returns Подписанный imgproxy URL
   *
   * @example
   * const url = imgproxyService.signedUrl(
   *   'users/123/photos/avatar.jpg',
   *   { width: 256, height: 256, fit: 'cover', format: 'webp', quality: 80 }
   * );
   * // Результат: http://localhost:8081/{signature}/rs:fill:256:256/q:80/plain/http://minio:9000/aq-media/users/123/photos/avatar.jpg@webp
   */
  signedUrl(s3Key: string, options: ImgproxyOptions = {}): string {
    // Построить URL источника
    const sourceUrl = `http://minio:9000/aq-media/${s3Key}`;

    // Построить processing options
    const processingOptions = this.buildProcessingOptions(options);

    // Закодировать source URL в base64url
    const encodedUrl = this.base64UrlEncode(sourceUrl);

    // Построить path
    const path = `/${processingOptions}/plain/${encodedUrl}`;

    // Добавить формат если указан
    const fullPath = options.format ? `${path}@${options.format}` : path;

    // Вычислить signature
    const signature = this.sign(fullPath);

    // Собрать полный URL
    return `${this.baseUrl}/${signature}${fullPath}`;
  }

  /**
   * Построить строку processing options
   */
  private buildProcessingOptions(options: ImgproxyOptions): string {
    const parts: string[] = [];

    // Resize
    if (options.width || options.height) {
      const fit = options.fit || 'fill';
      const w = options.width || 0;
      const h = options.height || 0;
      parts.push(`rs:${fit}:${w}:${h}`);
    }

    // Quality
    if (options.quality) {
      parts.push(`q:${options.quality}`);
    }

    return parts.join('/');
  }

  /**
   * Base64 URL encode (RFC 4648)
   */
  private base64UrlEncode(str: string): string {
    const base64 = btoa(str);
    return base64
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=/g, '');
  }

  /**
   * Вычислить HMAC signature для imgproxy
   */
  private sign(path: string): string {
    const keyBin = CryptoJS.enc.Hex.parse(this.key);
    const saltBin = CryptoJS.enc.Hex.parse(this.salt);

    // HMAC-SHA256
    const hmac = CryptoJS.HmacSHA256(saltBin.concat(CryptoJS.enc.Utf8.parse(path)), keyBin);

    // Конвертировать в base64url
    const base64 = hmac.toString(CryptoJS.enc.Base64);
    return base64
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=/g, '');
  }
}
```

**ВАЖНО:** Установить crypto-js:
```bash
npm install crypto-js
npm install --save-dev @types/crypto-js
```

### 2. Environment конфигурация

**Файл:** `src/environments/environment.ts`

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  imgproxyUrl: 'http://localhost:8081',
  imgproxyKey: '943b421c9eb07c830af81030552c86009268de4e532ba2ee2eab8247c6da0881',
  imgproxySalt: '520f986b998545b4785e0defbc4f3c1203f22de2374a3d53cb7a7fe9fea309c5',
  minioUrl: 'http://localhost:9000'
};
```

**⚠️ ВАЖНО:** Ключи выше - это примеры из docker-compose.yml. В production используйте другие ключи!

### 3. Использование в компонентах

**В карточке студента:**

```typescript
import { Component, Input } from '@angular/core';
import { ImgproxyService } from '@core/services/imgproxy.service';

@Component({
  selector: 'app-student-card',
  template: `
    <mat-card class="student-card">
      <mat-card-header>
        <img
          mat-card-avatar
          [src]="getAvatarUrl()"
          [alt]="student.fullName"
          (error)="onImageError($event)"
        />
        <mat-card-title>{{ student.fullName }}</mat-card-title>
        <mat-card-subtitle>{{ student.email }}</mat-card-subtitle>
      </mat-card-header>

      <mat-card-content>
        <!-- Остальной контент карточки -->
      </mat-card-content>
    </mat-card>
  `
})
export class StudentCardComponent {
  @Input() student!: Student;

  private fallbackImage = '/assets/images/default-avatar.png';

  constructor(private imgproxy: ImgproxyService) {}

  getAvatarUrl(): string {
    if (!this.student.photoKey) {
      return this.fallbackImage;
    }

    return this.imgproxy.signedUrl(this.student.photoKey, {
      width: 256,
      height: 256,
      fit: 'cover',
      format: 'webp',
      quality: 80
    });
  }

  onImageError(event: Event) {
    (event.target as HTMLImageElement).src = this.fallbackImage;
  }
}
```

### 4. Pipe для удобства

**Файл:** `src/app/shared/pipes/imgproxy.pipe.ts`

```typescript
import { Pipe, PipeTransform } from '@angular/core';
import { ImgproxyService, ImgproxyOptions } from '@core/services/imgproxy.service';

@Pipe({
  name: 'imgproxy',
  standalone: true
})
export class ImgproxyPipe implements PipeTransform {
  constructor(private imgproxy: ImgproxyService) {}

  transform(s3Key: string | null | undefined, options?: ImgproxyOptions): string {
    if (!s3Key) {
      return '/assets/images/default-avatar.png';
    }

    return this.imgproxy.signedUrl(s3Key, options || {});
  }
}
```

**Использование в шаблонах:**

```html
<img
  [src]="student.photoKey | imgproxy: { width: 256, height: 256, fit: 'cover', format: 'webp' }"
  [alt]="student.fullName"
/>
```

### 5. Preset размеры

**Добавить в ImgproxyService:**

```typescript
export class ImgproxyService {
  // ... существующий код ...

  /**
   * Предустановленные размеры для разных случаев
   */
  readonly presets = {
    thumbnail: { width: 100, height: 100, fit: 'cover' as const, format: 'webp' as const, quality: 80 },
    avatar: { width: 256, height: 256, fit: 'cover' as const, format: 'webp' as const, quality: 80 },
    card: { width: 400, height: 400, fit: 'cover' as const, format: 'webp' as const, quality: 85 },
    large: { width: 800, height: 800, fit: 'contain' as const, format: 'webp' as const, quality: 90 }
  };

  /**
   * Получить URL с пресетом
   */
  presetUrl(s3Key: string, preset: keyof typeof this.presets): string {
    return this.signedUrl(s3Key, this.presets[preset]);
  }
}
```

**Использование:**

```typescript
// В компоненте
getThumbnailUrl() {
  return this.imgproxy.presetUrl(this.student.photoKey, 'thumbnail');
}

getAvatarUrl() {
  return this.imgproxy.presetUrl(this.student.photoKey, 'avatar');
}
```

### 6. Директива для lazy loading

**Файл:** `src/app/shared/directives/lazy-image.directive.ts`

```typescript
import { Directive, ElementRef, Input, OnInit } from '@angular/core';

@Directive({
  selector: 'img[appLazyImage]',
  standalone: true
})
export class LazyImageDirective implements OnInit {
  @Input() appLazyImage!: string;
  @Input() fallback: string = '/assets/images/default-avatar.png';

  constructor(private el: ElementRef<HTMLImageElement>) {}

  ngOnInit() {
    const img = this.el.nativeElement;

    // Установить fallback пока грузится
    img.src = this.fallback;

    // IntersectionObserver для lazy loading
    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          img.src = this.appLazyImage;
          observer.disconnect();
        }
      });
    });

    observer.observe(img);

    // Error handling
    img.onerror = () => {
      img.src = this.fallback;
    };
  }
}
```

**Использование:**

```html
<img
  [appLazyImage]="student.photoKey | imgproxy: { width: 256, height: 256 }"
  [fallback]="'/assets/images/default-avatar.png'"
  [alt]="student.fullName"
/>
```

---

## 📋 Checklist

### Задача 5: UploadAvatar
- [ ] Создан компонент `UploadAvatarComponent`
- [ ] Реализована клиентская валидация (размер, тип, разрешение)
- [ ] Добавлен предпросмотр изображения
- [ ] Интегрирован `MediaService` для API вызовов
- [ ] Реализован полный цикл: presign → upload → reconcile
- [ ] Добавлен прогресс-бар и статусы
- [ ] Обработка ошибок с понятными сообщениями
- [ ] Компонент вставлен в формы студента/сотрудника
- [ ] Написаны стили
- [ ] Добавлен fallback для аватара

### Задача 6: Imgproxy
- [ ] Создан `ImgproxyService`
- [ ] Реализована генерация подписанных URL
- [ ] Установлен crypto-js
- [ ] Добавлены ключи в environment
- [ ] Создан pipe `ImgproxyPipe`
- [ ] Добавлены preset размеры
- [ ] Интегрировано в карточки студентов
- [ ] Добавлена директива lazy loading
- [ ] Обработка ошибок загрузки изображений

---

## 🧪 Тестирование

### Как протестировать Задачу 5:

1. Открыть форму создания студента
2. Выбрать изображение через компонент
3. Проверить что показывается preview
4. Нажать "Загрузить"
5. Убедиться что:
   - Показывается прогресс
   - Статус меняется: uploading → reconciling → success
   - После успеха компонент испускает событие с s3_key

### Как протестировать Задачу 6:

1. Открыть карточку студента с загруженным аватаром
2. Проверить что изображение отображается через imgproxy
3. В DevTools Network проверить URL:
   - Должен начинаться с `http://localhost:8081/`
   - Содержать signature
   - Содержать processing options (rs:fill:256:256/q:80)
4. Проверить что fallback работает если s3_key не указан

---

## 🐛 Возможные проблемы

### CORS ошибки при загрузке в MinIO

MinIO уже настроен с CORS в docker-compose, но если возникнут проблемы:
```bash
docker exec aqyldy-minio mc alias set myminio http://localhost:9000 minioadmin minioadmin123
docker exec aqyldy-minio mc cors set --cors-config /tmp/cors.json myminio/aq-media
```

### Imgproxy signature не совпадает

Проверить:
1. IMGPROXY_KEY и IMGPROXY_SALT в environment совпадают с docker-compose
2. Encoding path правильный (base64url без padding)
3. HMAC использует правильные hex ключи

### Изображение не загружается

1. Проверить статус в БД: `SELECT status FROM media_object WHERE s3_key = '...'`
2. Если UPLOADING - вызвать reconcile вручную
3. Если FAILED - проверить reason в API ответе

---

## 📚 Полезные ссылки

- [imgproxy documentation](https://docs.imgproxy.net/)
- [Angular HttpClient](https://angular.io/guide/http)
- [Angular Reactive Forms](https://angular.io/guide/reactive-forms)
- [MinIO JavaScript SDK](https://min.io/docs/minio/linux/developers/javascript/minio-javascript.html) (опционально)

---

**Удачи с реализацией! 🚀**
