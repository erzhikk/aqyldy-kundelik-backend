create extension if not exists pgcrypto;

create table if not exists educational_institution (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null,
  name text not null,
  type text not null check (type in ('SCHOOL','UNIVERSITY')),
  tenant_key text not null unique,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists app_user (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null,
  school_id uuid references educational_institution(id) on delete restrict,
  email text not null unique,
  full_name text not null,
  role text not null check (role in ('STUDENT','PARENT','TEACHER','ADMIN','ADMIN_SCHEDULE','ADMIN_ASSESSMENT','SUPER_ADMIN')),
  status text not null default 'ACTIVE' check (status in ('ACTIVE','INACTIVE')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists class_group (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null,
  school_id uuid not null references educational_institution(id) on delete restrict,
  name text not null,
  year int,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (school_id, name, year)
);
