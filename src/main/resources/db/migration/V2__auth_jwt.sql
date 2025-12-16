alter table app_user
  add column if not exists password_hash text not null default '';

create table if not exists refresh_token (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references app_user(id) on delete cascade,
  token_hash text not null unique,
  expires_at timestamptz not null,
  revoked boolean not null default false,
  created_at timestamptz not null default now()
);
create index if not exists idx_refresh_user_valid on refresh_token(user_id, revoked, expires_at);
