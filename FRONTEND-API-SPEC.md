# API Спецификация для карточек пользователей

## 1. Карточка СТУДЕНТА

### Эндпоинт
```
GET /api/users/student/{studentId}/card
```

### Headers
```
Authorization: Bearer {accessToken}
```

### Пример запроса
```bash
GET http://localhost:8080/api/users/student/ec79d59a-18f9-43d1-a9ef-5b8725cfff3b/card
Authorization: Bearer eyJraWQiOi...
```

### Ответ (Response DTO)

```typescript
interface StudentCardDto {
  id: string;                    // UUID студента
  email: string;                 // Email
  fullName: string;              // ФИО студента
  dateOfBirth: string | null;    // Дата рождения (формат: "2010-09-15")
  isActive: boolean;             // Активен ли аккаунт
  status: string;                // Статус: "ACTIVE" | "INACTIVE"
  schoolClass: ClassDto | null;  // Информация о классе (может быть null)
  attendanceStats: AttendanceStatsDto | null;  // Статистика посещаемости (может быть null)
}

interface ClassDto {
  id: string;              // UUID класса
  code: string;            // Код класса, например "5A", "7B", "11Г"
  classTeacherId: string | null;  // UUID классного руководителя
  langType: string;        // Язык обучения: "RUS" | "KAZ" | "ENG"
}

interface AttendanceStatsDto {
  totalLessons: number;    // Всего уроков в системе
  present: number;         // Присутствовал
  late: number;            // Опоздал
  absent: number;          // Отсутствовал (без причины)
  excused: number;         // Отсутствовал по уважительной причине
  attendanceRate: number;  // Процент посещаемости (0-100), например 91.67
}
```

### Пример реального ответа (студент БЕЗ класса и посещаемости)
```json
{
  "id": "ec79d59a-18f9-43d1-a9ef-5b8725cfff3b",
  "email": "student@example.com",
  "fullName": "Aidar Nurlanuly",
  "dateOfBirth": "2010-09-15",
  "isActive": true,
  "status": "ACTIVE",
  "schoolClass": null,
  "attendanceStats": null
}
```

### Пример ответа (студент С классом и посещаемостью)
```json
{
  "id": "ec79d59a-18f9-43d1-a9ef-5b8725cfff3b",
  "email": "student@example.com",
  "fullName": "Айдар Нұрланұлы",
  "dateOfBirth": "2010-09-15",
  "isActive": true,
  "status": "ACTIVE",
  "schoolClass": {
    "id": "456e7890-e89b-12d3-a456-426614174000",
    "code": "5A",
    "classTeacherId": "789e0123-e89b-12d3-a456-426614174000",
    "langType": "KAZ"
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

### Возможные ошибки
- **400 Bad Request** - пользователь не является студентом
- **404 Not Found** - пользователь с таким ID не найден
- **401 Unauthorized** - нет токена авторизации

---

## 2. Карточка СОТРУДНИКА (учителя/администратора)

### Эндпоинт
```
GET /api/users/staff/{staffId}/card
```

### Headers
```
Authorization: Bearer {accessToken}
```

### Пример запроса
```bash
GET http://localhost:8080/api/users/staff/6bea0c2a-8098-4b27-9b57-a9594645e357/card
Authorization: Bearer eyJraWQiOi...
```

### Ответ (Response DTO)

```typescript
interface StaffCardDto {
  id: string;                    // UUID сотрудника
  email: string;                 // Email
  fullName: string;              // ФИО сотрудника
  role: string;                  // Роль: "TEACHER" | "ADMIN" | "ADMIN_SCHEDULE" | "ADMIN_ASSESSMENT" | "SUPER_ADMIN"
  isActive: boolean;             // Активен ли аккаунт
  status: string;                // Статус: "ACTIVE" | "INACTIVE"
  classAsTeacher: ClassDto | null;         // Класс, где является классным руководителем (может быть null)
  taughtSubjects: TaughtSubjectDto[];      // Список предметов, которые преподает (может быть пустым)
}

interface ClassDto {
  id: string;              // UUID класса
  code: string;            // Код класса, например "5A", "7B", "11Г"
  classTeacherId: string | null;  // UUID классного руководителя
  langType: string;        // Язык обучения: "RUS" | "KAZ" | "ENG"
}

interface TaughtSubjectDto {
  subjectId: string;       // UUID предмета
  subjectName: string;     // Название предмета (на русском из nameRu)
  groups: string[];        // Названия учебных групп, например ["5A группа 1", "5B группа 1"]
}
```

### Пример реального ответа (учитель БЕЗ класса и предметов)
```json
{
  "id": "6bea0c2a-8098-4b27-9b57-a9594645e357",
  "email": "teacher@example.com",
  "fullName": "Marat Serikovich",
  "role": "TEACHER",
  "isActive": true,
  "status": "ACTIVE",
  "classAsTeacher": null,
  "taughtSubjects": []
}
```

### Пример ответа (учитель С классом и предметами)
```json
{
  "id": "6bea0c2a-8098-4b27-9b57-a9594645e357",
  "email": "teacher@example.com",
  "fullName": "Марат Сериков",
  "role": "TEACHER",
  "isActive": true,
  "status": "ACTIVE",
  "classAsTeacher": {
    "id": "456e7890-e89b-12d3-a456-426614174000",
    "code": "5A",
    "classTeacherId": "6bea0c2a-8098-4b27-9b57-a9594645e357",
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

### Возможные ошибки
- **400 Bad Request** - пользователь не является сотрудником
- **404 Not Found** - пользователь с таким ID не найден
- **401 Unauthorized** - нет токена авторизации

---

## Важные замечания для фронтенда

### 1. Nullable поля
Многие поля могут быть `null`:
- `schoolClass` - если студент не привязан к классу
- `attendanceStats` - если у студента нет записей о посещаемости
- `classAsTeacher` - если сотрудник не является классным руководителем
- `dateOfBirth` - если дата рождения не указана
- `classTeacherId` - если у класса нет классного руководителя

**Обязательно проверяйте на null перед использованием!**

### 2. Пустые массивы
- `taughtSubjects` - может быть пустым массивом `[]`, если у учителя нет назначенных уроков в расписании

### 3. Роли сотрудников
Возможные значения `role` для сотрудников:
- `TEACHER` - учитель
- `ADMIN` - администратор
- `ADMIN_SCHEDULE` - администратор расписания
- `ADMIN_ASSESSMENT` - администратор оценок
- `SUPER_ADMIN` - супер-администратор

### 4. Формат даты
Дата рождения приходит в формате ISO 8601: `"2010-09-15"` (YYYY-MM-DD)

### 5. Процент посещаемости
`attendanceRate` - это число от 0 до 100 (с двумя знаками после запятой)
- Рассчитывается как: `(present + late) / totalLessons * 100`
- Например: `91.67` означает 91.67%

### 6. Коды классов
`code` в `ClassDto` - это короткий код класса (до 3 символов):
- `"5A"`, `"7B"`, `"11Г"` и т.д.

### 7. Язык обучения
`langType` может быть:
- `"RUS"` - русский
- `"KAZ"` - казахский
- `"ENG"` - английский

---

## Примеры использования в React/TypeScript

### Получение карточки студента
```typescript
const getStudentCard = async (studentId: string) => {
  const response = await fetch(
    `http://localhost:8080/api/users/student/${studentId}/card`,
    {
      headers: {
        'Authorization': `Bearer ${accessToken}`
      }
    }
  );

  if (!response.ok) {
    throw new Error('Failed to fetch student card');
  }

  const data: StudentCardDto = await response.json();
  return data;
};

// Использование
const card = await getStudentCard('ec79d59a-18f9-43d1-a9ef-5b8725cfff3b');

// Проверка на null
if (card.schoolClass) {
  console.log('Класс:', card.schoolClass.code);
}

if (card.attendanceStats) {
  console.log('Посещаемость:', card.attendanceStats.attendanceRate + '%');
}
```

### Получение карточки сотрудника
```typescript
const getStaffCard = async (staffId: string) => {
  const response = await fetch(
    `http://localhost:8080/api/users/staff/${staffId}/card`,
    {
      headers: {
        'Authorization': `Bearer ${accessToken}`
      }
    }
  );

  if (!response.ok) {
    throw new Error('Failed to fetch staff card');
  }

  const data: StaffCardDto = await response.json();
  return data;
};

// Использование
const card = await getStaffCard('6bea0c2a-8098-4b27-9b57-a9594645e357');

// Проверка на null и пустые массивы
if (card.classAsTeacher) {
  console.log('Классный руководитель класса:', card.classAsTeacher.code);
}

if (card.taughtSubjects.length > 0) {
  console.log('Преподает предметы:', card.taughtSubjects.map(s => s.subjectName).join(', '));
}
```

---

## TypeScript интерфейсы (готовые к копированию)

```typescript
// Карточка студента
export interface StudentCardDto {
  id: string;
  email: string;
  fullName: string;
  dateOfBirth: string | null;
  isActive: boolean;
  status: string;
  schoolClass: ClassDto | null;
  attendanceStats: AttendanceStatsDto | null;
}

// Карточка сотрудника
export interface StaffCardDto {
  id: string;
  email: string;
  fullName: string;
  role: string;
  isActive: boolean;
  status: string;
  classAsTeacher: ClassDto | null;
  taughtSubjects: TaughtSubjectDto[];
}

// Класс
export interface ClassDto {
  id: string;
  code: string;
  classTeacherId: string | null;
  langType: string;
}

// Статистика посещаемости
export interface AttendanceStatsDto {
  totalLessons: number;
  present: number;
  late: number;
  absent: number;
  excused: number;
  attendanceRate: number;
}

// Преподаваемый предмет
export interface TaughtSubjectDto {
  subjectId: string;
  subjectName: string;
  groups: string[];
}
```

---

## Тестовые данные

### Тестовый студент
- **ID**: `ec79d59a-18f9-43d1-a9ef-5b8725cfff3b`
- **Email**: `student@example.com`
- **Password**: `password123`

### Тестовый учитель
- **ID**: `6bea0c2a-8098-4b27-9b57-a9594645e357`
- **Email**: `teacher@example.com`
- **Password**: `password123`

### Админ для тестов
- **Email**: `admin@local`
- **Password**: `admin123`
