create table if not exists school_class (
    id uuid primary key default gen_random_uuid(),
    code varchar(3) not null unique,
    class_teacher_id uuid references app_user(id) on delete set null,
    lang_type varchar(3) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_school_class_code on school_class(code);
create index idx_school_class_lang_type on school_class(lang_type);
create index idx_school_class_teacher on school_class(class_teacher_id);
