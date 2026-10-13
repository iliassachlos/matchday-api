-- Optimistic locking: Hibernate appends "AND version = ?" to every UPDATE and bumps the value, so a stale write affects zero rows and fails.
ALTER TABLE seats ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
