-- Add class_level_id column to school_class table

alter table school_class add column if not exists class_level_id uuid;

-- Add foreign key constraint to class_level table
alter table school_class add constraint fk_school_class_level
    foreign key (class_level_id) references class_level(id) on delete restrict;

-- Create index for performance
create index if not exists idx_school_class_level_id on school_class(class_level_id);
