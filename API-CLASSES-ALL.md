# API эндпоинт: Получить все классы без пагинации

## Новый эндпоинт

### GET `/api/classes/all`

Возвращает **полный список всех классов** без пагинации.

---

## Сравнение с существующим эндпоинтом

### ❌ Старый (с пагинацией): `GET /api/classes`

**Ответ:**
```json
{
  "content": [
    {
      "id": "b36a63cc-f61d-4d5b-9d7b-f17919a541f2",
      "code": "11A",
      "classTeacherId": null,
      "langType": "RUS"
    },
    ...
  ],
  "page": 0,
  "size": 20,
  "totalElements": 5,
  "totalPages": 1
}
```

Возвращает **объект PageDto** с метаданными пагинации.

---

### ✅ Новый (БЕЗ пагинации): `GET /api/classes/all`

**Ответ:**
```json
[
  {
    "id": "b36a63cc-f61d-4d5b-9d7b-f17919a541f2",
    "code": "11A",
    "classTeacherId": null,
    "langType": "RUS"
  },
  {
    "id": "04234e4e-f806-4138-8d8a-a044661b1bd9",
    "code": "1A",
    "classTeacherId": "e75a6950-84bd-4cc6-8e96-3f30478cceca",
    "langType": "kaz"
  },
  {
    "id": "58c968e6-1567-49d6-bd42-945a11cbc9b2",
    "code": "1B",
    "classTeacherId": "e75a6950-84bd-4cc6-8e96-3f30478cceca",
    "langType": "rus"
  },
  {
    "id": "caf109ba-9f4a-4f21-878f-a74a9b95ee41",
    "code": "5A",
    "classTeacherId": null,
    "langType": "RUS"
  },
  {
    "id": "2a5ea9f2-786f-4573-b40b-4220af75dcbd",
    "code": "7B",
    "classTeacherId": null,
    "langType": "KAZ"
  }
]
```

Возвращает **простой массив** объектов ClassDto.

---

## Детали

### Запрос

```bash
GET http://localhost:8080/api/classes/all
```

### Headers

```
Authorization: Bearer {accessToken}
```

### Query параметры

**Нет параметров** - эндпоинт всегда возвращает все классы.

### Response

**Тип:** `Array<ClassDto>`

**Статус:** 200 OK

---

## Структура ClassDto

```typescript
interface ClassDto {
  id: string;              // UUID класса
  code: string;            // Код класса (например: "5A", "7B", "11A")
  classTeacherId: string | null;  // UUID классного руководителя (может быть null)
  langType: string;        // Язык обучения: "RUS" | "KAZ" | "ENG" | "rus" | "kaz" | "eng"
}
```

---

## Примеры использования

### cURL

```bash
curl -X GET "http://localhost:8080/api/classes/all" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### JavaScript / TypeScript

```typescript
const getAllClasses = async (): Promise<ClassDto[]> => {
  const response = await fetch('http://localhost:8080/api/classes/all', {
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  });

  if (!response.ok) {
    throw new Error('Failed to fetch classes');
  }

  return await response.json();
};

// Использование
const classes = await getAllClasses();
console.log('Всего классов:', classes.length);
classes.forEach(cls => {
  console.log(`${cls.code} (${cls.langType})`);
});
```

### React Hook

```typescript
import { useState, useEffect } from 'react';

interface ClassDto {
  id: string;
  code: string;
  classTeacherId: string | null;
  langType: string;
}

export const useAllClasses = () => {
  const [classes, setClasses] = useState<ClassDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    const fetchClasses = async () => {
      try {
        const response = await fetch('http://localhost:8080/api/classes/all', {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
          }
        });

        if (!response.ok) {
          throw new Error('Failed to fetch classes');
        }

        const data = await response.json();
        setClasses(data);
      } catch (err) {
        setError(err as Error);
      } finally {
        setLoading(false);
      }
    };

    fetchClasses();
  }, []);

  return { classes, loading, error };
};

// Использование в компоненте
const ClassSelector = () => {
  const { classes, loading, error } = useAllClasses();

  if (loading) return <div>Загрузка...</div>;
  if (error) return <div>Ошибка: {error.message}</div>;

  return (
    <select>
      {classes.map(cls => (
        <option key={cls.id} value={cls.id}>
          {cls.code} - {cls.langType}
        </option>
      ))}
    </select>
  );
};
```

---

## Когда использовать

### ✅ Используйте `/api/classes/all` когда:

- Нужен **полный список** для dropdown/select
- Формируете фильтры или селекторы
- Небольшое количество классов (обычно < 100)
- Не нужна пагинация на фронте

### ❌ Используйте `/api/classes` (с пагинацией) когда:

- Большое количество данных
- Нужна пагинация на UI
- Нужны метаданные (totalElements, totalPages)
- Реализуете таблицу с постраничной навигацией

---

## Особенности

### Сортировка

Классы автоматически **сортируются по коду** (code) в алфавитном порядке.

**Пример порядка:**
```
1A → 1B → 5A → 7B → 11A
```

### Доступ

Эндпоинт доступен для **всех авторизованных пользователей**.

Не требуется специальных ролей.

### Performance

Если в системе более 100-200 классов, рекомендуется использовать пагинированный эндпоинт `/api/classes` вместо `/api/classes/all`.

---

## Возможные ошибки

### 401 Unauthorized

Отсутствует или невалидный токен авторизации.

**Решение:** Убедитесь что передаете валидный Bearer token в заголовке Authorization.

### 403 Forbidden

У пользователя нет доступа.

**Решение:** Проверьте что пользователь авторизован.

---

## TypeScript типы (готовые к использованию)

```typescript
// Класс
export interface ClassDto {
  id: string;
  code: string;
  classTeacherId: string | null;
  langType: string;
}

// API функция
export const getAllClasses = async (
  accessToken: string
): Promise<ClassDto[]> => {
  const response = await fetch(
    'http://localhost:8080/api/classes/all',
    {
      headers: {
        'Authorization': `Bearer ${accessToken}`
      }
    }
  );

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  return await response.json();
};
```

---

## Реальный пример ответа

```json
[
  {
    "id": "b36a63cc-f61d-4d5b-9d7b-f17919a541f2",
    "code": "11A",
    "classTeacherId": null,
    "langType": "RUS"
  },
  {
    "id": "04234e4e-f806-4138-8d8a-a044661b1bd9",
    "code": "1A",
    "classTeacherId": "e75a6950-84bd-4cc6-8e96-3f30478cceca",
    "langType": "kaz"
  },
  {
    "id": "58c968e6-1567-49d6-bd42-945a11cbc9b2",
    "code": "1B",
    "classTeacherId": "e75a6950-84bd-4cc6-8e96-3f30478cceca",
    "langType": "rus"
  },
  {
    "id": "caf109ba-9f4a-4f21-878f-a74a9b95ee41",
    "code": "5A",
    "classTeacherId": null,
    "langType": "RUS"
  },
  {
    "id": "2a5ea9f2-786f-4573-b40b-4220af75dcbd",
    "code": "7B",
    "classTeacherId": null,
    "langType": "KAZ"
  }
]
```

---

## Swagger UI

Эндпоинт также доступен через Swagger UI:

```
http://localhost:8080/swagger-ui.html
```

1. Откройте Swagger UI
2. Авторизуйтесь (кнопка "Authorize")
3. Найдите **class-controller**
4. Найдите **GET /api/classes/all**
5. Нажмите "Try it out" → "Execute"
