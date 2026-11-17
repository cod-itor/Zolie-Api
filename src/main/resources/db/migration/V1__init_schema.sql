-- =====================================================================
-- Zolie — PostgreSQL schema (Flyway migration V1)
-- Android app: social feed, Reels, 18+ dating, messaging
-- Stack: Spring Boot + MyBatis + PostgreSQL 16 + PostGIS
-- Auth: Firebase Authentication (users.firebase_uid)
-- Media: Cloudflare R2 (media_assets.object_key)
--
-- Conventions
--   * Primary keys are UUIDs (gen_random_uuid(), built into PostgreSQL 13+).
--   * Status/type columns are VARCHAR + CHECK instead of PostgreSQL ENUM
--     types, so MyBatis maps them as plain strings.
--   * All times are TIMESTAMPTZ (stored in UTC).
--   * Counter columns (like_count, follower_count, ...) are updated by the
--     service layer in the same transaction as the row they count.
--   * "Additional" features in this file: close_friends, story_stickers,
--     story_sticker_responses.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- =====================================================================
-- 1. ACCOUNTS
-- =====================================================================

CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    firebase_uid        VARCHAR(128) NOT NULL UNIQUE,
    email               VARCHAR(320) NOT NULL,
    username            VARCHAR(30)  NOT NULL,
    display_name        VARCHAR(50)  NOT NULL,
    bio                 VARCHAR(300),
    profile_photo_id    UUID,                       -- FK added after media_assets
    date_of_birth       DATE         NOT NULL,
    account_type        VARCHAR(10)  NOT NULL DEFAULT 'public',
    role                VARCHAR(10)  NOT NULL DEFAULT 'user',
    status              VARCHAR(12)  NOT NULL DEFAULT 'active',
    locale              VARCHAR(5)   NOT NULL DEFAULT 'km',
    theme               VARCHAR(10)  NOT NULL DEFAULT 'system',
    show_online_status  BOOLEAN      NOT NULL DEFAULT TRUE,
    last_active_at      TIMESTAMPTZ,
    suspended_until     TIMESTAMPTZ,
    terms_accepted_at   TIMESTAMPTZ  NOT NULL,
    follower_count      INTEGER      NOT NULL DEFAULT 0,
    following_count     INTEGER      NOT NULL DEFAULT 0,
    post_count          INTEGER      NOT NULL DEFAULT 0,
    search_vector       TSVECTOR GENERATED ALWAYS AS (
                            to_tsvector('simple', username || ' ' || display_name)
                        ) STORED,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ,
    CONSTRAINT users_username_format CHECK (username ~ '^[a-z0-9._]{3,30}$'),
    CONSTRAINT users_dob_sane        CHECK (date_of_birth > DATE '1900-01-01'),
    CONSTRAINT users_account_type    CHECK (account_type IN ('public','private')),
    CONSTRAINT users_role            CHECK (role IN ('user','admin')),
    CONSTRAINT users_status          CHECK (status IN ('active','suspended','deleted')),
    CONSTRAINT users_locale          CHECK (locale IN ('km','en')),
    CONSTRAINT users_theme           CHECK (theme IN ('system','light','dark'))
);
CREATE UNIQUE INDEX users_username_uq ON users (username);
CREATE UNIQUE INDEX users_email_uq    ON users (lower(email));
CREATE INDEX users_search_idx         ON users USING GIN (search_vector);
CREATE INDEX users_username_trgm_idx  ON users USING GIN (username gin_trgm_ops);
CREATE INDEX users_display_trgm_idx   ON users USING GIN (display_name gin_trgm_ops);

COMMENT ON TABLE  users IS 'One Zolie account. Login lives in Firebase Auth; everything else lives here.';
COMMENT ON COLUMN users.date_of_birth IS 'Private. Never returned by the API. Locked after registration (trigger users_lock_dob).';
COMMENT ON COLUMN users.account_type  IS 'Service sets private by default for users under 18.';
COMMENT ON COLUMN users.suspended_until IS 'Set with status = suspended for a temporary suspension. NULL = permanent or not suspended.';
COMMENT ON COLUMN users.terms_accepted_at IS 'When the user accepted the Terms, Privacy Policy and Child Safety Standards at registration.';

-- Date of birth cannot be changed once set (stops users unlocking Dating).
CREATE FUNCTION users_lock_dob() RETURNS trigger AS $$
BEGIN
    IF NEW.date_of_birth IS DISTINCT FROM OLD.date_of_birth THEN
        RAISE EXCEPTION 'date_of_birth cannot be changed after registration';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER users_lock_dob
    BEFORE UPDATE OF date_of_birth ON users
    FOR EACH ROW EXECUTE FUNCTION users_lock_dob();

-- Helper used by services and triggers.
CREATE FUNCTION age_years(dob DATE) RETURNS INTEGER AS $$
    SELECT date_part('year', age(current_date, dob))::INTEGER;
$$ LANGUAGE sql STABLE;

CREATE TABLE notification_preferences (
    user_id             UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    follows             BOOLEAN NOT NULL DEFAULT TRUE,
    likes               BOOLEAN NOT NULL DEFAULT TRUE,
    comments            BOOLEAN NOT NULL DEFAULT TRUE,
    mentions            BOOLEAN NOT NULL DEFAULT TRUE,
    matches             BOOLEAN NOT NULL DEFAULT TRUE,
    messages            BOOLEAN NOT NULL DEFAULT TRUE,
    story_interactions  BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE device_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(512) NOT NULL UNIQUE,
    platform    VARCHAR(10)  NOT NULL DEFAULT 'android',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT device_tokens_platform CHECK (platform IN ('android'))
);
CREATE INDEX device_tokens_user_idx ON device_tokens (user_id);
COMMENT ON TABLE device_tokens IS 'FCM push tokens. Private: never returned to other users.';

-- =====================================================================
-- 2. MEDIA (files live in Cloudflare R2; this table holds their keys)
-- =====================================================================

CREATE TABLE media_assets (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    object_key    VARCHAR(512) NOT NULL UNIQUE,
    kind          VARCHAR(10)  NOT NULL,
    purpose       VARCHAR(20)  NOT NULL,
    mime_type     VARCHAR(100) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    width         INTEGER,
    height        INTEGER,
    duration_ms   INTEGER,
    status        VARCHAR(12)  NOT NULL DEFAULT 'uploading',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT media_kind     CHECK (kind IN ('image','video','audio')),
    CONSTRAINT media_purpose  CHECK (purpose IN ('profile','post','story','reel','reel_thumbnail','chat','dating')),
    CONSTRAINT media_status   CHECK (status IN ('uploading','processing','ready','failed')),
    CONSTRAINT media_size     CHECK (size_bytes > 0 AND size_bytes <= 104857600),          -- 100 MB
    CONSTRAINT media_reel_len CHECK (purpose <> 'reel' OR duration_ms <= 90000)            -- 90 s
);
CREATE INDEX media_assets_owner_idx ON media_assets (owner_id);
COMMENT ON TABLE  media_assets IS 'One row per file in Cloudflare R2. Stores the object key, never the file or a public URL.';
COMMENT ON COLUMN media_assets.object_key IS 'R2 key, e.g. users/{uid}/reels/{reelId}/video.mp4. Served via presigned URLs.';

ALTER TABLE users
    ADD CONSTRAINT users_profile_photo_fk
    FOREIGN KEY (profile_photo_id) REFERENCES media_assets(id) ON DELETE SET NULL;

-- When a media row is deleted (post/Reel/story deleted, story expired, account
-- deleted), its R2 key is queued here so a scheduled job can delete the file.
-- Without this, account deletion would remove the rows but leave files in R2.
CREATE TABLE media_deletion_queue (
    id          BIGSERIAL PRIMARY KEY,
    object_key  VARCHAR(512) NOT NULL,
    queued_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    attempts    SMALLINT     NOT NULL DEFAULT 0,
    last_error  VARCHAR(500)
);
CREATE INDEX media_deletion_queue_queued_idx ON media_deletion_queue (queued_at);
COMMENT ON TABLE media_deletion_queue IS 'R2 files waiting to be deleted. Filled by trigger media_assets_queue_delete; emptied by a Spring @Scheduled job.';

CREATE FUNCTION media_assets_queue_delete() RETURNS trigger AS $$
BEGIN
    INSERT INTO media_deletion_queue (object_key) VALUES (OLD.object_key);
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER media_assets_queue_delete
    AFTER DELETE ON media_assets
    FOR EACH ROW EXECUTE FUNCTION media_assets_queue_delete();

-- =====================================================================
-- 3. SOCIAL GRAPH
-- =====================================================================

CREATE TABLE follows (
    follower_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    followed_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status       VARCHAR(10) NOT NULL DEFAULT 'accepted',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    accepted_at  TIMESTAMPTZ,
    PRIMARY KEY (follower_id, followed_id),
    CONSTRAINT follows_not_self CHECK (follower_id <> followed_id),
    CONSTRAINT follows_status   CHECK (status IN ('pending','accepted'))
);
CREATE INDEX follows_followed_idx ON follows (followed_id, status);
COMMENT ON COLUMN follows.status IS 'pending = follow request to a private account.';

-- Additional feature: Close Friends story list
CREATE TABLE close_friends (
    owner_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    friend_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (owner_id, friend_id),
    CONSTRAINT close_friends_not_self CHECK (owner_id <> friend_id)
);
CREATE INDEX close_friends_friend_idx ON close_friends (friend_id);
COMMENT ON TABLE close_friends IS 'Additional. People the owner picked to see close_friends stories. The list is private to the owner.';

CREATE TABLE blocks (
    blocker_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (blocker_id, blocked_id),
    CONSTRAINT blocks_not_self CHECK (blocker_id <> blocked_id)
);
CREATE INDEX blocks_blocked_idx ON blocks (blocked_id);

-- =====================================================================
-- 4. POSTS
-- =====================================================================

CREATE TABLE posts (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type           VARCHAR(10)   NOT NULL,
    text           VARCHAR(2200),
    visibility     VARCHAR(10)   NOT NULL DEFAULT 'public',
    poll_ends_at   TIMESTAMPTZ,
    like_count     INTEGER NOT NULL DEFAULT 0,
    comment_count  INTEGER NOT NULL DEFAULT 0,
    share_count    INTEGER NOT NULL DEFAULT 0,
    search_vector  TSVECTOR GENERATED ALWAYS AS (to_tsvector('simple', coalesce(text, ''))) STORED,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at     TIMESTAMPTZ,
    CONSTRAINT posts_type       CHECK (type IN ('text','photo','carousel','video','poll','audio')),
    CONSTRAINT posts_visibility CHECK (visibility IN ('public','followers')),
    CONSTRAINT posts_poll_end   CHECK (poll_ends_at IS NULL OR type = 'poll')
);
CREATE INDEX posts_author_idx  ON posts (author_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX posts_public_idx  ON posts (created_at DESC) WHERE deleted_at IS NULL AND visibility = 'public';
CREATE INDEX posts_search_idx  ON posts USING GIN (search_vector);
COMMENT ON COLUMN posts.visibility IS 'Private accounts: the service only shows posts to accepted followers regardless of this value.';

CREATE TABLE post_media (
    post_id   UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    media_id  UUID NOT NULL UNIQUE REFERENCES media_assets(id) ON DELETE CASCADE,
    position  SMALLINT NOT NULL,
    PRIMARY KEY (post_id, position),
    CONSTRAINT post_media_position CHECK (position BETWEEN 0 AND 9)
);
COMMENT ON TABLE post_media IS 'Photos, video or audio attached to a post, in carousel order (max 10).';

CREATE TABLE poll_options (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id     UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    position    SMALLINT    NOT NULL,
    text        VARCHAR(80) NOT NULL,
    vote_count  INTEGER     NOT NULL DEFAULT 0,
    UNIQUE (post_id, position),
    UNIQUE (id, post_id),
    CONSTRAINT poll_options_position CHECK (position BETWEEN 0 AND 3)
);

CREATE TABLE poll_votes (
    post_id     UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    option_id   UUID NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (post_id, user_id),
    FOREIGN KEY (option_id, post_id) REFERENCES poll_options(id, post_id) ON DELETE CASCADE
);
COMMENT ON TABLE poll_votes IS 'One vote per user per poll. The composite FK guarantees the option belongs to the same post.';

CREATE TABLE post_likes (
    post_id     UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (post_id, user_id)
);
CREATE INDEX post_likes_user_idx ON post_likes (user_id);

CREATE TABLE saved_posts (
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id     UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, post_id)
);

-- =====================================================================
-- 5. REELS
-- =====================================================================

CREATE TABLE reels (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    video_id       UUID NOT NULL UNIQUE REFERENCES media_assets(id),
    thumbnail_id   UUID UNIQUE REFERENCES media_assets(id) ON DELETE SET NULL,
    caption        VARCHAR(2200),
    duration_ms    INTEGER     NOT NULL,
    visibility     VARCHAR(10) NOT NULL DEFAULT 'public',
    status         VARCHAR(12) NOT NULL DEFAULT 'processing',
    like_count     INTEGER NOT NULL DEFAULT 0,
    comment_count  INTEGER NOT NULL DEFAULT 0,
    share_count    INTEGER NOT NULL DEFAULT 0,
    view_count     BIGINT  NOT NULL DEFAULT 0,
    search_vector  TSVECTOR GENERATED ALWAYS AS (to_tsvector('simple', coalesce(caption, ''))) STORED,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at     TIMESTAMPTZ,
    CONSTRAINT reels_duration   CHECK (duration_ms > 0 AND duration_ms <= 90000),
    CONSTRAINT reels_visibility CHECK (visibility IN ('public','followers')),
    CONSTRAINT reels_status     CHECK (status IN ('processing','ready','failed'))
);
CREATE INDEX reels_feed_idx    ON reels (created_at DESC) WHERE status = 'ready' AND deleted_at IS NULL;
CREATE INDEX reels_author_idx  ON reels (author_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX reels_search_idx  ON reels USING GIN (search_vector);
COMMENT ON COLUMN reels.status IS 'processing until FFmpeg has compressed the video to 720p and made the thumbnail.';

CREATE TABLE reel_likes (
    reel_id     UUID NOT NULL REFERENCES reels(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (reel_id, user_id)
);
CREATE INDEX reel_likes_user_idx ON reel_likes (user_id);

CREATE TABLE saved_reels (
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reel_id     UUID NOT NULL REFERENCES reels(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, reel_id)
);

-- =====================================================================
-- 6. COMMENTS AND MENTIONS (shared by posts and Reels)
-- =====================================================================

CREATE TABLE comments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id     UUID REFERENCES posts(id) ON DELETE CASCADE,
    reel_id     UUID REFERENCES reels(id) ON DELETE CASCADE,
    parent_id   UUID REFERENCES comments(id) ON DELETE CASCADE,
    text        VARCHAR(1000) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT comments_one_target CHECK (num_nonnulls(post_id, reel_id) = 1)
);
CREATE INDEX comments_post_idx ON comments (post_id, created_at) WHERE post_id IS NOT NULL;
CREATE INDEX comments_reel_idx ON comments (reel_id, created_at) WHERE reel_id IS NOT NULL;
COMMENT ON COLUMN comments.parent_id IS 'Set for a reply to another comment.';

CREATE TABLE mentions (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mentioned_user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    author_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id            UUID REFERENCES posts(id)    ON DELETE CASCADE,
    reel_id            UUID REFERENCES reels(id)    ON DELETE CASCADE,
    comment_id         UUID REFERENCES comments(id) ON DELETE CASCADE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT mentions_one_source CHECK (num_nonnulls(post_id, reel_id, comment_id) = 1)
);
CREATE INDEX mentions_user_idx ON mentions (mentioned_user_id, created_at DESC);

-- =====================================================================
-- 7. STORIES (24 h) + Additional: Close Friends audience and stickers
-- =====================================================================

CREATE TABLE stories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    media_id    UUID NOT NULL UNIQUE REFERENCES media_assets(id) ON DELETE CASCADE,
    audience    VARCHAR(15) NOT NULL DEFAULT 'everyone',
    view_count  INTEGER     NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ NOT NULL DEFAULT now() + INTERVAL '24 hours',
    CONSTRAINT stories_audience CHECK (audience IN ('everyone','close_friends')),
    CONSTRAINT stories_expiry   CHECK (expires_at > created_at)
);
CREATE INDEX stories_author_idx ON stories (author_id, expires_at DESC);
CREATE INDEX stories_expiry_idx ON stories (expires_at);
COMMENT ON COLUMN stories.audience IS 'everyone = followers (or public, per account type). close_friends = only users in the author''s close_friends list.';

CREATE TABLE story_views (
    story_id   UUID NOT NULL REFERENCES stories(id) ON DELETE CASCADE,
    viewer_id  UUID NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
    viewed_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (story_id, viewer_id)
);

-- Additional feature: interactive story stickers
CREATE TABLE story_stickers (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    story_id           UUID NOT NULL REFERENCES stories(id) ON DELETE CASCADE,
    type               VARCHAR(15) NOT NULL,
    prompt             VARCHAR(100),
    emoji              VARCHAR(16),
    countdown_ends_at  TIMESTAMPTZ,
    pos_x              REAL NOT NULL DEFAULT 0.5,
    pos_y              REAL NOT NULL DEFAULT 0.5,
    scale              REAL NOT NULL DEFAULT 1.0,
    rotation_deg       REAL NOT NULL DEFAULT 0,
    response_count     INTEGER NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (story_id, type),
    CONSTRAINT stickers_type     CHECK (type IN ('question','emoji_slider','countdown')),
    CONSTRAINT stickers_position CHECK (pos_x BETWEEN 0 AND 1 AND pos_y BETWEEN 0 AND 1),
    CONSTRAINT stickers_fields   CHECK (
        (type = 'question'     AND prompt IS NOT NULL) OR
        (type = 'emoji_slider' AND emoji  IS NOT NULL) OR
        (type = 'countdown'    AND prompt IS NOT NULL AND countdown_ends_at IS NOT NULL)
    )
);
COMMENT ON TABLE  story_stickers IS 'Additional. Question box, emoji slider or countdown placed on a story. One of each type per story.';
COMMENT ON COLUMN story_stickers.pos_x IS 'Position on the story as a fraction of width (0-1), so it fits any screen size.';

-- conversations and messages are created before sticker responses (FK below).

-- =====================================================================
-- 8. DATING (18+ only)
-- =====================================================================

CREATE TABLE dating_profiles (
    user_id              UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    status               VARCHAR(10)  NOT NULL DEFAULT 'active',
    bio                  VARCHAR(500),
    gender               VARCHAR(12)  NOT NULL,
    looking_for          VARCHAR(15)  NOT NULL DEFAULT 'not_sure',
    education            VARCHAR(100),
    occupation           VARCHAR(100),
    city                 VARCHAR(80),
    location             GEOGRAPHY(Point, 4326),
    location_updated_at  TIMESTAMPTZ,
    pref_genders         VARCHAR(12)[] NOT NULL DEFAULT ARRAY['woman','man','nonbinary']::VARCHAR(12)[],
    pref_min_age         SMALLINT NOT NULL DEFAULT 18,
    pref_max_age         SMALLINT NOT NULL DEFAULT 99,
    max_distance_km      SMALLINT NOT NULL DEFAULT 25,
    show_age             BOOLEAN NOT NULL DEFAULT TRUE,
    show_city            BOOLEAN NOT NULL DEFAULT TRUE,
    show_education       BOOLEAN NOT NULL DEFAULT FALSE,
    show_occupation      BOOLEAN NOT NULL DEFAULT FALSE,
    show_distance        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT dating_status      CHECK (status IN ('active','paused','off')),
    CONSTRAINT dating_gender      CHECK (gender IN ('woman','man','nonbinary','other')),
    CONSTRAINT dating_looking_for CHECK (looking_for IN ('relationship','casual','friends','not_sure')),
    CONSTRAINT dating_pref_ages   CHECK (pref_min_age >= 18 AND pref_max_age <= 99 AND pref_min_age <= pref_max_age),
    CONSTRAINT dating_distance    CHECK (max_distance_km BETWEEN 1 AND 200)
);
CREATE INDEX dating_profiles_location_idx ON dating_profiles USING GIST (location);
CREATE INDEX dating_profiles_active_idx   ON dating_profiles (gender) WHERE status = 'active';
COMMENT ON TABLE  dating_profiles IS 'Dating view of the account. Only users aged 18+ can have one (trigger dating_profiles_adult_only).';
COMMENT ON COLUMN dating_profiles.location IS 'Rounded to ~1 km before saving. Never returned by the API; only distance bands are.';

-- Database-level safety net: no dating profile for anyone under 18.
CREATE FUNCTION dating_profiles_adult_only() RETURNS trigger AS $$
BEGIN
    IF (SELECT age_years(date_of_birth) FROM users WHERE id = NEW.user_id) < 18 THEN
        RAISE EXCEPTION 'Dating is only available to users aged 18 and over';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER dating_profiles_adult_only
    BEFORE INSERT OR UPDATE ON dating_profiles
    FOR EACH ROW EXECUTE FUNCTION dating_profiles_adult_only();

CREATE TABLE dating_profile_photos (
    user_id   UUID NOT NULL REFERENCES dating_profiles(user_id) ON DELETE CASCADE,
    media_id  UUID NOT NULL REFERENCES media_assets(id) ON DELETE CASCADE,
    position  SMALLINT NOT NULL,
    PRIMARY KEY (user_id, position),
    UNIQUE (user_id, media_id),
    CONSTRAINT dating_photos_position CHECK (position BETWEEN 0 AND 5)
);

CREATE TABLE interests (
    id       SMALLSERIAL PRIMARY KEY,
    name_en  VARCHAR(40) NOT NULL UNIQUE,
    name_km  VARCHAR(60) NOT NULL UNIQUE
);
COMMENT ON TABLE interests IS 'Fixed list of interests in English and Khmer, seeded by a migration.';

CREATE TABLE dating_profile_interests (
    user_id      UUID     NOT NULL REFERENCES dating_profiles(user_id) ON DELETE CASCADE,
    interest_id  SMALLINT NOT NULL REFERENCES interests(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, interest_id)
);

CREATE TABLE dating_likes (
    from_user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    to_user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type          VARCHAR(6) NOT NULL DEFAULT 'swipe',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (from_user_id, to_user_id),
    CONSTRAINT dating_likes_not_self CHECK (from_user_id <> to_user_id),
    CONSTRAINT dating_likes_type     CHECK (type IN ('swipe','intro'))
);
CREATE INDEX dating_likes_to_idx ON dating_likes (to_user_id);
COMMENT ON TABLE dating_likes IS 'Separate from post/Reel likes and follows.';

CREATE TABLE dating_passes (
    from_user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    to_user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (from_user_id, to_user_id),
    CONSTRAINT dating_passes_not_self CHECK (from_user_id <> to_user_id)
);

CREATE TABLE dating_intros (
    from_user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    to_user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message_text  VARCHAR(500) NOT NULL,
    status        VARCHAR(10)  NOT NULL DEFAULT 'pending',
    sent_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (from_user_id, to_user_id),
    FOREIGN KEY (from_user_id, to_user_id) REFERENCES dating_likes(from_user_id, to_user_id) ON DELETE CASCADE,
    CONSTRAINT dating_intros_status CHECK (status IN ('pending','matched','passed'))
);
COMMENT ON TABLE dating_intros IS 'One intro per sender/recipient pair (primary key). Must have a matching dating_likes row (FK), saved in the same transaction.';

CREATE TABLE matches (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_a_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_b_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status      VARCHAR(10) NOT NULL DEFAULT 'active',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    ended_at    TIMESTAMPTZ,
    ended_by    UUID REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE (user_a_id, user_b_id),
    CONSTRAINT matches_ordered CHECK (user_a_id < user_b_id),
    CONSTRAINT matches_status  CHECK (status IN ('active','unmatched','blocked'))
);
CREATE INDEX matches_user_b_idx ON matches (user_b_id);
COMMENT ON COLUMN matches.user_a_id IS 'Always the smaller UUID of the pair, so each pair can only match once.';

-- =====================================================================
-- 9. MESSAGING (regular + dating, one-to-one)
-- =====================================================================

CREATE TABLE conversations (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type             VARCHAR(10) NOT NULL,
    user_a_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_b_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    match_id         UUID UNIQUE REFERENCES matches(id) ON DELETE CASCADE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_message_at  TIMESTAMPTZ,
    CONSTRAINT conversations_type     CHECK (type IN ('regular','dating')),
    CONSTRAINT conversations_ordered  CHECK (user_a_id < user_b_id),
    CONSTRAINT conversations_match    CHECK ((type = 'dating') = (match_id IS NOT NULL))
);
CREATE UNIQUE INDEX conversations_regular_pair_uq ON conversations (user_a_id, user_b_id) WHERE type = 'regular';
COMMENT ON TABLE conversations IS 'A dating conversation always belongs to a match. Two users have at most one regular conversation.';

CREATE TABLE conversation_participants (
    conversation_id  UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    state            VARCHAR(10) NOT NULL DEFAULT 'inbox',
    last_read_at     TIMESTAMPTZ,
    cleared_at       TIMESTAMPTZ,
    muted            BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (conversation_id, user_id),
    CONSTRAINT participants_state CHECK (state IN ('inbox','request'))
);
CREATE INDEX conversation_participants_user_idx ON conversation_participants (user_id);
COMMENT ON COLUMN conversation_participants.cleared_at IS '"Delete conversation" for this user only: messages before this time are hidden from them.';
COMMENT ON COLUMN conversation_participants.last_read_at IS 'Unread = messages from the other person created after this time.';
COMMENT ON COLUMN conversation_participants.state IS 'request = message request from someone this user does not follow (and, for under-18 users, not allowed until approved). Accepting moves it to inbox.';

CREATE TABLE messages (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id    UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type               VARCHAR(12) NOT NULL DEFAULT 'text',
    text               VARCHAR(2000),
    media_id           UUID REFERENCES media_assets(id) ON DELETE SET NULL,
    reply_to_story_id  UUID REFERENCES stories(id) ON DELETE SET NULL,
    shared_post_id     UUID REFERENCES posts(id)   ON DELETE SET NULL,
    shared_reel_id     UUID REFERENCES reels(id)   ON DELETE SET NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at         TIMESTAMPTZ,
    CONSTRAINT messages_type    CHECK (type IN ('text','image','intro','story_reply','post_share','reel_share','system')),
    -- Only text-type messages must have text. Image/share messages keep their row
    -- (shown as "unavailable") if the image, post or Reel is later deleted.
    CONSTRAINT messages_content CHECK (type NOT IN ('text','intro','system') OR text IS NOT NULL)
);
CREATE INDEX messages_conversation_idx ON messages (conversation_id, created_at DESC);
COMMENT ON COLUMN messages.reply_to_story_id IS 'Set when the message is a reply or sticker answer to a story (shows a story preview in chat).';
COMMENT ON COLUMN messages.shared_post_id IS 'In-app share of a post into a chat (type post_share).';
COMMENT ON COLUMN messages.shared_reel_id IS 'In-app share of a Reel into a chat (type reel_share).';

-- Additional feature: answers to story stickers (delivered as private replies)
CREATE TABLE story_sticker_responses (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sticker_id    UUID NOT NULL REFERENCES story_stickers(id) ON DELETE CASCADE,
    responder_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    answer_text   VARCHAR(300),
    slider_value  SMALLINT,
    remind_me     BOOLEAN NOT NULL DEFAULT FALSE,
    message_id    UUID REFERENCES messages(id) ON DELETE SET NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (sticker_id, responder_id),
    CONSTRAINT sticker_responses_slider CHECK (slider_value IS NULL OR slider_value BETWEEN 0 AND 100),
    CONSTRAINT sticker_responses_value  CHECK (answer_text IS NOT NULL OR slider_value IS NOT NULL OR remind_me)
);
CREATE INDEX sticker_responses_remind_idx ON story_sticker_responses (sticker_id) WHERE remind_me;
COMMENT ON TABLE  story_sticker_responses IS 'Additional. One answer per person per sticker. Question/slider answers are also sent to the author as a private story_reply message.';
COMMENT ON COLUMN story_sticker_responses.remind_me IS 'Countdown sticker: notify this user when the countdown ends.';

-- =====================================================================
-- 10. NOTIFICATIONS
-- =====================================================================

CREATE TABLE notifications (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    actor_id      UUID REFERENCES users(id) ON DELETE CASCADE,
    type          VARCHAR(20) NOT NULL,
    target_type   VARCHAR(15),
    target_id     UUID,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at       TIMESTAMPTZ,
    CONSTRAINT notifications_type CHECK (type IN (
        'follow','follow_request','follow_accepted','post_like','reel_like','comment',
        'mention','match','dating_intro','message','story_reply','sticker_response',
        'countdown_ended','report_resolved','moderation_notice')),
    CONSTRAINT notifications_target CHECK (target_type IS NULL OR target_type IN (
        'user','post','reel','comment','story','story_sticker','match','conversation','report'))
);
CREATE INDEX notifications_recipient_idx ON notifications (recipient_id, created_at DESC);
CREATE INDEX notifications_unread_idx    ON notifications (recipient_id) WHERE read_at IS NULL;
COMMENT ON COLUMN notifications.target_id IS 'Points to the row named by target_type (no FK, because it can be several tables).';

-- =====================================================================
-- 11. SAFETY AND MODERATION
-- =====================================================================

CREATE TABLE reports (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_type      VARCHAR(10) NOT NULL,
    target_id        UUID NOT NULL,
    reason           VARCHAR(15) NOT NULL,
    details          VARCHAR(1000),
    status           VARCHAR(10) NOT NULL DEFAULT 'open',
    reviewed_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at      TIMESTAMPTZ,
    resolution_note  VARCHAR(500),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (reporter_id, target_type, target_id),
    CONSTRAINT reports_target CHECK (target_type IN ('user','post','reel','comment','story','message')),
    CONSTRAINT reports_reason CHECK (reason IN ('spam','harassment','nudity','violence','hate','underage','scam','fake_profile','other')),
    CONSTRAINT reports_status CHECK (status IN ('open','reviewing','resolved','dismissed'))
);
CREATE INDEX reports_queue_idx  ON reports (status, created_at) WHERE status IN ('open','reviewing');
CREATE INDEX reports_target_idx ON reports (target_type, target_id);
COMMENT ON TABLE reports IS 'Reviewed by admins (users.role = admin). One report per user per item.';

CREATE TABLE moderation_actions (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id     UUID REFERENCES users(id) ON DELETE SET NULL,
    report_id    UUID REFERENCES reports(id) ON DELETE SET NULL,
    action       VARCHAR(20) NOT NULL,
    target_type  VARCHAR(10) NOT NULL,
    target_id    UUID NOT NULL,
    note         VARCHAR(500),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT moderation_action CHECK (action IN ('remove_content','restore_content','warn_user','suspend_user','unsuspend_user','dismiss_report')),
    CONSTRAINT moderation_target CHECK (target_type IN ('user','post','reel','comment','story','message'))
);
CREATE INDEX moderation_actions_target_idx ON moderation_actions (target_type, target_id);
COMMENT ON TABLE moderation_actions IS 'Audit log of every admin action.';
