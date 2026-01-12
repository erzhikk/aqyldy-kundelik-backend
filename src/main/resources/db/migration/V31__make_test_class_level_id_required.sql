-- Add class_level_id column if it doesn't exist
ALTER TABLE test
ADD COLUMN IF NOT EXISTS class_level_id uuid REFERENCES class_level(id) ON DELETE RESTRICT;

-- Create index on class_level_id if it doesn't exist
CREATE INDEX IF NOT EXISTS idx_test_class_level_id ON test(class_level_id);

-- Set class_level_id for existing tests based on their subject's class_level
UPDATE test
SET class_level_id = subject.class_level_id
FROM subject
WHERE test.subject_id = subject.id
  AND test.class_level_id IS NULL;

-- Now make class_level_id NOT NULL
ALTER TABLE test
ALTER COLUMN class_level_id SET NOT NULL;
