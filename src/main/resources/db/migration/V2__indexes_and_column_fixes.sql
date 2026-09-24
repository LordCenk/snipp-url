-- Foreign keys are not indexed automatically in Postgres; these back the
-- per-user URL listing and the analytics/delete queries.
CREATE INDEX IF NOT EXISTS idx_urls_user_id ON urls (user_id);
CREATE INDEX IF NOT EXISTS idx_analytics_event_url_id ON analytics_event (url_id);

-- User-Agent and Referer headers regularly exceed 255 characters, which made
-- the redirect fail when recording the click.
ALTER TABLE analytics_event ALTER COLUMN device TYPE text;
ALTER TABLE analytics_event ALTER COLUMN referrer TYPE text;

UPDATE urls SET click_count = 0 WHERE click_count IS NULL;
ALTER TABLE urls ALTER COLUMN click_count SET DEFAULT 0;
ALTER TABLE urls ALTER COLUMN click_count SET NOT NULL;
