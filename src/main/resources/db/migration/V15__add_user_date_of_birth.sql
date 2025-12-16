-- Add date_of_birth column to app_user table
alter table app_user add column if not exists date_of_birth date;
