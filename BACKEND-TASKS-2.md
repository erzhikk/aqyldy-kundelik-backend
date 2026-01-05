# Backend Tasks: Topics inside Subject Card (CRUD + CreatedBy fullName)

Goal:
Implement full Topic management for Assessment question bank, but surfaced through Subject card on frontend.
Topics are created/edited/deleted in context of a Subject (subjectId).
Frontend must see Topic author fullName in the Topics list.

Repo context (already exists):
- TopicEntity: src/main/kotlin/kz/aqyldykundelik/assessment/domain/TopicEntity.kt
- TopicService: src/main/kotlin/kz/aqyldykundelik/assessment/service/TopicService.kt
- TopicRepository: src/main/kotlin/kz/aqyldykundelik/assessment/repo/AssessmentRepositories.kt
- AssessmentController has endpoints:
    - POST /api/assess/topics
    - GET  /api/assess/topics?subjectId=&q=

Missing:
- createdBy (author) fields in Topic
- update/delete endpoints
- topic list payload must include createdByFullName (and optionally createdAt, questionsCount)
- safe delete rule (cannot delete topic if there are questions)

---

## 1) DB Migration

Create new Flyway migration (next available version in your repo), for example:
- src/main/resources/db/migration/V__topic_created_by.sql

Changes:
1) Add column `created_by_user_id` to `topic` table.
    - type: uuid
    - nullable initially (to avoid breaking existing data)
2) Optional but recommended: add `updated_at`, `updated_by_user_id` (can be postponed)
3) Add index on `subject_id`, and optionally on `created_by_user_id`.

Example SQL (adjust naming to match your conventions):

```sql
ALTER TABLE topic
  ADD COLUMN IF NOT EXISTS created_by_user_id uuid;

CREATE INDEX IF NOT EXISTS idx_topic_subject_id ON topic(subject_id);
CREATE INDEX IF NOT EXISTS idx_topic_created_by ON topic(created_by_user_id);
```

Do NOT drop existing `created_at` column; keep it.

---

## 2) Entity changes: TopicEntity

File:
- src/main/kotlin/kz/aqyldykundelik/assessment/domain/TopicEntity.kt

Add field:
- createdByUserId: UUID? mapped to `created_by_user_id`

Keep existing createdAt behavior.

Example:

```kotlin
@Column(name = "created_by_user_id")
var createdByUserId: UUID? = null
```

---

## 3) DTO changes: TopicDto

File:
- src/main/kotlin/kz/aqyldykundelik/assessment/api/dto/AssessmentDtos.kt

Update TopicDto to include author fullName:

Add fields:
- createdByFullName: String?  (required by frontend)
  Optional:
- createdAt: OffsetDateTime?
- questionsCount: Long?  (nice to show and needed to enforce delete UX)

Example:

```kotlin
data class TopicDto(
  val id: UUID,
  val subjectId: UUID,
  val name: String,
  val description: String?,
  val createdByFullName: String?,
  val createdAt: OffsetDateTime?,
  val questionsCount: Long?
)
```

Also create DTOs for update:
- UpdateTopicDto(name, description)

---

## 4) Current user id helper (reuse pattern)

There is already a helper pattern in:
- assessment/service/TestService.kt: getCurrentUserId() reads SecurityContextHolder auth.name -> UUID

Implement the same approach in TopicService OR (better) create a reusable util:
- kz/aqyldykundelik/security/SecurityUtils.kt
    - fun currentUserIdOrNull(): UUID?

But simplest acceptable: implement private getCurrentUserId() inside TopicService.

Assumption:
- authentication.name contains UUID string of current user (as in TestService).

---

## 5) Repository: topic list with author name and questions count (avoid N+1)

We need to return TopicDto list with:
- author fullName (from app_user.full_name)
- questionsCount (count(question.id) by topic)

Implement one of:

### Option A (recommended): Projection + JPQL

Add projection interface in assessment/repo package:

```kotlin
interface TopicListProjection {
  fun getId(): UUID
  fun getSubjectId(): UUID
  fun getName(): String
  fun getDescription(): String?
  fun getCreatedAt(): OffsetDateTime?
  fun getCreatedByFullName(): String?
  fun getQuestionsCount(): Long
}
```

In TopicRepository add query (JPQL with joins by id fields):

```kotlin
@Query("""
  SELECT 
    t.id as id,
    t.subjectId as subjectId,
    t.name as name,
    t.description as description,
    t.createdAt as createdAt,
    u.fullName as createdByFullName,
    COUNT(q.id) as questionsCount
  FROM TopicEntity t
  LEFT JOIN UserEntity u ON u.id = t.createdByUserId
  LEFT JOIN QuestionEntity q ON q.topicId = t.id
  WHERE (:subjectId IS NULL OR t.subjectId = :subjectId)
    AND (:q IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :q, '%')))
  GROUP BY t.id, t.subjectId, t.name, t.description, t.createdAt, u.fullName
  ORDER BY t.name
""")
fun findTopicList(subjectId: UUID?, q: String?): List<TopicListProjection>
```

### Option B: Native SQL projection
If JPQL join causes issues, use native SQL and a projection interface.

---

## 6) TopicService: CRUD + delete guard

File:
- assessment/service/TopicService.kt

### Create
When creating topic:
- set createdByUserId = currentUserIdOrNull()
- save
- return TopicDto including createdByFullName, questionsCount=0

### Update
Add method:
- update(topicId, dto)
  Rules:
- topic exists else 404
- update name/description
- (optional) update modifiedAt

### Delete
Add method:
- delete(topicId)
  Rules:
- if topic not found => 404
- if questionsCount > 0 => throw 409 CONFLICT with message "Topic has questions"
- else delete topic

To check questions count:
- add method to QuestionRepository: countByTopicId(topicId): Long
  or use the projection list query + fetch one.

Return types:
- Delete endpoint can be void (204) or return TopicDto list; prefer void.

---

## 7) Controller endpoints (AssessmentController)

File:
- assessment/api/AssessmentController.kt

Existing:
- POST /topics
- GET /topics

Add:
- PUT /topics/{id}
- DELETE /topics/{id}

Security:
Reuse existing @PreAuthorize used for topics.

Request/response:
- PUT returns TopicDto (with createdByFullName, questionsCount)
- DELETE returns 204 No Content (or 200 with message)

Also consider adding:
- GET /topics/{id} already exists in service but not in controller; if needed by frontend add it.

---

## 8) Error contract

For delete conflict:
- return HTTP 409 with reason:
    - "TOPIC_HAS_QUESTIONS" or message "Topic has questions"
      Frontend will show a snackbar with a friendly message.

---

## 9) Acceptance checks

1) GET /api/assess/topics?subjectId={subjectId}
    - returns list ordered by name
    - includes createdByFullName (nullable allowed)
    - includes questionsCount (>=0)
2) POST /api/assess/topics creates topic with createdByUserId set from security context
3) PUT updates name/description
4) DELETE:
    - if no questions: 204
    - if has questions: 409
5) No N+1 queries when listing topics (single query is preferred)

---

## 10) Optional improvements (can be postponed)

- Soft delete / archive of topics instead of blocking delete
- unique constraint: (subject_id, lower(name)) to avoid duplicates
- add updated_at / updated_by for audit
