-- Create choice table
create table if not exists choice (
    id uuid primary key default gen_random_uuid(),
    question_id uuid not null references question(id) on delete cascade,
    text text not null,
    is_correct bool not null,
    "order" int not null default 0,
    media_id uuid references media_object(id) on delete set null
);

-- Add index on question_id for performance
create index if not exists idx_choice_question_id on choice(question_id);

-- Add index on media_id for joins
create index if not exists idx_choice_media_id on choice(media_id);

-- Add index on (question_id, order) for ordering choices
create index if not exists idx_choice_question_order on choice(question_id, "order");
