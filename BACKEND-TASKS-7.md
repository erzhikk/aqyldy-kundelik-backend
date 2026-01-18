# Aqyldy Kundelik — AI Coach v2 (Backend, OpenAI + JSON schema)

Цель: заменить stub‑ответы на **OpenAI (gpt‑4.1‑mini / gpt‑4o‑mini)** и сразу получать
**строгий JSON по схеме**, удобный для UI (без парсинга текста).

---

## 0) Ключевые решения (ВАЖНО)
- Используем **OpenAI Responses API**
- Формат ответа: **JSON schema (structured output)**
- Модель: `gpt-4.1-mini` или `gpt-4o-mini`
- Stub остаётся для dev (`ai.provider=stub`)
- В проде: `ai.provider=openai`

---

## 1) Конфигурация

### application.yml
```yaml
ai:
  provider: openai            # stub | openai
  model: gpt-4.1-mini
  maxTokens: 900
  temperature: 0.4
  cacheTtlDays:
    plan: 7
    topicHelp: 3

openai:
  apiKey: ${OPENAI_API_KEY}
  baseUrl: https://api.openai.com/v1
```

---

## 2) JSON схемы ответов (контракт с фронтом)

### 2.1 PLAN (план по слабым темам)

```json
{
  "weakTopics": [
    {
      "topicId": "uuid",
      "topicName": "Логарифмы",
      "accuracy": 0.42,
      "mainMistake": "Ошибки в свойствах логарифмов"
    }
  ],
  "weeklyPlan": [
    {
      "day": 1,
      "focus": "Свойства логарифмов",
      "actions": [
        "Повторить определения",
        "Разобрать 2 примера"
      ],
      "timeMinutes": 15
    }
  ],
  "rules": [
    "log(a*b)=log(a)+log(b)",
    "log(a/b)=log(a)-log(b)"
  ],
  "selfCheck": [
    {
      "question": "Чему равен log(100)?",
      "answer": "2"
    }
  ]
}
```

### 2.2 TOPIC_HELP (разбор конкретного топика)

```json
{
  "topic": {
    "topicId": "uuid",
    "topicName": "Логарифмы"
  },
  "mainError": "Неправильное применение свойств логарифмов",
  "explanation": "При умножении аргументов логарифмы складываются...",
  "examples": [
    {
      "question": "log(2*8)",
      "solution": "log(2)+log(8)=1+3=4"
    }
  ],
  "practice": [
    {
      "question": "log(10*100)",
      "answer": "3"
    }
  ]
}
```

⚠️ Эти схемы **жёстко фиксированы**. Модель не должна возвращать ничего вне структуры.

---

## 3) DTO на бэке

Создать DTO (пакет `kz.aqyldykundelik.ai.dto`):

- `AiPlanResponseDto`
- `AiTopicHelpResponseDto`

Пример:
```kotlin
data class AiPlanResponseDto(
  val weakTopics: List<WeakTopicDto>,
  val weeklyPlan: List<DayPlanDto>,
  val rules: List<String>,
  val selfCheck: List<SelfCheckDto>
)
```

⚠️ **AiGeneratedDto** теперь содержит поле `payload: Any`
(PLAN → AiPlanResponseDto, TOPIC_HELP → AiTopicHelpResponseDto)

---

## 4) OpenAI client (Responses API)

### 4.1 Интерфейс остаётся
`AiClient.generate(request): AiGenerateResult`

### 4.2 OpenAiClient (новый)
Файл:
`kz/aqyldykundelik/ai/client/OpenAiClient.kt`

Запрос:
- endpoint: `POST /responses`
- headers:
    - `Authorization: Bearer {API_KEY}`
    - `Content-Type: application/json`

Тело запроса (пример для PLAN):

```json
{
  "model": "gpt-4.1-mini",
  "input": [
    {
      "role": "system",
      "content": "Ты школьный ИИ‑репетитор. Отвечай ТОЛЬКО валидным JSON по схеме."
    },
    {
      "role": "user",
      "content": "<<<SNAPSHOT JSON>>>"
    }
  ],
  "response_format": {
    "type": "json_schema",
    "json_schema": {
      "name": "ai_plan",
      "schema": { ...SCHEMA FROM SECTION 2.1... }
    }
  },
  "max_output_tokens": 900,
  "temperature": 0.4
}
```

Из ответа:
- взять `output[0].content[0].text`
- распарсить в нужный DTO через Jackson

---

## 5) Prompt Builder (критично для качества)

### SYSTEM prompt (общий)
- язык: ru (или из запроса)
- стиль: учебный, краткий
- запрещено: markdown, лишний текст, пояснения вне схемы
- **ответ строго JSON**

### USER prompt (PLAN)
- краткий snapshot:
    - weak topics (id, name, accuracy)
    - типичные ошибки
- цель: «составь план на 7 дней по 10–15 минут»

### USER prompt (TOPIC_HELP)
- topic name
- список неправильных вопросов (max 5)
- explanations (если есть)

---

## 6) Кеш и лимиты
Без изменений из предыдущей таски:
- promptHash включает **версию схемы** (например `schema=v1`)
- если схема меняется → кеш инвалидируется автоматически

---

## 7) Acceptance Criteria
- OpenAI возвращает **валидный JSON**
- Jackson успешно мапит в DTO
- фронт не парсит строки, а работает с объектами
- при ошибке OpenAI → 500 с понятным message
- при повторном запросе → кеш

---

## 8) Итог
Backend становится:
- provider‑agnostic
- дешёвым (mini‑модели)
- безопасным для UI
- готовым к масштабированию

--- 
