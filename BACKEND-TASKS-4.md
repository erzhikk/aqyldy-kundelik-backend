## 1) Backend таска — Analytics API

### Статус: ✅ РЕАЛИЗОВАНО

**Дата реализации:** 2026-01-11

**Реализованные компоненты:**
- ✅ Новые DTO в `AssessmentDtos.kt`
- ✅ Новые методы в `TestAttemptRepository`
- ✅ Расширенный `AnalyticsService` с методами для студентов и учителей
- ✅ `StudentAnalyticsController` с 4 endpoints
- ✅ `TeacherAnalyticsController` с 4 endpoints

### Цель
Сделать API для аналитики результатов тестов:
- **Студент** видит только свою аналитику (по токену, без studentId в запросах).
- **Учитель/админ** видит аналитику по классам (агрегация по классу), с проваливанием в конкретный класс и внутрь топика.

Аналитика строится из фактических ответов:

`TestAttempt (status=GRADED) → AttemptAnswer → Question → Topic`

---

### 1.1 Данные и связи (источник аналитики)
Используем сущности/таблицы:
- `test`
- `test_attempt`
- `attempt_answer`
- `question`
- `topic`
- `user/student` (и связь с `class/group`, например `class_id`/`group_id`)

**Важно:** аналитика всегда считается по конкретной попытке (`attemptId`).

---

### 1.2 Student Dashboard API (только текущий пользователь)

#### A) Получить последнюю оцененную попытку (последний тест студента)
- Endpoint: `GET /api/student/analytics/last-attempt`
- Доступ: роль STUDENT (или общий доступ с проверкой currentUser is student)
- Логика: берём **последний `test_attempt` со статусом `GRADED`**.

SQL (пример):
```sql
SELECT ta.id, ta.test_id, ta.graded_at
FROM test_attempt ta
WHERE ta.student_id = :currentStudentId
  AND ta.status = 'GRADED'
ORDER BY ta.graded_at DESC
LIMIT 1;
```

> Если поля `graded_at` нет — использовать `modified_date`/`finished_at`/`created_date` (что ближе к факту выставления оценки).

#### B) Summary для карточки (уровень 1)
- Endpoint: `GET /api/student/analytics/attempts/{attemptId}/summary`
- Проверка доступа: `attempt.student_id == currentUser.id`

Возвращает:
- testId, testName
- totalQuestions, correctAnswers, wrongAnswers, percent
- `strongTopics[]`, `weakTopics[]` (по порогам)
- optional: `attemptDate`

Пороговые зоны (MVP):
- green: `>= 75%`
- yellow: `50–74%`
- red: `< 50%`

#### C) Bar chart по топикам (уровень 2)
- Endpoint: `GET /api/student/analytics/attempts/{attemptId}/topics`
- Возвращает массив топиков с метриками.

SQL (Postgres, пример с FILTER):
```sql
SELECT
    t2.id   AS topic_id,
    t2.name AS topic_name,
    COUNT(*) AS total,
    COUNT(*) FILTER (WHERE aa.is_correct = true)  AS correct,
    COUNT(*) FILTER (WHERE aa.is_correct = false) AS wrong,
    COUNT(*) FILTER (WHERE aa.is_correct IS NULL) AS skipped,
    ROUND(
        100.0 * COUNT(*) FILTER (WHERE aa.is_correct = true) / NULLIF(COUNT(*), 0),
        2
    ) AS percent
FROM attempt_answer aa
JOIN question q ON q.id = aa.question_id
JOIN topic t2 ON t2.id = q.topic_id
WHERE aa.attempt_id = :attemptId
GROUP BY t2.id, t2.name
ORDER BY percent ASC;
```

#### D) Drill-down внутрь топика (уровень 3)
- Endpoint: `GET /api/student/analytics/attempts/{attemptId}/topics/{topicId}`
- Возвращает список вопросов топика и как ответил студент:
    - questionId, text
    - chosenAnswerId (или payload ответа)
    - isCorrect
    - explanation (если есть)

SQL (пример):
```sql
SELECT
    q.id AS question_id,
    q.text,
    aa.choice_id,
    aa.is_correct,
    q.explanation
FROM attempt_answer aa
JOIN question q ON q.id = aa.question_id
WHERE aa.attempt_id = :attemptId
  AND q.topic_id = :topicId
ORDER BY q.id;
```

---

### 1.3 Teacher/Admin — Analytics по классам

#### A) Список классов для дэшборда
- Endpoint: `GET /api/teacher/analytics/classes`
- Возвращает: classId, className (+ краткие метрики опционально)
- Доступ:
    - TEACHER: только свои классы (по привязке)
    - ADMIN: все классы

#### B) Аналитика конкретного класса по последнему тесту (карточка)
- Endpoint: `GET /api/teacher/analytics/classes/{classId}/last-test/summary`
- Возвращает:
    - testId/testName, date
    - avgPercent, medianPercent
    - weakTopics[] (топики с низким avg)
    - riskStudentsCount (сколько учеников ниже порога, напр. < 40%)

**Как выбрать “последний тест” класса (MVP):**
- Берём последний `test_attempt.status=GRADED` среди учеников класса, и по его `test_id` считаем агрегаты.
- (Позже можно выбрать “последний назначенный тест” по расписанию/назначениям.)

#### C) Bar chart по топикам для класса (уровень 2)
- Endpoint: `GET /api/teacher/analytics/classes/{classId}/tests/{testId}/topics`

Важно: агрегировать **по последней попытке каждого ученика** (иначе попытки “раздуют” статистику).

SQL (среднее по ученикам, “правильнее”):
```sql
WITH latest_attempt AS (
    SELECT DISTINCT ON (ta.student_id)
        ta.id,
        ta.student_id
    FROM test_attempt ta
    JOIN app_user u ON u.id = ta.student_id
    WHERE ta.test_id = :testId
      AND u.class_id = :classId
      AND ta.status = 'GRADED'
    ORDER BY ta.student_id, ta.graded_at DESC
),
per_student_topic AS (
    SELECT
        la.student_id,
        q.topic_id,
        SUM(CASE WHEN aa.is_correct = true THEN 1 ELSE 0 END)::float / NULLIF(COUNT(*), 0) AS pct
    FROM latest_attempt la
    JOIN attempt_answer aa ON aa.attempt_id = la.id
    JOIN question q ON q.id = aa.question_id
    GROUP BY la.student_id, q.topic_id
)
SELECT
    pst.topic_id,
    tp.name AS topic_name,
    ROUND(AVG(pst.pct) * 100.0, 2) AS avg_percent,
    ROUND(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY pst.pct) * 100.0, 2) AS median_percent,
    COUNT(*) AS students_count
FROM per_student_topic pst
JOIN topic tp ON tp.id = pst.topic_id
GROUP BY pst.topic_id, tp.name
ORDER BY avg_percent ASC;
```

#### D) Drill-down класса внутрь топика (уровень 3)
- Endpoint: `GET /api/teacher/analytics/classes/{classId}/tests/{testId}/topics/{topicId}`
- Возвращает:
    - top worst questions (по % неправильных)
    - распределение ответов по вариантам (если есть варианты)
    - список учеников “risk” по этому топику (опционально)

Worst questions (пример):
```sql
WITH latest_attempt AS (
    SELECT DISTINCT ON (ta.student_id)
        ta.id,
        ta.student_id
    FROM test_attempt ta
    JOIN app_user u ON u.id = ta.student_id
    WHERE ta.test_id = :testId
      AND u.class_id = :classId
      AND ta.status = 'GRADED'
    ORDER BY ta.student_id, ta.graded_at DESC
)
SELECT
    q.id AS question_id,
    q.text,
    COUNT(*) AS answers_count,
    COUNT(*) FILTER (WHERE aa.is_correct = false) AS wrong_count,
    ROUND(100.0 * COUNT(*) FILTER (WHERE aa.is_correct = false) / NULLIF(COUNT(*), 0), 2) AS wrong_percent
FROM latest_attempt la
JOIN attempt_answer aa ON aa.attempt_id = la.id
JOIN question q ON q.id = aa.question_id
WHERE q.topic_id = :topicId
GROUP BY q.id, q.text
ORDER BY wrong_percent DESC, wrong_count DESC
LIMIT 10;
```

---

### 1.4 DTO (примерные контракты)

#### TopicScoreDto
- topicId: UUID
- topicName: String
- total: Int
- correct: Int
- wrong: Int
- skipped: Int?
- percent: BigDecimal

#### StudentAttemptSummaryDto
- testId: UUID
- testName: String
- attemptId: UUID
- totalQuestions: Int
- correctAnswers: Int
- wrongAnswers: Int
- percent: BigDecimal
- strongTopics: List<String>
- weakTopics: List<String>

#### ClassTopicAnalyticsDto
- topicId: UUID
- topicName: String
- avgPercent: BigDecimal
- medianPercent: BigDecimal
- studentsCount: Int

---

### 1.5 Технические требования
- Все student endpoints работают по currentUser (JWT), без studentId в query.
- Для агрегаций использовать **native SQL** (COUNT FILTER / CTE).
- Индексы (если начнёт тормозить):
    - `test_attempt(test_id, status, student_id, graded_at)`
    - `attempt_answer(attempt_id, question_id, is_correct)`
    - `question(topic_id)`

---

## 2) Реализованные Endpoints

### Student Analytics API

**BaseURL:** `/api/student/analytics`

**Авторизация:** `ROLE_STUDENT`

1. **GET `/last-attempt`**
   - Описание: Получить последнюю оцененную попытку студента
   - Возвращает: `LastAttemptDto`

2. **GET `/attempts/{attemptId}/summary`**
   - Описание: Сводка по конкретной попытке (карточка уровень 1)
   - Возвращает: `StudentAttemptSummaryDto` с totalQuestions, correctAnswers, wrongAnswers, percent, strongTopics[], weakTopics[]

3. **GET `/attempts/{attemptId}/topics`**
   - Описание: Метрики по топикам для попытки (bar chart уровень 2)
   - Возвращает: `List<TopicScoreDto>` с total, correct, wrong, skipped, percent

4. **GET `/attempts/{attemptId}/topics/{topicId}`**
   - Описание: Детали вопросов в топике (drill-down уровень 3)
   - Возвращает: `List<QuestionDetailDto>` с questionId, text, choiceId, isCorrect, explanation

### Teacher Analytics API

**BaseURL:** `/api/teacher/analytics`

**Авторизация:** `ROLE_TEACHER`, `ROLE_ADMIN_ASSESSMENT`, `ROLE_SUPER_ADMIN`

1. **GET `/classes`**
   - Описание: Список классов (для учителя - только его классы, для админа - все)
   - Возвращает: `List<ClassInfoDto>` с classId, className

2. **GET `/classes/{classId}/last-test/summary`**
   - Описание: Сводка по последнему тесту класса (карточка уровень 1)
   - Возвращает: `ClassTestSummaryDto` с testId, testName, date, avgPercent, medianPercent, weakTopics[], riskStudentsCount

3. **GET `/classes/{classId}/tests/{testId}/topics`**
   - Описание: Метрики по топикам для класса (bar chart уровень 2)
   - Возвращает: `List<ClassTopicAnalyticsDto>` с avgPercent, medianPercent, studentsCount

4. **GET `/classes/{classId}/tests/{testId}/topics/{topicId}`**
   - Описание: Детали топика - самые сложные вопросы (drill-down уровень 3)
   - Возвращает: `TopicDrillDownDto` с topWeakQuestions[] (топ-10 вопросов с наибольшим процентом ошибок)

---

## 3) Технические детали реализации

### Добавленные файлы:
1. `src/main/kotlin/kz/aqyldykundelik/assessment/api/StudentAnalyticsController.kt`
2. `src/main/kotlin/kz/aqyldykundelik/assessment/api/TeacherAnalyticsController.kt`

### Модифицированные файлы:
1. `src/main/kotlin/kz/aqyldykundelik/assessment/api/dto/AssessmentDtos.kt`
   - Добавлены DTO: LastAttemptDto, StudentAttemptSummaryDto, TopicScoreDto, QuestionDetailDto, ClassInfoDto, ClassTestSummaryDto, ClassTopicAnalyticsDto, WorstQuestionDto, TopicDrillDownDto

2. `src/main/kotlin/kz/aqyldykundelik/assessment/repo/AssessmentRepositories.kt`
   - Добавлены методы: findLastGradedAttemptByStudentId, findLatestGradedAttemptsByClassAndTest, findLastGradedAttemptByClassId

3. `src/main/kotlin/kz/aqyldykundelik/assessment/service/AnalyticsService.kt`
   - Добавлены student методы: getLastAttempt, getAttemptSummary, getAttemptTopics, getAttemptTopicDetails
   - Добавлены teacher методы: getClassesList, getClassLastTestSummary, getClassTestTopics, getClassTestTopicDetails
   - Добавлены helper методы: calculateTopicScoresForAttempt, calculateClassTopicScores

### Особенности реализации:

1. **Использование finishedAt**: Вместо graded_at используется поле finishedAt для сортировки попыток
2. **Название класса**: Используется поле code из ClassEntity как className
3. **Пороги**:
   - Сильные топики: >= 75%
   - Слабые топики: < 50%
   - Risk students: < 40%
4. **Агрегация для классов**: Используется DISTINCT ON для получения последней попытки каждого студента, затем агрегируются метрики
5. **Безопасность**: Студенты видят только свои данные, учителя - только свои классы, админы - все

---