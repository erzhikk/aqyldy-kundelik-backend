-- Предметы (минимум)
create table if not exists subject (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null,
  school_id uuid not null references educational_institution(id) on delete restrict,
  name text not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- Кабинеты
create table if not exists room (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null,
  school_id uuid not null references educational_institution(id) on delete restrict,
  name text not null,
  capacity int,
  unique (school_id, name)
);

-- Уроки
create table if not exists timetable_lesson (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null,
  group_id uuid not null references class_group(id) on delete cascade,
  subject_id uuid not null references subject(id) on delete restrict,
  teacher_id uuid not null references app_user(id) on delete restrict,
  room_id uuid references room(id) on delete set null,
  weekday smallint not null check (weekday between 1 and 7),
  start_time time not null,
  end_time time not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (group_id, weekday, start_time, end_time)
);
