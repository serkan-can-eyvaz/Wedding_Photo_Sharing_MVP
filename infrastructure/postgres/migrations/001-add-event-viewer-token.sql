-- Existing-volume upgrade, stage 1. Run once before deploying the viewer release.
-- Keeps existing events intact; the application backfills only NULL values at startup.
ALTER TABLE events ADD COLUMN IF NOT EXISTS viewer_token VARCHAR(255);
CREATE UNIQUE INDEX IF NOT EXISTS ux_events_viewer_token
    ON events (viewer_token)
    WHERE viewer_token IS NOT NULL;
