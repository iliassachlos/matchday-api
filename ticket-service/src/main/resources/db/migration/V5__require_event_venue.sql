-- Contract step for V3's expand: every event must now have a venue.
-- Events without one can only be load-test fixtures created before venues existed their seats are removed with them by ON DELETE CASCADE.
DELETE FROM events WHERE venue_id IS NULL;

ALTER TABLE events ALTER COLUMN venue_id SET NOT NULL;
