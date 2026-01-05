-- Add created_by_user_id to topic table
ALTER TABLE topic
  ADD COLUMN IF NOT EXISTS created_by_user_id uuid;

-- Add foreign key constraint
ALTER TABLE topic
  ADD CONSTRAINT fk_topic_created_by_user
  FOREIGN KEY (created_by_user_id) REFERENCES app_user(id) ON DELETE SET NULL;

-- Add index for performance
CREATE INDEX IF NOT EXISTS idx_topic_created_by ON topic(created_by_user_id);
