-- Добавление photo_media_id в таблицу app_user (только если не существует)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'app_user' AND column_name = 'photo_media_id'
    ) THEN
        ALTER TABLE app_user ADD COLUMN photo_media_id UUID;
    END IF;
END $$;

-- Добавление внешнего ключа на media_object (только если не существует)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_user_photo_media'
    ) THEN
        ALTER TABLE app_user
        ADD CONSTRAINT fk_user_photo_media
        FOREIGN KEY (photo_media_id) REFERENCES media_object(id) ON DELETE SET NULL;
    END IF;
END $$;

-- Создание индекса для быстрого поиска (только если не существует)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE indexname = 'idx_app_user_photo_media_id'
    ) THEN
        CREATE INDEX idx_app_user_photo_media_id ON app_user(photo_media_id);
    END IF;
END $$;
