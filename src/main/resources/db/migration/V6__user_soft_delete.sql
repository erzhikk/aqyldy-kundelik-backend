-- Add is_active and is_deleted columns to app_user table
alter table app_user
  add column if not exists is_active boolean not null default true,
  add column if not exists is_deleted boolean not null default false;

-- Add index for better query performance on is_deleted
create index if not exists idx_app_user_is_deleted on app_user(is_deleted);
