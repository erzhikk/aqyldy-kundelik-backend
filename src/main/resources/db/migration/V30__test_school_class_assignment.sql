-- Create many-to-many relationship between test and school_class
-- One test can be assigned to multiple classes (9A, 9B, 9C, etc.)

-- Create assignment table
create table if not exists test_school_class (
    id uuid primary key default gen_random_uuid(),
    test_id uuid not null references test(id) on delete cascade,
    school_class_id uuid not null references school_class(id) on delete cascade,
    assigned_at timestamptz not null default now(),
    unique (test_id, school_class_id)
);

-- Add indexes for performance
create index if not exists idx_test_school_class_test_id on test_school_class(test_id);
create index if not exists idx_test_school_class_school_class_id on test_school_class(school_class_id);

-- Remove old class_level_id column from test table (nullable, so safe to drop)
alter table test drop column if exists class_level_id;
