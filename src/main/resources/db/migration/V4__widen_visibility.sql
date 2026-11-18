-- =====================================================================
-- Zolie — V4: room for the 'close_friends' audience value
-- =====================================================================
ALTER TABLE posts ALTER COLUMN visibility TYPE VARCHAR(15);
ALTER TABLE reels ALTER COLUMN visibility TYPE VARCHAR(15);
