# Backend Tasks: Subject -> Topic -> Questions (with Answers) + Breadcrumb support

Context (current UI target):
- There is a menu item "Subjects" showing a table of subjects.
- Subject name is a link -> opens Subject card page.
- Subject card shows subject info + Topics table.
- Topic name is a link -> opens Topic card page.
- Topic card shows topic info + Questions+Answers table (two-level: question row + its 4–5 answers).
- UI wants breadcrumbs like: Subject / Topic.

This document describes backend API + model changes required for the flow above.
Assume existing Assessment module already contains Subject/Topic/Question concepts.

---

## A) Entities & DB (verify/adjust to match current code)

### 1) Topic author (Created By)
Requirement:
- Topics list (in Subject card) must show author fullName.

Implementation:
- Add to Topic: created_by_user_id (uuid, nullable for existing rows).
- Populate on create from current user (SecurityContext) OR via JPA auditing.

DB migration:
- Add column `created_by_user_id` to `topic`.
- Add index `idx_topic_subject_id`, `idx_topic_created_by`.

### 2) Questions and answers data model
Requirement:
- Topic card shows list of questions for the topic, and for each question 4–5 answers with isCorrect flag.

If your model already has:
- QuestionEntity(topicId, text, explanation?, ...)
- AnswerOptionEntity(questionId, text, isCorrect)
  Then: reuse.

If not present yet:
- Create tables:
    - assess_question (id, topic_id, text, explanation, created_at, ...)
    - assess_answer_option (id, question_id, text, is_correct, order_index, created_at, ...)

Constraints:
- Per question: answers count must be 4 or 5 (enforce in service validation).
- For SINGLE_CHOICE: exactly 1 correct answer.

---

## B) API Endpoints (REST)

### 1) Subjects
Assume subjects already exist (CRUD).
Need at minimum:
- GET /api/assess/subjects
- GET /api/assess/subjects/{subjectId}

If Subject endpoints are in another controller/module, reuse them.

### 2) Topics (scoped by subject for UI)
UI entry points:
- Subject card needs topic list for a subject.
- Topic card needs topic details.

Implement / ensure:

1) List topics for subject
   GET /api/assess/topics?subjectId={subjectId}&q={optional}

Response: TopicListItemDto[]
Fields:
- id, subjectId, name, description?
- createdByFullName (String?)
- createdAt (optional)
- questionsCount (Long) (optional but helpful for UI + delete rules)

2) Get topic details
   GET /api/assess/topics/{topicId}

Response: TopicDetailsDto
Fields:
- id, subjectId, subjectName (optional, nice for breadcrumbs)
- name, description?
- createdByFullName (optional)
- createdAt (optional)

3) Create topic
   POST /api/assess/topics
   Body: CreateTopicDto(subjectId, name, description?)
   Return: TopicDetailsDto

Set createdByUserId = current user id.

4) Update topic
   PUT /api/assess/topics/{topicId}
   Body: UpdateTopicDto(name, description?)
   Return: TopicDetailsDto

5) Delete topic
   DELETE /api/assess/topics/{topicId}
   Rules:
- If topic has questions -> return 409 CONFLICT code TOPIC_HAS_QUESTIONS
- Else delete -> 204

---

## C) Questions + Answers API (scoped by topic)

### 1) List questions for topic (with answers)
GET /api/assess/topics/{topicId}/questions?search={optional}&page=&size=&sort=

Return: Page<QuestionWithAnswersDto> (or simple list if you don't use pagination yet).
Each item:
- questionId
- text
- explanation? (optional)
- answers: AnswerDto[]  (length 4 or 5)
    - answerId, text, isCorrect, orderIndex

This endpoint is the backbone of the two-level table on the frontend.

### 2) Create question (with answers)
POST /api/assess/topics/{topicId}/questions
Body: CreateQuestionDto
- text (required)
- explanation? (optional)
- answers: List<CreateAnswerDto> (length 4 or 5)
    - text, isCorrect

Validation:
- answers size in [4,5]
- exactly one correct (for SINGLE_CHOICE)
  Return: QuestionWithAnswersDto

### 3) Update question (with answers)
PUT /api/assess/questions/{questionId}
Body: UpdateQuestionDto
- text, explanation?
- answers: List<UpsertAnswerDto>
    - id? (optional for new)
    - text
    - isCorrect
    - orderIndex
      Return: QuestionWithAnswersDto

### 4) Delete question
DELETE /api/assess/questions/{questionId}
Rules:
- If question is attached to any PUBLISHED test -> 409 QUESTION_IN_PUBLISHED_TEST
- Else delete (or archive).

(If you don't have tests linkage yet, implement simple delete now; but add TODO for the rule.)

---

## D) DTOs (Kotlin)

Create/extend DTO files in Assessment module (keep consistency with existing dto package):

- TopicListItemDto
- TopicDetailsDto
- CreateTopicDto / UpdateTopicDto

- AnswerDto / CreateAnswerDto / UpsertAnswerDto
- QuestionWithAnswersDto
- CreateQuestionDto / UpdateQuestionDto

Keep JSON friendly names and stable contract; frontend will rely on it.

---

## E) Current user resolution (CreatedBy)
Preferred:
- Implement SecurityUtils.currentUserIdOrNull() based on SecurityContextHolder.
- OR use JPA Auditing with AuditorAware<UUID> to populate @CreatedBy.

Minimal acceptable:
- In TopicService.create: set topic.createdByUserId = UUID.fromString(authentication.name)

---

## F) Performance / N+1
For topics list:
- Use single query with LEFT JOIN to users and COUNT(questions)
- Return projections -> map to DTO.

For questions list:
- Fetch questions + answers efficiently:
    - Option A: two queries (questions page + answers by questionIds) then group in service.
    - Option B: native query and manual mapping.
      Avoid per-question queries.

---

## G) Acceptance / test checklist

1) Subjects list works (existing).
2) Subject card:
    - calls GET topics?subjectId
    - sees createdByFullName filled for new topics.
3) Topic card:
    - calls GET /topics/{id} for header + breadcrumbs data
    - calls GET /topics/{id}/questions to render table
4) Create/Update question enforces 4–5 answers and 1 correct.
5) Delete topic with questions returns 409 TOPIC_HAS_QUESTIONS.
