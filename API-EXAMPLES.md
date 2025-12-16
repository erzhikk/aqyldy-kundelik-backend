# API для карточек студентов и сотрудников

## Новые эндпоинты

### 1. GET /api/users/student/{id}/card
Получить расширенную карточку студента

**Описание:**
- Возвращает полную информацию о студенте
- Включает информацию о классе
- Показывает статистику посещаемости

**Пример запроса:**
```bash
curl -X GET "http://localhost:8080/api/users/student/{studentId}/card" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Пример ответа:**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "email": "student@example.com",
  "fullName": "Иванов Иван Иванович",
  "dateOfBirth": "2010-05-15",
  "isActive": true,
  "status": "ACTIVE",
  "schoolClass": {
    "id": "456e7890-e89b-12d3-a456-426614174000",
    "code": "5A",
    "classTeacherId": "789e0123-e89b-12d3-a456-426614174000",
    "langType": "RUS"
  },
  "attendanceStats": {
    "totalLessons": 120,
    "present": 100,
    "late": 10,
    "absent": 5,
    "excused": 5,
    "attendanceRate": 91.67
  }
}
```

**Возможные ошибки:**
- 404 - пользователь не найден
- 400 - пользователь не является студентом

---

### 2. GET /api/users/staff/{id}/card
Получить расширенную карточку сотрудника

**Описание:**
- Возвращает полную информацию о сотруднике (учитель, администратор)
- Включает класс, в котором сотрудник является классным руководителем
- Показывает список предметов, которые преподает

**Пример запроса:**
```bash
curl -X GET "http://localhost:8080/api/users/staff/{staffId}/card" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Пример ответа:**
```json
{
  "id": "789e0123-e89b-12d3-a456-426614174000",
  "email": "teacher@example.com",
  "fullName": "Петров Петр Петрович",
  "role": "TEACHER",
  "isActive": true,
  "status": "ACTIVE",
  "classAsTeacher": {
    "id": "456e7890-e89b-12d3-a456-426614174000",
    "code": "5A",
    "classTeacherId": "789e0123-e89b-12d3-a456-426614174000",
    "langType": "RUS"
  },
  "taughtSubjects": [
    {
      "subjectId": "abc12345-e89b-12d3-a456-426614174000",
      "subjectName": "Математика",
      "groups": ["5A группа 1", "5B группа 1"]
    },
    {
      "subjectId": "def67890-e89b-12d3-a456-426614174000",
      "subjectName": "Физика",
      "groups": ["5A группа 1"]
    }
  ]
}
```

**Возможные ошибки:**
- 404 - пользователь не найден
- 400 - пользователь не является сотрудником

---

## Как тестировать

### 1. Авторизация
Сначала получите токен доступа:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@local","password":"admin123"}'
```

Ответ будет содержать `accessToken`, который нужно использовать в последующих запросах.

### 2. Создание тестового студента

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "email": "student@test.com",
    "fullName": "Тестовый Студент",
    "role": "STUDENT",
    "password": "password123",
    "classId": null,
    "dateOfBirth": "2010-05-15"
  }'
```

Сохраните `id` из ответа.

### 3. Получение карточки студента

```bash
curl -X GET http://localhost:8080/api/users/student/{id}/card \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 4. Создание тестового учителя

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "email": "teacher@test.com",
    "fullName": "Тестовый Учитель",
    "role": "TEACHER",
    "password": "password123",
    "classId": null,
    "dateOfBirth": "1985-03-20"
  }'
```

Сохраните `id` из ответа.

### 5. Получение карточки сотрудника

```bash
curl -X GET http://localhost:8080/api/users/staff/{id}/card \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## Swagger UI

Вы также можете протестировать API через Swagger UI:
```
http://localhost:8080/swagger-ui.html
```

1. Откройте Swagger UI в браузере
2. Нажмите кнопку "Authorize" в правом верхнем углу
3. Введите токен в формате: `Bearer YOUR_ACCESS_TOKEN`
4. Найдите новые эндпоинты в секции "users-controller"
5. Попробуйте выполнить запросы

---

## Структура данных

### StudentCardDto
```kotlin
data class StudentCardDto(
    val id: UUID,
    val email: String,
    val fullName: String,
    val dateOfBirth: LocalDate?,
    val isActive: Boolean,
    val status: String,
    val schoolClass: ClassDto?,          // Класс студента
    val attendanceStats: AttendanceStatsDto?  // Статистика посещаемости
)
```

### StaffCardDto
```kotlin
data class StaffCardDto(
    val id: UUID,
    val email: String,
    val fullName: String,
    val role: String,
    val isActive: Boolean,
    val status: String,
    val classAsTeacher: ClassDto?,       // Класс, где является классным руководителем
    val taughtSubjects: List<TaughtSubjectDto>  // Предметы, которые преподает
)
```

### AttendanceStatsDto
```kotlin
data class AttendanceStatsDto(
    val totalLessons: Long,    // Всего занятий
    val present: Long,         // Присутствовал
    val late: Long,            // Опоздал
    val absent: Long,          // Отсутствовал
    val excused: Long,         // Отсутствовал по уважительной причине
    val attendanceRate: Double // Процент посещаемости (0-100)
)
```

### TaughtSubjectDto
```kotlin
data class TaughtSubjectDto(
    val subjectId: UUID,
    val subjectName: String,   // Название предмета (на русском)
    val groups: List<String>   // Названия групп
)
```

---

## Примечания

1. **Статистика посещаемости** будет `null` если у студента нет записей о посещаемости
2. **classAsTeacher** будет `null` если сотрудник не является классным руководителем
3. **taughtSubjects** будет пустым списком если у учителя нет назначенных уроков в расписании
4. **Процент посещаемости** рассчитывается как: `(present + late) / totalLessons * 100`
5. Оба эндпоинта доступны для всех авторизованных пользователей (без специальных ролей)
