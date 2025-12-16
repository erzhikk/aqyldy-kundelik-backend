create table if not exists attendance_sheet (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null,
  lesson_date date not null,
  group_id uuid not null references class_group(id) on delete cascade,
  subject_id uuid not null references subject(id) on delete restrict,
  lesson_ref_id uuid,
  created_at timestamptz not null default now(),
  unique (lesson_date, group_id, subject_id)
);

create table if not exists attendance_record (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null,
  sheet_id uuid not null references attendance_sheet(id) on delete cascade,
  student_id uuid not null references app_user(id) on delete restrict,
  status text not null check (status in ('PRESENT','LATE','ABSENT','EXCUSED')),
  reason text,
  marked_at timestamptz not null default now(),
  marked_by uuid references app_user(id),
  unique (sheet_id, student_id)
);
