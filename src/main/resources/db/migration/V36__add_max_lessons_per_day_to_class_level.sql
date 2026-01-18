-- Add max_lessons_per_day column to class_level table
-- This defines the maximum number of lessons allowed per day for each class level
-- Used for curriculum validation: sum of subject hours must fit into weekly schedule

alter table class_level add column if not exists max_lessons_per_day int not null default 7;

-- Set appropriate values based on class level
-- Level 1: max 4 lessons per day
-- Levels 2-4: max 5 lessons per day
-- Levels 5-11: max 7 lessons per day

update class_level set max_lessons_per_day = 4 where level = 1;
update class_level set max_lessons_per_day = 5 where level between 2 and 4;
update class_level set max_lessons_per_day = 7 where level between 5 and 11;
update class_level set max_lessons_per_day = 7 where level = 0 or level = 12;
