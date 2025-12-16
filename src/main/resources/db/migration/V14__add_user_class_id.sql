-- Add class_id column to app_user table
alter table app_user add column if not exists class_id uuid;

-- Add foreign key constraint to school_class table
alter table app_user add constraint fk_user_class
    foreign key (class_id) references school_class(id) on delete set null;

-- Create index for performance
create index if not exists idx_user_class_id on app_user(class_id);
