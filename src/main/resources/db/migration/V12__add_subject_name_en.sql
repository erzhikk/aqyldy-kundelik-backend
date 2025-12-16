-- Add name_en column to subject table
alter table subject add column if not exists name_en text not null default '';

-- Remove default after migration
alter table subject alter column name_en drop default;

-- Create index for performance
create index if not exists idx_subject_name_en on subject(lower(name_en));
