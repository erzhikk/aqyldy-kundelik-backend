-- Rename name column to name_ru and add new columns
alter table subject rename column name to name_ru;

alter table subject add column if not exists name_kk text not null default '';
alter table subject add column if not exists class_level int not null default 1;

-- Remove default values after migration
alter table subject alter column name_kk drop default;
alter table subject alter column class_level drop default;

-- Add index for performance
create index if not exists idx_subject_class_level on subject(class_level);
create index if not exists idx_subject_name_ru on subject(lower(name_ru));
create index if not exists idx_subject_name_kk on subject(lower(name_kk));
