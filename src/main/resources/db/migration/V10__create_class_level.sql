create table if not exists class_level (
    id uuid primary key default gen_random_uuid(),
    level int not null unique,
    name_ru text not null,
    name_kk text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_class_level_level on class_level(level);

-- Insert default class levels (1-11)
insert into class_level (level, name_ru, name_kk) values
    (1, '1 класс', '1 сынып'),
    (2, '2 класс', '2 сынып'),
    (3, '3 класс', '3 сынып'),
    (4, '4 класс', '4 сынып'),
    (5, '5 класс', '5 сынып'),
    (6, '6 класс', '6 сынып'),
    (7, '7 класс', '7 сынып'),
    (8, '8 класс', '8 сынып'),
    (9, '9 класс', '9 сынып'),
    (10, '10 класс', '10 сынып'),
    (11, '11 класс', '11 сынып')
on conflict (level) do nothing;
