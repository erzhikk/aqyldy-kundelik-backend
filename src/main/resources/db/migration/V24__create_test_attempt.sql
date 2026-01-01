-- Create test_attempt table
create table if not exists test_attempt (
    id uuid primary key default gen_random_uuid(),
    test_id uuid not null references test(id) on delete cascade,
    student_id uuid not null references app_user(id) on delete cascade,
    started_at timestamptz not null default now(),
    finished_at timestamptz,
    status text not null default 'IN_PROGRESS' check (status in ('IN_PROGRESS', 'SUBMITTED', 'GRADED')),
    score int not null default 0,
    percent numeric(5, 2) not null default 0
);

-- Add index on test_id for performance
create index if not exists idx_test_attempt_test_id on test_attempt(test_id);

-- Add index on student_id for performance
create index if not exists idx_test_attempt_student_id on test_attempt(student_id);

-- Add index on status for filtering
create index if not exists idx_test_attempt_status on test_attempt(status);

-- Add composite index for student's attempts on a specific test
create index if not exists idx_test_attempt_student_test on test_attempt(student_id, test_id);
