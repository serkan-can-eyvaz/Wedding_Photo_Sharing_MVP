-- Existing-volume upgrade, stage 2. Run after the viewer-enabled backend has
-- completed its startup backfill and this query returns zero rows:
-- SELECT count(*) FROM events WHERE viewer_token IS NULL;
ALTER TABLE events ALTER COLUMN viewer_token SET NOT NULL;
DROP INDEX IF EXISTS ux_events_viewer_token;
ALTER TABLE events ADD CONSTRAINT uq_events_viewer_token UNIQUE (viewer_token);
