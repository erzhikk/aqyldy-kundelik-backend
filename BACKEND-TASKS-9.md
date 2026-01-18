# Backend Task — Days per week (5/6) stored in `class_level`

## Goal
Add an editable setting **Days per week** (5 or 6) for each class level and expose it via the existing Curriculum/Admin API so the frontend can read/update it from `/app/admin/curriculum`.

**Important:** Do not implement schedule, slot validation, draft/active in this task. Only the days-per-week setting.

---

## Scope
1) Database: add `days_per_week` to `class_level`
2) Backend model/entity: map the new field
3) API: return and update the field
4) Validation: only 5 or 6
5) Minimal tests

---

## 1) Database (Flyway migration)
### 1.1 Add column
- Table: `public.class_level`
- Column: `days_per_week` (integer)
- Default: `5`
- Not null: `true`
- Constraint: only `5` or `6`

### 1.2 Backfill
- With default `5`, existing rows get a valid value automatically.

### 1.3 Migration safety
- Make migration safe if it runs on a DB where the column already exists (avoid failure).
- Ensure constraint name does not conflict if re-applied.

---

## 2) Backend entity/model
Update `ClassLevelEntity` (or equivalent) to include:
- `daysPerWeek` (Int)

---

## 3) API contract updates

### 3.1 List levels endpoint
Wherever backend returns class levels for curriculum UI (typically):
- `GET /api/admin/curriculum/levels`

Add field:
- `daysPerWeek: 5 | 6`

### 3.2 Level details endpoint (optional but recommended)
If the frontend loads subjects for a level via:
- `GET /api/admin/curriculum/levels/{classLevelId}/subjects`

Include `daysPerWeek` in this response too, so FE doesn’t need to cross-reference the levels list.
If you prefer not to change this response, FE will fallback to `/levels`, but including it here makes FE simpler.

### 3.3 Update endpoint
Extend the existing “update curriculum for a level” endpoint (typically a PUT to the same `/subjects` endpoint) to accept an optional field:
- `daysPerWeek`

Rules:
- If `daysPerWeek` is provided → update `class_level.days_per_week`
- If not provided → do not change it (backward compatibility)

Response:
- Return the updated value in the response DTO.

---

## 4) Validation
- Accept only 5 or 6.
- On invalid value return HTTP 400 with a clear error message key (e.g., `invalidDaysPerWeek`).

---

## 5) Tests (minimal)
- Reading: `/levels` includes correct `daysPerWeek`.
- Updating: set 6 → persisted; set 7 → 400.

---

## Acceptance Criteria
- DB has `class_level.days_per_week` with default 5 and enforced 5/6 constraint
- API returns `daysPerWeek` for levels
- API updates `daysPerWeek` for a given level (without breaking old clients)
- Invalid values are rejected with 400
