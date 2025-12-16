-- Drop indexes on old column
drop index if exists idx_subject_class_level;

-- Add new column class_level_id as UUID
alter table subject add column if not exists class_level_id uuid;

-- For existing data: map old class_level (int) to class_level_id (uuid)
-- This assumes class_level records exist for levels 1-11
update subject s
set class_level_id = (
    select id from class_level cl where cl.level = s.class_level
)
where class_level_id is null and class_level is not null;

-- Make class_level_id NOT NULL after data migration
alter table subject alter column class_level_id set not null;

-- Add foreign key constraint
alter table subject add constraint fk_subject_class_level
    foreign key (class_level_id) references class_level(id) on delete restrict;

-- Drop old class_level column
alter table subject drop column if exists class_level;

-- Create index on new column
create index idx_subject_class_level_id on subject(class_level_id);
