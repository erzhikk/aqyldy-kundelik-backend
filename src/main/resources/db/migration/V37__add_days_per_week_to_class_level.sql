-- Add days_per_week column to class_level table
-- Default is 5 (Mon-Fri), can be 6 (Mon-Sat)

-- Add column if not exists (safe migration)
do $$
begin
    if not exists (
        select 1 from information_schema.columns
        where table_name = 'class_level' and column_name = 'days_per_week'
    ) then
        alter table class_level add column days_per_week int not null default 5;
    end if;
end $$;

-- Add constraint if not exists
do $$
begin
    if not exists (
        select 1 from information_schema.table_constraints
        where constraint_name = 'class_level_days_per_week_check' and table_name = 'class_level'
    ) then
        alter table class_level add constraint class_level_days_per_week_check
            check (days_per_week in (5, 6));
    end if;
end $$;
