-- Create topic table
create table if not exists topic (
    id uuid primary key default gen_random_uuid(),
    subject_id uuid not null references subject(id) on delete cascade,
    name text not null,
    description text,
    created_at timestamptz not null default now()
);

-- Add index on subject_id for performance
create index if not exists idx_topic_subject_id on topic(subject_id);
