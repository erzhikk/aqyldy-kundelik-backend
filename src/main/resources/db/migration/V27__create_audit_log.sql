-- Create audit_log table for tracking important events

create table if not exists audit_log (
    id uuid primary key default gen_random_uuid(),
    event_type varchar(50) not null,  -- e.g., 'TEST_CREATED', 'TEST_PUBLISHED', 'ATTEMPT_COMPLETED'
    entity_type varchar(50) not null,  -- e.g., 'TEST', 'ATTEMPT'
    entity_id uuid not null,
    user_id uuid references app_user(id) on delete set null,
    metadata jsonb,  -- Additional data as JSON
    created_at timestamptz not null default now()
);

-- Index on event_type for filtering by event type
create index if not exists idx_audit_log_event_type on audit_log(event_type);

-- Index on entity_type and entity_id for finding logs for specific entity
create index if not exists idx_audit_log_entity on audit_log(entity_type, entity_id);

-- Index on user_id for finding actions by specific user
create index if not exists idx_audit_log_user_id on audit_log(user_id);

-- Index on created_at for date range queries
create index if not exists idx_audit_log_created_at on audit_log(created_at);

-- Composite index for common queries (entity + date)
create index if not exists idx_audit_log_entity_created
    on audit_log(entity_type, entity_id, created_at);
