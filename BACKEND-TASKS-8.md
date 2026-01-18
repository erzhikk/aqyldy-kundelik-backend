# Task (Backend) — Curriculum (Hours/Week) + Teacher Assignments + Schedule MVP

Цель: подготовить **норматив часов в неделю** и **назначения учителей на предметы**, чтобы затем
делать **ручное расписание** и (позже) auto-draft.

Эта задача рассчитана на отдельную сессию Claude для backend.

---

## 0) Контекст / текущая база

У тебя уже есть:
- `class_level` (уровни 1..11 и UUID)
- `school_class` (1A..11B, A=KZ, B=RU, `class_level_id`, `lang_type`)
- `subject` (предметы по уровням, например `Математика 3`, `Алгебра 9`, `Азбука` только для 1)
- `app_user` (учителя seeded: `teacher.<key>.<kz|ru>@demo.aqyldy.kz`, и классные руководители)

Нужно добавить:
1) Учебный план (норматив часов/нед) — **per class_level**
2) Назначение учителя на предмет — **per school_class**
3) MVP расписания (ручное) — **schedule + lessons**, чтобы фронт мог сохранять сетку

---

## 1) Миграции БД (Flyway)

### 1.1 Таблица нормативов часов: `curriculum_subject_hours`
**Смысл:** сколько часов в неделю предмет должен быть в расписании для данного уровня класса.

Колонки:
- `id uuid PK`
- `class_level_id uuid FK -> class_level(id)`
- `subject_id uuid FK -> subject(id)`
- `hours_per_week int NOT NULL` (0..12)
- `created_at`, `updated_at`
- UNIQUE `(class_level_id, subject_id)`

### 1.2 Таблица назначений учителей: `class_subject_teacher`
**Смысл:** кто ведёт данный предмет в конкретном классе.

Колонки:
- `id uuid PK`
- `class_id uuid FK -> school_class(id)`
- `subject_id uuid FK -> subject(id)`
- `teacher_id uuid FK -> app_user(id)`
- `created_at`, `updated_at`
- UNIQUE `(class_id, subject_id)`

### 1.3 Таблицы расписания (MVP)
#### `class_schedule`
Один класс может иметь одно активное расписание (MVP).

Колонки:
- `id uuid PK`
- `class_id uuid FK -> school_class(id)` UNIQUE (на MVP)
- `status varchar(16)` ('DRAFT','ACTIVE') default 'DRAFT'
- `days_per_week int` (5/6) default 5
- `lessons_per_day int` default 7
- `created_at`, `updated_at`

#### `class_schedule_lesson`
Урок в конкретный слот недели.

Колонки:
- `id uuid PK`
- `schedule_id uuid FK -> class_schedule(id) ON DELETE CASCADE`
- `day_of_week int NOT NULL` (1..7, где 1=Mon)
- `lesson_number int NOT NULL` (1..N)
- `subject_id uuid FK -> subject(id)`
- `teacher_id uuid FK -> app_user(id)` (денормализация для удобства)
- UNIQUE `(schedule_id, day_of_week, lesson_number)`

> Почему teacher_id хранить прямо тут: чтобы быстро проверять конфликты/показывать UI. Источник истины по умолчанию — `class_subject_teacher`, но пользователь может переопределить учителя на конкретный слот в будущем (замены).

---

## 2) Seed (MVP данные)

### 2.1 Seed для `curriculum_subject_hours`
Сделать скрипт `seed_curriculum_subject_hours.sql`:
- для уровней 1..11 проставить разумные hours/week (MVP)
- idempotent (UPSERT)

Правило поиска subject:
- искать subject по `(name_ru, class_level_id)`:
    - `Математика {level}` или `Алгебра {level}` и т.д.
    - `Азбука` только level=1

### 2.2 Seed для `class_subject_teacher`
Скрипт `seed_class_subject_teacher.sql`:
- Для каждого класса `school_class`:
    - A (KZ) → teacher emails с `.kz`
    - B (RU) → teacher emails с `.ru`

Учителя по шаблону email:
- math: `teacher.math.<kz|ru>@demo.aqyldy.kz`
- kazakh: `teacher.kazakh_lang.<kz|ru>@demo.aqyldy.kz`
- russian: `teacher.russian_lang.<kz|ru>@demo.aqyldy.kz`
- english: `teacher.english.<kz|ru>@demo.aqyldy.kz`
- history, geography, biology, chemistry, physics, informatics, pe, art, music
- reading/world_studies (если есть): `teacher.reading.*`, `teacher.world_studies.*`

> Важно: если учителя чтения/дүниетану нет — seed должен создать их (TEACHER role).

---

## 3) Backend API (контракты для фронта)

### 3.1 Curriculum API (Admin)
Base: `/api/admin/curriculum`

#### GET `/levels`
Возвращает список уровней (id, level, name_ru, name_kk)

#### GET `/levels/{classLevelId}/subjects`
Возвращает список предметов уровня + текущие hours/week:
```json
{
  "classLevelId":"uuid",
  "subjects":[
    {
      "subjectId":"uuid",
      "nameRu":"Математика 5",
      "nameKk":"Математика 5",
      "nameEn":"Math 5",
      "hoursPerWeek":5
    }
  ]
}
```

#### PUT `/levels/{classLevelId}/subjects`
Обновить hours/week пачкой:
```json
{
  "items":[
    {"subjectId":"uuid","hoursPerWeek":5},
    {"subjectId":"uuid","hoursPerWeek":3}
  ]
}
```
Ответ: 200 OK + актуальное состояние.

---

### 3.2 Teacher Assignment API (Admin)
Base: `/api/admin/assignments`

#### GET `/classes/{classId}`
Возвращает предметы класса и назначенных учителей:
```json
{
  "classId":"uuid",
  "classCode":"7A",
  "items":[
    {
      "subjectId":"uuid",
      "subjectNameRu":"Алгебра 7",
      "teacherId":"uuid",
      "teacherFullName":"Математика мұғалімі (қаз)",
      "teacherEmail":"teacher.math.kz@demo.aqyldy.kz"
    }
  ]
}
```

#### PUT `/classes/{classId}`
Сохранить назначения пачкой:
```json
{
  "items":[
    {"subjectId":"uuid","teacherId":"uuid"},
    {"subjectId":"uuid","teacherId":"uuid"}
  ]
}
```

#### GET `/teachers`
Список учителей (для выбора в UI):
- фильтр optional `?q=`
- возвращаем только role=TEACHER, is_deleted=false, status=ACTIVE

---

### 3.3 Schedule API (Admin) — ручное расписание
Base: `/api/admin/schedule`

#### GET `/classes/{classId}`
Возвращает расписание класса (создаёт DRAFT при отсутствии):
```json
{
  "scheduleId":"uuid",
  "classId":"uuid",
  "status":"DRAFT",
  "daysPerWeek":5,
  "lessonsPerDay":7,
  "grid":[
    {
      "dayOfWeek":1,
      "lessonNumber":1,
      "subjectId":"uuid",
      "subjectNameRu":"Математика 7",
      "teacherId":"uuid",
      "teacherFullName":"..."
    }
  ],
  "conflicts":[
    {
      "type":"TEACHER_BUSY",
      "teacherId":"uuid",
      "dayOfWeek":2,
      "lessonNumber":3,
      "message":"Учитель занят в другом классе"
    }
  ]
}
```

#### PUT `/classes/{classId}`
Сохранить всю сетку пачкой:
```json
{
  "daysPerWeek":5,
  "lessonsPerDay":7,
  "grid":[
    {"dayOfWeek":1,"lessonNumber":1,"subjectId":"uuid"},
    {"dayOfWeek":1,"lessonNumber":2,"subjectId":"uuid"}
  ]
}
```

Server-side:
- если `teacherId` не пришёл, берём из `class_subject_teacher` для пары (classId, subjectId)
- сохраняем/апдейтим `class_schedule_lesson`
- возвращаем обновлённый grid + conflicts

#### POST `/classes/{classId}/activate`
Ставит `status=ACTIVE` (и проверяет, что конфликтов нет или они подтверждены).

---

## 4) Логика конфликтов (MVP)

### TEACHER_BUSY
Учитель не может вести одновременно два урока в один слот.
Проверка:
- по всем активным/черновым расписаниям других классов
- конфликт если есть запись с тем же `(day_of_week, lesson_number, teacher_id)`.

### HOURS_MISMATCH (мягкий конфликт)
Сравнить `curriculum_subject_hours` с фактическим количеством уроков предмета в сетке.
Возвращать предупреждения, но не блокировать сохранение.

---

## 5) Реализация (структура кода)

Рекомендуемые пакеты:
- `...curriculum` (controller/service/repo/dto)
- `...assignments` (controller/service/repo/dto)
- `...schedule` (controller/service/repo/dto + conflict checker)

Репозитории:
- `CurriculumSubjectHoursRepository`
- `ClassSubjectTeacherRepository`
- `ClassScheduleRepository`
- `ClassScheduleLessonRepository`
- `UserRepository` (teacher list)

---

## 6) Acceptance Criteria
- Миграции применяются без ошибок
- Seed идемпотентен (повторный запуск не дублирует)
- API отдаёт данные в форматах, описанных выше
- PUT schedule сохраняет сетку и возвращает conflicts
- TEACHER_BUSY конфликт реально ловится на пересечении двух классов

---

## 7) Out of scope (позже)
- кабинеты
- чёт/нечёт недели
- замены по датам
- авто-генерация (draft) — отдельная задача после MVP ручного расписания
