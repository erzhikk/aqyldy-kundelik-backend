-- Create test table
create table if not exists test (
    id uuid primary key default gen_random_uuid(),
    subject_id uuid not null references subject(id) on delete cascade,
    class_level_id uuid references class_level(id) on delete restrict,
    name text not null,
    description text,
    duration_sec int,
    max_score int not null default 0,
    status text not null default 'DRAFT' check (status in ('DRAFT', 'PUBLISHED')),
    shuffle_questions bool not null default true,
    shuffle_choices bool not null default true,
    allowed_attempts int,
    opens_at timestamptz,
    closes_at timestamptz,
    passing_percent numeric(5, 2),
    review_policy text check (review_policy in ('NEVER', 'AFTER_SUBMIT', 'AFTER_CLOSE_WINDOW')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-- Add index on subject_id for performance
create index if not exists idx_test_subject_id on test(subject_id);

-- Add index on class_level_id
create index if not exists idx_test_class_level_id on test(class_level_id);

-- Add index on status for filtering
create index if not exists idx_test_status on test(status);

-- Add index on opens_at and closes_at for window queries
create index if not exists idx_test_opens_at on test(opens_at);
create index if not exists idx_test_closes_at on test(closes_at);
