-- Create question table
create table if not exists question (
    id uuid primary key default gen_random_uuid(),
    topic_id uuid not null references topic(id) on delete cascade,
    text text not null,
    media_id uuid references media_object(id) on delete set null,
    difficulty text not null default 'MEDIUM' check (difficulty in ('EASY', 'MEDIUM', 'HARD')),
    explanation text,
    created_at timestamptz not null default now()
);

-- Add index on topic_id for performance
create index if not exists idx_question_topic_id on question(topic_id);

-- Add index on difficulty for filtering
create index if not exists idx_question_difficulty on question(difficulty);

-- Add index on media_id for joins
create index if not exists idx_question_media_id on question(media_id);
