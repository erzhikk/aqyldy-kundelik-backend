-- Add validation fields to media_object table
alter table media_object add column if not exists width integer;
alter table media_object add column if not exists height integer;
alter table media_object add column if not exists sha256 text;

-- Drop old CHECK constraint and create new one with updated statuses
alter table media_object drop constraint if exists media_object_status_check;
alter table media_object add constraint media_object_status_check
    check (status in ('UPLOADING','READY','FAILED','DELETED'));

-- Add index on sha256 for duplicate detection
create index if not exists idx_media_object_sha256 on media_object(sha256);
