-- Curriculum subject hours: how many hours per week for each subject in each class level
create table if not exists curriculum_subject_hours (
    id uuid primary key default gen_random_uuid(),
    class_level_id uuid not null references class_level(id) on delete cascade,
    subject_id uuid not null references subject(id) on delete cascade,
    hours_per_week int not null check (hours_per_week >= 0 and hours_per_week <= 12),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (class_level_id, subject_id)
);

create index idx_curriculum_subject_hours_class_level on curriculum_subject_hours(class_level_id);
create index idx_curriculum_subject_hours_subject on curriculum_subject_hours(subject_id);

-- Class subject teacher: which teacher teaches which subject in which class
create table if not exists class_subject_teacher (
    id uuid primary key default gen_random_uuid(),
    class_id uuid not null references school_class(id) on delete cascade,
    subject_id uuid not null references subject(id) on delete cascade,
    teacher_id uuid not null references app_user(id) on delete cascade,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (class_id, subject_id)
);

create index idx_class_subject_teacher_class on class_subject_teacher(class_id);
create index idx_class_subject_teacher_subject on class_subject_teacher(subject_id);
create index idx_class_subject_teacher_teacher on class_subject_teacher(teacher_id);

-- Class schedule: one schedule per class (MVP - one active per class)
create table if not exists class_schedule (
    id uuid primary key default gen_random_uuid(),
    class_id uuid not null references school_class(id) on delete cascade unique,
    status varchar(16) not null default 'DRAFT' check (status in ('DRAFT', 'ACTIVE')),
    days_per_week int not null default 5 check (days_per_week in (5, 6)),
    lessons_per_day int not null default 7 check (lessons_per_day >= 1 and lessons_per_day <= 10),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_class_schedule_class on class_schedule(class_id);
create index idx_class_schedule_status on class_schedule(status);

-- Class schedule lesson: individual lesson slots in the schedule
create table if not exists class_schedule_lesson (
    id uuid primary key default gen_random_uuid(),
    schedule_id uuid not null references class_schedule(id) on delete cascade,
    day_of_week int not null check (day_of_week >= 1 and day_of_week <= 7),
    lesson_number int not null check (lesson_number >= 1),
    subject_id uuid references subject(id) on delete set null,
    teacher_id uuid references app_user(id) on delete set null,
    unique (schedule_id, day_of_week, lesson_number)
);

create index idx_class_schedule_lesson_schedule on class_schedule_lesson(schedule_id);
create index idx_class_schedule_lesson_teacher on class_schedule_lesson(teacher_id);
create index idx_class_schedule_lesson_slot on class_schedule_lesson(day_of_week, lesson_number);
