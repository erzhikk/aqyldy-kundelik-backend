-- Create test_question junction table
create table if not exists test_question (
    test_id uuid not null references test(id) on delete cascade,
    question_id uuid not null references question(id) on delete cascade,
    "order" int not null default 0,
    weight int not null default 1,
    primary key (test_id, question_id)
);

-- Add index on test_id for performance
create index if not exists idx_test_question_test_id on test_question(test_id);

-- Add index on question_id for reverse lookups
create index if not exists idx_test_question_question_id on test_question(question_id);

-- Add index on (test_id, order) for ordering questions in test
create index if not exists idx_test_question_test_order on test_question(test_id, "order");
