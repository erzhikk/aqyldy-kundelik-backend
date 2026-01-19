# Backend Task — Enforce activation rules (PLAN_EXCEEDS_SLOTS, TEACHER_BUSY) + unify policy source

## Goal
Make schedule activation **safe and deterministic**:
- A schedule can be saved as **DRAFT** even with issues.
- Activation to **ACTIVE** must be **blocked** if there are critical conflicts:
    - `PLAN_EXCEEDS_SLOTS` (totalHoursPerWeek > daysPerWeek * maxLessonsPerDay)
    - `TEACHER_BUSY` (teacher assigned in two classes at same day/lesson)
    - (optional) `INVALID_SLOT_RANGE` (lessonNumber/dayOfWeek outside policy)

Also reduce “split-brain” by unifying schedule policy with `class_level`:
- `daysPerWeek` and `maxLessonsPerDay` live in `class_level` and are the authoritative source for schedule grid boundaries.

This task is for the backend Claude session.

---

## Current situation (from code review)
- Curriculum PUT already saves `daysPerWeek` and calculates warnings.
- Schedule activation currently flips status to ACTIVE without validating conflicts.
- Frontend may show “activate anyway” confirm, so backend must become the final gatekeeper.

---

## 1) Conflict model (backend)
### 1.1 Add/ensure conflict types
Backend should provide conflicts in schedule responses with clear `type` values:

**Critical (block activation)**
- `PLAN_EXCEEDS_SLOTS`
- `TEACHER_BUSY`
- `INVALID_SLOT_RANGE` (optional but recommended)

**Non-critical (warnings)**
- `HOURS_MISMATCH` (if you already have it)
- any other UI hints

Define which ones are **critical** centrally (e.g., `ConflictSeverity` or `isCritical()` mapping).

### 1.2 Conflict DTO contract (must match frontend)
Schedule responses must include:
- `conflicts: ScheduleConflictDto[]`

Where `ScheduleConflictDto` includes at minimum:
- `type: string`
- `message: string`
- optional fields (only if relevant): `teacherId`, `dayOfWeek`, `lessonNumber`, `subjectId`

---

## 2) Policy source of truth: class_level
### 2.1 Ensure class_level has:
- `days_per_week` (5/6)
- `max_lessons_per_day` (existing in your project)

### 2.2 Schedule entity fields
If `class_schedule` currently stores `daysPerWeek` / `lessonsPerDay`:
- Keep for now (to avoid big refactor), but **treat them as derived** from `class_level`.
- On schedule load / creation, sync:
    - `schedule.daysPerWeek = classLevel.daysPerWeek`
    - `schedule.lessonsPerDay = classLevel.maxLessonsPerDay`

### 2.3 Update schedule API input
If the schedule PUT currently accepts `daysPerWeek/lessonsPerDay`:
- Backend should **ignore** client-provided values and use `class_level` values.
- Alternatively, validate equality and reject mismatch (400). MVP recommendation: ignore and return authoritative policy in response.

---

## 3) Compute PLAN_EXCEEDS_SLOTS conflict
### 3.1 Calculation
For class’s `class_level_id`:
- `totalHoursPerWeek = sum(curriculum_subject_hours.hours_per_week for that class_level)`
- `availableSlotsPerWeek = classLevel.daysPerWeek * classLevel.maxLessonsPerDay`

If `totalHoursPerWeek > availableSlotsPerWeek`:
- add conflict `{type: "PLAN_EXCEEDS_SLOTS", ...}` as **critical**
- message example:
    - "Учебный план: {totalHoursPerWeek} ч/нед, доступно слотов: {availableSlotsPerWeek}. План не помещается."

### 3.2 Where to expose
- Include this conflict in:
    - `GET /api/admin/schedule/classes/{classId}`
    - `PUT /api/admin/schedule/classes/{classId}`
    - activation response/error body (see below)

This ensures the schedule screen always shows the reason why activation is blocked.

---

## 4) Activation endpoint behavior
### 4.1 Endpoint
Existing:
- `POST /api/admin/schedule/classes/{classId}/activate`

### 4.2 New behavior (required)
On activate:
1) Load schedule (or create draft if missing)
2) Sync policy from `class_level` (days/week + lessons/day)
3) Recompute conflicts:
    - `PLAN_EXCEEDS_SLOTS`
    - `TEACHER_BUSY`
    - optional `INVALID_SLOT_RANGE`
4) If there are **critical** conflicts:
    - return HTTP **409 Conflict**
    - body should match the same `ClassScheduleResponse` shape (or a dedicated error DTO) containing `conflicts`
    - do **not** change status
5) If no critical conflicts:
    - set `status=ACTIVE`
    - return 200 with updated schedule response

> Why 409: Frontend can treat it as “can’t activate because of conflicts”.

---

## 5) TEACHER_BUSY should be treated as critical
Ensure teacher-busy conflicts are included and marked critical for activation.
If you already detect it, reuse the same detection.

---

## 6) INVALID_SLOT_RANGE (optional but recommended)
If saved lessons contain:
- `dayOfWeek > classLevel.daysPerWeek`
- `lessonNumber > classLevel.maxLessonsPerDay`
  Add a critical conflict and block activation.

---

## 7) Tests (minimal)
Add integration tests for activation:
1) When `totalHours > slots` → activate returns 409 and includes `PLAN_EXCEEDS_SLOTS`
2) When teacher busy conflict exists → activate returns 409 and includes `TEACHER_BUSY`
3) When no critical conflicts → activate returns 200 and status becomes ACTIVE

Also test policy sync:
- changing `class_level.daysPerWeek` affects returned schedule policy.

---

## Acceptance Criteria
- Backend is the final authority: activation cannot be forced if conflicts exist
- `PLAN_EXCEEDS_SLOTS` conflict appears on schedule endpoints
- Activation returns 409 with conflicts when blocked
- Schedule policy (days/week, lessons/day) is consistent with `class_level` and returned to frontend
