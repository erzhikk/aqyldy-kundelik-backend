create extension if not exists pg_trgm;
create index if not exists idx_subject_name_trgm on subject using gin (name gin_trgm_ops);
