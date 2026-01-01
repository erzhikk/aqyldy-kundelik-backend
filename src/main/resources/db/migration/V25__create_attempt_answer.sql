-- Create attempt_answer table
create table if not exists attempt_answer (
    id uuid primary key default gen_random_uuid(),
    attempt_id uuid not null references test_attempt(id) on delete cascade,
    question_id uuid not null references question(id) on delete cascade,
    choice_id uuid not null references choice(id) on delete cascade,
    is_correct bool,
    score_delta int not null default 0
);

-- Add index on attempt_id for performance
create index if not exists idx_attempt_answer_attempt_id on attempt_answer(attempt_id);

-- Add index on question_id for lookups
create index if not exists idx_attempt_answer_question_id on attempt_answer(question_id);

-- Add index on choice_id for lookups
create index if not exists idx_attempt_answer_choice_id on attempt_answer(choice_id);

-- Add unique constraint to prevent multiple answers to the same question in one attempt
create unique index if not exists idx_attempt_answer_unique
    on attempt_answer(attempt_id, question_id);
