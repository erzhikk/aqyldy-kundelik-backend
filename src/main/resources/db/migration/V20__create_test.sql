-- Create test table
create table if not exists test (
    id uuid primary key default gen_random_uuid(),
    subject_id uuid not null references subject(id) on delete cascade,
    name text not null,
    grade int,
    duration_sec int,
    max_score int,
    is_published bool not null default false,
    shuffle_questions bool not null default true,
    shuffle_choices bool not null default true,
    allowed_attempts int,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-- Add index on subject_id for performance
create index if not exists idx_test_subject_id on test(subject_id);

-- Add index on is_published for filtering published tests
create index if not exists idx_test_is_published on test(is_published);
