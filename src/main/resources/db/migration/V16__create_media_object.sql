-- Create media_object table for storing metadata about uploaded files
create table if not exists media_object (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references app_user(id) on delete cascade,
  s3_key text not null unique,
  content_type text not null,
  file_size bigint,
  status text not null default 'UPLOADING' check (status in ('UPLOADING','CONFIRMED','DELETED')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- Add index on user_id for performance
create index if not exists idx_media_object_user_id on media_object(user_id);

-- Add index on status for queries filtering by status
create index if not exists idx_media_object_status on media_object(status);

-- Add photo_media_id column to app_user table
alter table app_user add column if not exists photo_media_id uuid;

-- Add foreign key constraint to media_object table
alter table app_user add constraint fk_user_photo_media
    foreign key (photo_media_id) references media_object(id) on delete set null;

-- Create index for performance
create index if not exists idx_user_photo_media_id on app_user(photo_media_id);
