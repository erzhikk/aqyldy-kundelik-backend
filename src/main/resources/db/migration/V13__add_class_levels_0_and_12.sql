-- Add class levels 0 and 12 if they don't exist
insert into class_level (level, name_ru, name_kk) values
    (0, 'Подготовительный класс', 'Дайындық сыныбы'),
    (12, '12 класс', '12 сынып')
on conflict (level) do nothing;
