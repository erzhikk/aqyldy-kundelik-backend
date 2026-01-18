# Task (Frontend) — Curriculum + Teacher Assignments + Schedule MVP

Цель: создать UI для управления **учебным планом** (часы/неделю), **назначениями учителей** и **ручным расписанием**.

---

## 0) Контекст

Backend уже реализован и предоставляет три группы API:
1. **Curriculum API** — нормативы часов в неделю по уровням классов
2. **Assignments API** — назначение учителей на предметы для классов
3. **Schedule API** — ручное составление расписания с проверкой конфликтов

---

## 1) Curriculum (Учебный план)

### 1.1 API Endpoints
Base: `/api/admin/curriculum`

#### GET `/levels`
Список уровней классов (1-11):
```json
[
  {
    "id": "uuid",
    "level": 1,
    "nameRu": "1 класс",
    "nameKk": "1 сынып",
    "maxLessonsPerDay": 4
  }
]
```

#### GET `/levels/{classLevelId}/subjects`
Предметы уровня с часами:
```json
{
  "classLevelId": "uuid",
  "maxLessonsPerDay": 4,
  "maxHoursPerWeek": 20,
  "totalHoursPerWeek": 18,
  "subjects": [
    {
      "subjectId": "uuid",
      "nameRu": "Математика 1",
      "nameKk": "Математика 1",
      "nameEn": "Math 1",
      "hoursPerWeek": 5
    }
  ],
  "warnings": ["Сумма часов (22) превышает максимум (20)"]
}
```

#### PUT `/levels/{classLevelId}/subjects`
Обновить часы пачкой:
```json
{
  "items": [
    {"subjectId": "uuid", "hoursPerWeek": 5},
    {"subjectId": "uuid", "hoursPerWeek": 3}
  ]
}
```

### 1.2 UI Requirements

**Страница: `/admin/curriculum`**

1. **Табы/селектор по уровням классов** (1-11)
2. Для выбранного уровня показать:
   - Информация: `maxLessonsPerDay`, `maxHoursPerWeek`, `totalHoursPerWeek`
   - Прогресс-бар: `totalHoursPerWeek / maxHoursPerWeek` (красный если превышен)
   - Таблица предметов с editable `hoursPerWeek` (0-12)
3. **Warnings** — показывать если `warnings.length > 0`
4. Кнопка "Сохранить" — PUT запрос с измененными данными

**UX:**
- При изменении часов пересчитывать `totalHoursPerWeek` локально
- Показывать warning в реальном времени если сумма > max
- После сохранения обновить данные с сервера

---

## 2) Assignments (Назначения учителей)

### 2.1 API Endpoints
Base: `/api/admin/assignments`

#### GET `/classes/{classId}`
Предметы класса с назначенными учителями:
```json
{
  "classId": "uuid",
  "classCode": "7A",
  "items": [
    {
      "subjectId": "uuid",
      "subjectNameRu": "Алгебра 7",
      "teacherId": "uuid",
      "teacherFullName": "Математика мұғалімі (қаз)",
      "teacherEmail": "teacher.math.kz@demo.aqyldy.kz"
    }
  ]
}
```

#### PUT `/classes/{classId}`
Сохранить назначения:
```json
{
  "items": [
    {"subjectId": "uuid", "teacherId": "uuid"}
  ]
}
```

#### GET `/teachers?q=...`
Список учителей (для dropdown):
```json
[
  {
    "id": "uuid",
    "fullName": "Иванов Иван",
    "email": "teacher.math.kz@demo.aqyldy.kz"
  }
]
```

### 2.2 UI Requirements

**Страница: `/admin/assignments`**

1. **Селектор класса** (выбрать из списка классов)
2. Для выбранного класса:
   - Таблица: Предмет | Учитель (dropdown с поиском)
   - Каждый ряд: выбор учителя из `/teachers?q=...`
3. Кнопка "Сохранить"

**UX:**
- Dropdown учителей с поиском (debounced)
- Показывать email учителя рядом с именем
- Если учитель не назначен — показывать "Не назначен"

---

## 3) Schedule (Расписание)

### 3.1 API Endpoints
Base: `/api/admin/schedule`

#### GET `/classes/{classId}`
Получить/создать расписание:
```json
{
  "scheduleId": "uuid",
  "classId": "uuid",
  "status": "DRAFT",
  "daysPerWeek": 5,
  "lessonsPerDay": 7,
  "grid": [
    {
      "dayOfWeek": 1,
      "lessonNumber": 1,
      "subjectId": "uuid",
      "subjectNameRu": "Математика 7",
      "teacherId": "uuid",
      "teacherFullName": "Иванов Иван"
    }
  ],
  "conflicts": [
    {
      "type": "TEACHER_BUSY",
      "teacherId": "uuid",
      "dayOfWeek": 2,
      "lessonNumber": 3,
      "message": "Учитель занят в другом классе"
    },
    {
      "type": "MAX_LESSONS_EXCEEDED",
      "teacherId": null,
      "dayOfWeek": 1,
      "lessonNumber": 0,
      "message": "Понедельник: 5 уроков (максимум 4 для 1 класса)"
    },
    {
      "type": "HOURS_MISMATCH",
      "teacherId": null,
      "dayOfWeek": 0,
      "lessonNumber": 0,
      "message": "Математика: ожидается 5 ч/нед, фактически 3"
    }
  ]
}
```

#### PUT `/classes/{classId}`
Сохранить сетку:
```json
{
  "daysPerWeek": 5,
  "lessonsPerDay": 7,
  "grid": [
    {"dayOfWeek": 1, "lessonNumber": 1, "subjectId": "uuid"},
    {"dayOfWeek": 1, "lessonNumber": 2, "subjectId": "uuid"}
  ]
}
```

#### POST `/classes/{classId}/activate`
Активировать расписание (status → ACTIVE)

### 3.2 UI Requirements

**Страница: `/admin/schedule`**

1. **Селектор класса**
2. **Сетка расписания:**
   - Колонки: Пн, Вт, Ср, Чт, Пт (+ Сб если daysPerWeek=6)
   - Ряды: 1-й урок, 2-й урок, ... (до lessonsPerDay)
   - Каждая ячейка: dropdown для выбора предмета

3. **Панель конфликтов:**
   - Показывать все conflicts с цветовой кодировкой:
     - `TEACHER_BUSY` — красный (критично)
     - `MAX_LESSONS_EXCEEDED` — красный (критично)
     - `HOURS_MISMATCH` — желтый (предупреждение)
   - Клик на конфликт → подсветить ячейку

4. **Действия:**
   - "Сохранить черновик" — PUT запрос
   - "Активировать" — POST activate (только если нет критичных конфликтов)

**UX:**
- Drag & drop предметов в ячейки (опционально)
- При выборе предмета учитель подставляется автоматически из assignments
- Подсветка ячеек с конфликтами
- Счетчик: "Математика: 3/5 часов" (фактически/норматив)

---

## 4) Навигация

Добавить в admin sidebar:
- **Учебный план** → `/admin/curriculum`
- **Назначения учителей** → `/admin/assignments`
- **Расписание** → `/admin/schedule`

Порядок работы для пользователя:
1. Сначала настроить curriculum (часы)
2. Затем назначить учителей
3. Затем составить расписание

---

## 5) Типы конфликтов

| Тип | Описание | Критичность |
|-----|----------|-------------|
| `TEACHER_BUSY` | Учитель занят в другом классе в этот слот | Критично |
| `MAX_LESSONS_EXCEEDED` | Превышен лимит уроков в день для уровня | Критично |
| `HOURS_MISMATCH` | Количество часов предмета не совпадает с нормативом | Предупреждение |

---

## 6) Лимиты уроков в день по уровням

| Уровень | Макс. уроков/день |
|---------|-------------------|
| 1 класс | 4 |
| 2-4 класс | 5 |
| 5-11 класс | 7 |

Эти данные приходят в `maxLessonsPerDay` из API.

---

## 7) Acceptance Criteria

- [ ] Curriculum: можно редактировать часы для каждого уровня
- [ ] Curriculum: показывает warning при превышении лимита
- [ ] Assignments: можно назначить учителя на предмет
- [ ] Schedule: сетка расписания с выбором предметов
- [ ] Schedule: конфликты отображаются и подсвечиваются
- [ ] Schedule: TEACHER_BUSY реально ловится при пересечении
- [ ] Schedule: MAX_LESSONS_EXCEEDED показывается для младших классов

---

## 8) Зависимости

Для работы Schedule нужны:
1. Список классов: `GET /api/classes/all`
2. Список предметов класса: из `GET /api/admin/assignments/classes/{classId}`

---
