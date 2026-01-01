-- Add indexes for analytics performance

-- Index on finished_at for date range filtering in student analytics
create index if not exists idx_test_attempt_finished_at on test_attempt(finished_at);

-- Composite index on (test_id, status) for faster filtering
create index if not exists idx_test_attempt_test_status on test_attempt(test_id, status);

-- Composite index on (student_id, status, finished_at) for student analytics with date filtering
create index if not exists idx_test_attempt_student_status_finished
    on test_attempt(student_id, status, finished_at);

-- Index on question.topic_id for grouping by topics (if not already exists)
create index if not exists idx_question_topic_id on question(topic_id);
