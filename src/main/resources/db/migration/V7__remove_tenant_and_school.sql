-- Remove unique constraints that depend on school_id
alter table class_group drop constraint if exists class_group_school_id_name_year_key;
alter table room drop constraint if exists room_school_id_name_key;

-- Remove foreign key constraints on school_id
alter table app_user drop constraint if exists app_user_school_id_fkey;
alter table class_group drop constraint if exists class_group_school_id_fkey;
alter table subject drop constraint if exists subject_school_id_fkey;
alter table room drop constraint if exists room_school_id_fkey;

-- Drop school_id columns
alter table app_user drop column if exists school_id;
alter table class_group drop column if exists school_id;
alter table subject drop column if exists school_id;
alter table room drop column if exists school_id;

-- Drop tenant_id columns
alter table educational_institution drop column if exists tenant_id;
alter table app_user drop column if exists tenant_id;
alter table class_group drop column if exists tenant_id;
alter table subject drop column if exists tenant_id;
alter table room drop column if exists tenant_id;
alter table timetable_lesson drop column if exists tenant_id;
alter table attendance_sheet drop column if exists tenant_id;
alter table attendance_record drop column if exists tenant_id;

-- Drop educational_institution table
drop table if exists educational_institution cascade;
