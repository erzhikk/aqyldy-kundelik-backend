create table if not exists ai_generated_content (
    id uuid primary key,
    student_id uuid not null,
    attempt_id uuid null,
    topic_id uuid null,
    type varchar(32) not null,
    prompt_hash varchar(64) not null,
    content text not null,
    model varchar(128) null,
    provider varchar(64) null,
    cached_until timestamptz null,
    created_at timestamptz not null default now(),
    input_tokens int null,
    output_tokens int null
);

create unique index if not exists ux_ai_generated_content_student_type_hash
    on ai_generated_content (student_id, type, prompt_hash);

create index if not exists idx_ai_generated_content_student_created_at
    on ai_generated_content (student_id, created_at desc);

create index if not exists idx_ai_generated_content_attempt_id
    on ai_generated_content (attempt_id);

create index if not exists idx_ai_generated_content_topic_id
    on ai_generated_content (topic_id);
