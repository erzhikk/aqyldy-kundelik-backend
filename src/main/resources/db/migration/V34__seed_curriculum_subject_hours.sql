-- Seed curriculum subject hours with reasonable MVP values
-- idempotent: uses ON CONFLICT DO NOTHING

-- Helper function to get hours based on subject name pattern
-- Math-related: 5-6 hours, Languages: 4-5 hours, Sciences: 3-4 hours, Others: 2-3 hours

-- Insert curriculum hours for all subjects in each class level
-- This inserts for every subject that exists in the subject table

insert into curriculum_subject_hours (class_level_id, subject_id, hours_per_week)
select
    s.class_level_id,
    s.id as subject_id,
    case
        -- Math subjects: 5 hours
        when lower(s.name_ru) like '%математик%' then 5
        when lower(s.name_ru) like '%алгебр%' then 5
        when lower(s.name_ru) like '%геометр%' then 3

        -- Languages: 4-5 hours
        when lower(s.name_ru) like '%казах%' or lower(s.name_ru) like '%қазақ%' then 4
        when lower(s.name_ru) like '%русск%' then 4
        when lower(s.name_ru) like '%англий%' or lower(s.name_ru) like '%english%' then 3
        when lower(s.name_ru) like '%азбук%' then 4
        when lower(s.name_ru) like '%чтени%' or lower(s.name_ru) like '%литератур%' then 3

        -- Sciences: 2-3 hours
        when lower(s.name_ru) like '%физик%' then 3
        when lower(s.name_ru) like '%хими%' then 2
        when lower(s.name_ru) like '%биолог%' then 2
        when lower(s.name_ru) like '%географ%' then 2
        when lower(s.name_ru) like '%информатик%' then 2
        when lower(s.name_ru) like '%естествознан%' or lower(s.name_ru) like '%дүниетану%' then 2
        when lower(s.name_ru) like '%познани%' or lower(s.name_ru) like '%окружающ%' then 2

        -- History and social: 2 hours
        when lower(s.name_ru) like '%истори%' then 2
        when lower(s.name_ru) like '%общество%' then 1
        when lower(s.name_ru) like '%право%' then 1

        -- Art, music, PE: 1-2 hours
        when lower(s.name_ru) like '%физкульт%' or lower(s.name_ru) like '%физическ%' then 3
        when lower(s.name_ru) like '%музык%' then 1
        when lower(s.name_ru) like '%изо%' or lower(s.name_ru) like '%рисован%' or lower(s.name_ru) like '%искусств%' then 1
        when lower(s.name_ru) like '%технолог%' or lower(s.name_ru) like '%труд%' then 1

        -- Default
        else 2
    end as hours_per_week
from subject s
join class_level cl on s.class_level_id = cl.id
where cl.level between 1 and 11
on conflict (class_level_id, subject_id) do nothing;
