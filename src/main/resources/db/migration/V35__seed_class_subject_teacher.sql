-- Seed class subject teacher assignments
-- Maps subjects to teachers based on subject name pattern and class language type
-- Class code ending with 'A' uses KZ teachers, 'B' uses RU teachers
-- idempotent: uses ON CONFLICT DO NOTHING

-- First, create any missing teachers for reading and world_studies if needed
-- (These teachers might not exist from the original seed)

insert into app_user (email, full_name, role, status, is_active, is_deleted, password_hash)
select
    'teacher.reading.kz@demo.aqyldy.kz',
    'Оқу мұғалімі (қаз)',
    'TEACHER',
    'ACTIVE',
    true,
    false,
    '$2a$10$dummyhashdummyhashdummy'
where not exists (select 1 from app_user where email = 'teacher.reading.kz@demo.aqyldy.kz');

insert into app_user (email, full_name, role, status, is_active, is_deleted, password_hash)
select
    'teacher.reading.ru@demo.aqyldy.kz',
    'Учитель чтения (рус)',
    'TEACHER',
    'ACTIVE',
    true,
    false,
    '$2a$10$dummyhashdummyhashdummy'
where not exists (select 1 from app_user where email = 'teacher.reading.ru@demo.aqyldy.kz');

insert into app_user (email, full_name, role, status, is_active, is_deleted, password_hash)
select
    'teacher.world_studies.kz@demo.aqyldy.kz',
    'Дүниетану мұғалімі (қаз)',
    'TEACHER',
    'ACTIVE',
    true,
    false,
    '$2a$10$dummyhashdummyhashdummy'
where not exists (select 1 from app_user where email = 'teacher.world_studies.kz@demo.aqyldy.kz');

insert into app_user (email, full_name, role, status, is_active, is_deleted, password_hash)
select
    'teacher.world_studies.ru@demo.aqyldy.kz',
    'Учитель познания мира (рус)',
    'TEACHER',
    'ACTIVE',
    true,
    false,
    '$2a$10$dummyhashdummyhashdummy'
where not exists (select 1 from app_user where email = 'teacher.world_studies.ru@demo.aqyldy.kz');

-- Insert teacher assignments for each class and subject
-- Use lang_type to determine whether to use kz or ru teacher

insert into class_subject_teacher (class_id, subject_id, teacher_id)
select
    sc.id as class_id,
    s.id as subject_id,
    t.id as teacher_id
from school_class sc
join class_level cl on sc.class_level_id = cl.id
join subject s on s.class_level_id = cl.id
cross join lateral (
    select u.id
    from app_user u
    where u.role = 'TEACHER'
      and u.is_deleted = false
      and u.status = 'ACTIVE'
      and u.email like 'teacher.%@demo.aqyldy.kz'
      and (
          -- Match language based on class lang_type
          (sc.lang_type = 'KAZ' and u.email like '%.kz@%') or
          (sc.lang_type = 'RUS' and u.email like '%.ru@%') or
          -- Default to KZ for other lang types
          (sc.lang_type not in ('KAZ', 'RUS') and u.email like '%.kz@%')
      )
      and (
          -- Match subject to teacher specialty
          (lower(s.name_ru) like '%математик%' and u.email like 'teacher.math.%') or
          (lower(s.name_ru) like '%алгебр%' and u.email like 'teacher.math.%') or
          (lower(s.name_ru) like '%геометр%' and u.email like 'teacher.math.%') or
          (lower(s.name_ru) like '%казах%' and u.email like 'teacher.kazakh_lang.%') or
          (lower(s.name_ru) like '%қазақ%' and u.email like 'teacher.kazakh_lang.%') or
          (lower(s.name_ru) like '%русск%' and u.email like 'teacher.russian_lang.%') or
          (lower(s.name_ru) like '%литератур%' and u.email like 'teacher.russian_lang.%') or
          (lower(s.name_ru) like '%англий%' and u.email like 'teacher.english.%') or
          (lower(s.name_ru) like '%english%' and u.email like 'teacher.english.%') or
          (lower(s.name_ru) like '%истори%' and u.email like 'teacher.history.%') or
          (lower(s.name_ru) like '%географ%' and u.email like 'teacher.geography.%') or
          (lower(s.name_ru) like '%биолог%' and u.email like 'teacher.biology.%') or
          (lower(s.name_ru) like '%хими%' and u.email like 'teacher.chemistry.%') or
          (lower(s.name_ru) like '%физик%' and u.email like 'teacher.physics.%') or
          (lower(s.name_ru) like '%информатик%' and u.email like 'teacher.informatics.%') or
          (lower(s.name_ru) like '%физкульт%' and u.email like 'teacher.pe.%') or
          (lower(s.name_ru) like '%физическ%' and u.email like 'teacher.pe.%') or
          ((lower(s.name_ru) like '%изо%' or lower(s.name_ru) like '%искусств%' or lower(s.name_ru) like '%рисован%') and u.email like 'teacher.art.%') or
          (lower(s.name_ru) like '%музык%' and u.email like 'teacher.music.%') or
          ((lower(s.name_ru) like '%чтени%' or lower(s.name_ru) like '%азбук%') and u.email like 'teacher.reading.%') or
          ((lower(s.name_ru) like '%познани%' or lower(s.name_ru) like '%дүниетану%' or lower(s.name_ru) like '%естеств%' or lower(s.name_ru) like '%окружающ%') and u.email like 'teacher.world_studies.%')
      )
    limit 1
) t
where cl.level between 1 and 11
on conflict (class_id, subject_id) do nothing;
