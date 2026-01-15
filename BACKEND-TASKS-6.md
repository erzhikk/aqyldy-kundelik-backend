# Aqyldy Kundelik — AI Coach (Backend tasks)

Цель: добавить “ИИ‑коуча” для ученика, который **на основе уже существующей аналитики** (attempt → topics → question details) генерирует:
1) **План подтянуть слабые темы** по конкретной попытке (attemptId)
2) **Разбор слабого топика** (attemptId + topicId): почему ошибки, короткое объяснение, мини‑тренировка

Важно: ИИ **общий на всех**, персонализация делается вашей БД. Стоимость контролируем **кешем + лимитами**.

---

## 0) Контекст проекта (что уже есть)
- Student analytics endpoints: `kz.aqyldykundelik.assessment.api.StudentAnalyticsController`
    - `GET /api/student/analytics/attempts/{attemptId}/topics`
    - `GET /api/student/analytics/attempts/{attemptId}/topics/{topicId}`
- Логика аналитики: `kz.aqyldykundelik.assessment.service.AnalyticsService`

Мы **не переписываем** аналитику — только переиспользуем её.

---

## 1) API: новые endpoints для student AI

Создать контроллер (новый файл):

`src/main/kotlin/kz/aqyldykundelik/assessment/api/StudentAiCoachController.kt`

### 1.1 Endpoint: План по слабым темам
`POST /api/student/ai/plan`

**Request DTO** (новый файл):
`src/main/kotlin/kz/aqyldykundelik/assessment/api/dto/AiPlanRequestDto.kt`

```kotlin
data class AiPlanRequestDto(
  val attemptId: UUID,          // по какой попытке строим план
  val language: String? = null  // "ru"|"kk"|"en" (optional, по умолчанию брать из Accept-Language или "ru")
)
```

**Response DTO** (новый файл):
`src/main/kotlin/kz/aqyldykundelik/assessment/api/dto/AiGeneratedDto.kt`

```kotlin
data class AiGeneratedDto(
  val type: String,             // "PLAN" | "TOPIC_HELP"
  val attemptId: UUID?,
  val topicId: UUID?,
  val content: String,
  val cached: Boolean,
  val createdAt: OffsetDateTime
)
```

### 1.2 Endpoint: Разбор конкретного топика
`POST /api/student/ai/attempts/{attemptId}/topics/{topicId}/help`

**Request DTO** (новый файл):
`AiTopicHelpRequestDto.kt`
```kotlin
data class AiTopicHelpRequestDto(
  val language: String? = null,
  val mode: String? = "coach"   // optional: "coach"|"short"|"practice"
)
```

**Security**
- Оба endpoint: `@PreAuthorize("hasRole('STUDENT')")`
- StudentId доставать так же, как в `StudentAnalyticsController` (через `Authentication` + `UserRepository`).
- В сервисе — обязательно проверить, что attempt принадлежит студенту (можно вызвать `AnalyticsService.getAttemptTopics(...)` / `getAttemptTopicDetails(...)` — там уже есть check).

---

## 2) Хранилище кеша (Flyway + Entity + Repo)

### 2.1 Flyway миграция
Создать файл:

`src/main/resources/db/migration/V32__create_ai_generated_content.sql`

Таблица `ai_generated_content`:

- `id uuid primary key`
- `student_id uuid not null`
- `attempt_id uuid null`
- `topic_id uuid null`
- `type varchar(32) not null` — PLAN/TOPIC_HELP
- `prompt_hash varchar(64) not null` — ключ кеша
- `content text not null`
- `model varchar(128) null`
- `provider varchar(64) null`
- `cached_until timestamptz null` (или `expires_at`)
- `created_at timestamptz not null default now()`
- `input_tokens int null`
- `output_tokens int null`

Индексы:
- unique на `(student_id, type, prompt_hash)`
- index на `(student_id, created_at desc)`
- index на `(attempt_id)` и `(topic_id)` (опционально)

### 2.2 Entity + Repo
Создать:
- `src/main/kotlin/kz/aqyldykundelik/ai/domain/AiGeneratedContentEntity.kt`
- `src/main/kotlin/kz/aqyldykundelik/ai/repo/AiGeneratedContentRepository.kt`

Repository методы:
- `findTop1ByStudentIdAndTypeAndPromptHashOrderByCreatedAtDesc(...)`
- `save(...)`

---

## 3) Сервис AI Coach

### 3.1 AiCoachService
Создать:
`src/main/kotlin/kz/aqyldykundelik/ai/service/AiCoachService.kt`

Методы:
- `generatePlan(studentId: UUID, attemptId: UUID, language: String?): AiGeneratedDto`
- `generateTopicHelp(studentId: UUID, attemptId: UUID, topicId: UUID, language: String?, mode: String?): AiGeneratedDto`

### 3.2 Как формировать “snapshot” (важно для цены)
Не отправлять в LLM “весь мир”. Делать компактно:

**PLAN**
- взять `AnalyticsService.getAttemptTopics(attemptId, studentId)`
- выбрать `weakTopics = topics.sortedBy(percent).take(3..5)`
- для каждого weak topic взять `getAttemptTopicDetails(...)` и:
    - `wrongCount`
    - `exampleWrongQuestions` (только 3–5 вопросов: text + explanation если есть)
- собрать короткий JSON/текст для промпта

**TOPIC_HELP**
- взять `getAttemptTopicDetails(...)` по topicId
- отфильтровать только неправильные/пропущенные
- максимум 5 вопросов, чтобы не раздувать токены

### 3.3 Кеширование
- Собрать `promptHash = sha256(studentId + type + attemptId + topicId + language + mode + snapshotVersion)`
- Если в таблице есть запись и `cached_until > now()` → вернуть `cached=true`
- Иначе → вызвать LLM, сохранить запись и вернуть `cached=false`
- TTL для кеша:
    - PLAN: 7 дней
    - TOPIC_HELP: 3 дня
    - (или конфигом)

---

## 4) LLM Client (провайдер-агностичный + заглушка)

Чтобы Claude‑сессии могли работать независимо, делаем абстракцию:

### 4.1 Интерфейс
`src/main/kotlin/kz/aqyldykundelik/ai/client/AiClient.kt`
```kotlin
interface AiClient {
  fun generate(request: AiGenerateRequest): AiGenerateResult
}
data class AiGenerateRequest(
  val system: String,
  val user: String,
  val model: String,
  val maxTokens: Int,
  val temperature: Double
)
data class AiGenerateResult(
  val content: String,
  val inputTokens: Int? = null,
  val outputTokens: Int? = null,
  val provider: String? = null,
  val model: String? = null
)
```

### 4.2 Заглушка для MVP
`src/main/kotlin/kz/aqyldykundelik/ai/client/StubAiClient.kt`

Возвращает детерминированный текст, например:
- PLAN: “Топ слабых тем: …; План: День1 … День7 …”
- TOPIC_HELP: “Ошибки: …; Объяснение: …; 5 задач: …”

Это позволит фронту и API заработать без ключей/оплаты.

### 4.3 Реальный клиент (вторая итерация)
- `OpenAiClient` или `GeminiClient` (на выбор)
- но **в рамках этой таски достаточно заглушки + интерфейса**, чтобы подключение реального провайдера было заменой класса.

---

## 5) Конфигурация (application-*.yml)

Добавить конфиги (в `application-dev.yml` и `application-prod.yml`):

```yaml
ai:
  enabled: true
  provider: stub          # stub|openai|gemini|mistral
  model: gpt-4o-mini      # пример, для stub не важно
  maxTokens: 700
  temperature: 0.4
  cacheTtlDays:
    plan: 7
    topicHelp: 3
  limits:
    perStudentPerDay: 5   # rate limit, см. ниже
```

(Если пока без лимитов — оставить поля, но enforcement можно сделать позже. Лучше сразу заложить.)

---

## 6) Лимиты (минимум: в памяти, лучше: в БД/Redis)

MVP вариант (быстро):
- в `AiCoachService` перед генерацией проверить количество созданных записей `ai_generated_content` за последние 24 часа по studentId
- если >= limit → `ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "AI limit reached")`

Лучше вариант (следующий шаг):
- Redis bucket4j / Resilience4j rate limiter

В этой таске достаточно **MVP‑лимита через БД**.

---

## 7) Формат промптов (чтобы ответы были “учебные”, а не “лирика”)

### SYSTEM prompt (пример)
- язык: ru/kk/en
- стиль: кратко, по шагам
- запрет: не просить персональные данные
- запрет: не упоминать токены/модели

PLAN — попросить:
- список слабых тем
- 7‑дневный план по 10–15 минут
- 2–3 коротких правила/формулы на тему
- 3 вопроса на самопроверку

TOPIC_HELP — попросить:
- объяснить одну главную ошибку
- дать 2 примера “как правильно”
- дать 5 задач (с ответами)
- если есть `explanation` в вопросе — использовать как источник

---

## 8) Acceptance Criteria

### API
- `POST /api/student/ai/plan` возвращает `AiGeneratedDto` с `type="PLAN"`
- `POST /api/student/ai/attempts/{attemptId}/topics/{topicId}/help` возвращает `type="TOPIC_HELP"`
- Проверка ownership: студент не может запросить чужой attempt/topic
- При повторном запросе в пределах TTL возвращается `cached=true`

### DB
- миграция V32 применима на пустую БД
- кеш сохраняется и читается

### Security/Errors
- 401/403 при неправильной роли/чужом attempt
- 429 при превышении лимита (если включили)

---

## 9) Тесты (минимум)
- Unit тест на `promptHash` и кеш‑ветку (когда запись есть/нет)
- Integration тест контроллера:
    - студент получает 200
    - чужой attempt → 403

---

## 10) Подсказка по “точке входа”
Сервисы/пакеты:
- `kz.aqyldykundelik.ai.*` — новое
- `kz.aqyldykundelik.assessment.api.StudentAiCoachController` — новые endpoints
- `kz.aqyldykundelik.assessment.service.AnalyticsService` — источник данных (не менять бизнес‑логику)

---
