-- =====================================================================
-- Zolie — V3: features shown on the Version 2 screens
-- places, profile extras, privacy, insights, reactions, comment likes,
-- hashtags, collections, sounds, text moments, notes, message reactions,
-- dating prompts, events
-- =====================================================================

-- ---------------------------------------------------------------------
-- Places (location tags, "Places I love", Explore, events)
-- ---------------------------------------------------------------------
CREATE TABLE places (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id   UUID REFERENCES places(id) ON DELETE SET NULL,
    type        VARCHAR(10)  NOT NULL,
    name_en     VARCHAR(80)  NOT NULL,
    name_km     VARCHAR(120),
    location    GEOGRAPHY(Point, 4326),
    is_popular  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT places_type CHECK (type IN ('province','city','district','landmark','venue'))
);
CREATE INDEX places_name_en_trgm_idx ON places USING GIN (name_en gin_trgm_ops);
CREATE INDEX places_name_km_trgm_idx ON places USING GIN (name_km gin_trgm_ops);

CREATE TABLE user_favorite_places (
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    place_id    UUID NOT NULL REFERENCES places(id) ON DELETE CASCADE,
    position    SMALLINT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, place_id)
);

INSERT INTO places (type, name_en, name_km, location, is_popular) VALUES
    ('city',     'Phnom Penh',            'ភ្នំពេញ',          ST_SetSRID(ST_MakePoint(104.9282, 11.5564), 4326)::geography, TRUE),
    ('city',     'Siem Reap',             'សៀមរាប',          ST_SetSRID(ST_MakePoint(103.8590, 13.3633), 4326)::geography, TRUE),
    ('city',     'Kampot',                'កំពត',            ST_SetSRID(ST_MakePoint(104.1810, 10.6104), 4326)::geography, TRUE),
    ('city',     'Kep',                   'កែប',             ST_SetSRID(ST_MakePoint(104.3167, 10.4833), 4326)::geography, TRUE),
    ('city',     'Battambang',            'បាត់ដំបង',        ST_SetSRID(ST_MakePoint(103.1986, 13.0957), 4326)::geography, TRUE),
    ('city',     'Sihanoukville',         'ក្រុងព្រះសីហនុ',    ST_SetSRID(ST_MakePoint(103.5234, 10.6253), 4326)::geography, TRUE),
    ('province', 'Mondulkiri',            'មណ្ឌលគិរី',        ST_SetSRID(ST_MakePoint(107.1878, 12.4558), 4326)::geography, FALSE),
    ('landmark', 'Koh Rong',              'កោះរ៉ុង',          ST_SetSRID(ST_MakePoint(103.2833, 10.7167), 4326)::geography, TRUE),
    ('landmark', 'Angkor Wat',            'អង្គរវត្ត',        ST_SetSRID(ST_MakePoint(103.8670, 13.4125), 4326)::geography, TRUE),
    ('landmark', 'Sisowath Quay',         'មាត់ទន្លេ',        ST_SetSRID(ST_MakePoint(104.9310, 11.5690), 4326)::geography, TRUE),
    ('landmark', 'Independence Monument', 'វិមានឯករាជ្យ',     ST_SetSRID(ST_MakePoint(104.9280, 11.5564), 4326)::geography, FALSE),
    ('landmark', 'Royal Palace',          'ព្រះបរមរាជវាំង',   ST_SetSRID(ST_MakePoint(104.9310, 11.5639), 4326)::geography, FALSE);

-- ---------------------------------------------------------------------
-- Profile, privacy, account
-- ---------------------------------------------------------------------
ALTER TABLE users
    ADD COLUMN khmer_name                  VARCHAR(50),
    ADD COLUMN website_url                 VARCHAR(200),
    ADD COLUMN city                        VARCHAR(80),
    ADD COLUMN cover_photo_id              UUID REFERENCES media_assets(id) ON DELETE SET NULL,
    ADD COLUMN data_saver                  BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN read_receipts               BOOLEAN     NOT NULL DEFAULT TRUE,
    ADD COLUMN who_can_message             VARCHAR(10) NOT NULL DEFAULT 'everyone',
    ADD COLUMN who_can_mention             VARCHAR(10) NOT NULL DEFAULT 'everyone',
    ADD COLUMN story_replies_from          VARCHAR(10) NOT NULL DEFAULT 'everyone',
    ADD COLUMN hide_dating_from_following  BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN deletion_requested_at       TIMESTAMPTZ,
    ADD COLUMN delete_after                TIMESTAMPTZ,
    ADD COLUMN deletion_reason             VARCHAR(200),
    ADD CONSTRAINT users_who_can_message    CHECK (who_can_message    IN ('everyone','followers','nobody')),
    ADD CONSTRAINT users_who_can_mention    CHECK (who_can_mention    IN ('everyone','followers','nobody')),
    ADD CONSTRAINT users_story_replies_from CHECK (story_replies_from IN ('everyone','followers','nobody'));
CREATE INDEX users_delete_after_idx ON users (delete_after) WHERE delete_after IS NOT NULL;

ALTER TABLE notification_preferences
    ADD COLUMN events        BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN paused_until  TIMESTAMPTZ;

ALTER TABLE device_tokens
    ADD COLUMN device_name   VARCHAR(100),
    ADD COLUMN last_seen_at  TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE media_assets DROP CONSTRAINT media_purpose;
ALTER TABLE media_assets ADD CONSTRAINT media_purpose
    CHECK (purpose IN ('profile','post','story','reel','reel_thumbnail','chat','dating','sound','event'));

-- ---------------------------------------------------------------------
-- Insights
-- ---------------------------------------------------------------------
CREATE TABLE profile_visits (
    profile_user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    visitor_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    visit_date       DATE NOT NULL DEFAULT current_date,
    PRIMARY KEY (profile_user_id, visitor_id, visit_date)
);
CREATE INDEX profile_visits_date_idx ON profile_visits (profile_user_id, visit_date);

CREATE TABLE post_views (
    post_id    UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    viewer_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    viewed_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (post_id, viewer_id)
);
CREATE INDEX post_views_viewed_idx ON post_views (viewed_at);

-- ---------------------------------------------------------------------
-- Help
-- ---------------------------------------------------------------------
CREATE TABLE help_articles (
    id          SMALLSERIAL PRIMARY KEY,
    topic       VARCHAR(20)   NOT NULL,
    title_en    VARCHAR(120)  NOT NULL,
    title_km    VARCHAR(200),
    body_en     VARCHAR(2000) NOT NULL,
    body_km     VARCHAR(3000),
    is_popular  BOOLEAN  NOT NULL DEFAULT FALSE,
    sort_order  SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT help_topic CHECK (topic IN ('account','privacy','dating_safety','reporting'))
);

INSERT INTO help_articles (topic, title_en, title_km, body_en, is_popular, sort_order) VALUES
    ('account',       'I can''t get the SMS code',            'ខ្ញុំមិនទទួលបានកូដ SMS',
     'Codes can take a minute on Smart, Cellcard and Metfone. Wait for the timer, tap Resend, or sign in with your email instead.', TRUE, 1),
    ('privacy',       'How do I make my account private?',    'តើខ្ញុំធ្វើឲ្យគណនីរបស់ខ្ញុំជាឯកជនដោយរបៀបណា?',
     'Open Settings, then Privacy, and turn on Private account. Only people you approve will see your posts and moments.', TRUE, 2),
    ('dating_safety', 'Who can see my Sneh profile?',         'តើអ្នកណាអាចមើលឃើញប្រវត្តិរូប Sneh របស់ខ្ញុំ?',
     'Only people aged 18 and over who are on Sneh. You can also hide it from people you follow in Settings, then Privacy.', TRUE, 3),
    ('reporting',     'How do I report a fake page?',         'តើខ្ញុំរាយការណ៍ទំព័រក្លែងក្លាយដោយរបៀបណា?',
     'Open the profile or post, tap the menu, choose Report and pick Scam or fake page. Reports are anonymous.', TRUE, 4),
    ('account',       'Can I change my username?',            'តើខ្ញុំអាចប្តូរឈ្មោះអ្នកប្រើបានទេ?',
     'Yes. Open your profile, tap Edit and choose a new username that is still free.', TRUE, 5);

-- ---------------------------------------------------------------------
-- Sounds
-- ---------------------------------------------------------------------
CREATE TABLE sounds (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(120) NOT NULL,
    artist_name     VARCHAR(80),
    creator_id      UUID REFERENCES users(id) ON DELETE SET NULL,
    audio_media_id  UUID UNIQUE REFERENCES media_assets(id) ON DELETE SET NULL,
    duration_ms     INTEGER,
    is_library      BOOLEAN NOT NULL DEFAULT FALSE,
    use_count       INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX sounds_title_trgm_idx ON sounds USING GIN (title gin_trgm_ops);
CREATE INDEX sounds_popular_idx    ON sounds (use_count DESC, id DESC);

-- ---------------------------------------------------------------------
-- Posts: audience, location, sound, text style, post options, polls
-- ---------------------------------------------------------------------
ALTER TABLE posts DROP CONSTRAINT posts_visibility;
ALTER TABLE posts
    ADD CONSTRAINT posts_visibility CHECK (visibility IN ('public','followers','close_friends','private')),
    ADD COLUMN place_id                 UUID REFERENCES places(id) ON DELETE SET NULL,
    ADD COLUMN sound_id                 UUID REFERENCES sounds(id) ON DELETE SET NULL,
    ADD COLUMN text_background          VARCHAR(20),
    ADD COLUMN text_style               VARCHAR(20),
    ADD COLUMN comments_enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN hide_like_count          BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN pinned_at                TIMESTAMPTZ,
    ADD COLUMN poll_allow_multiple      BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN poll_show_results_first  BOOLEAN NOT NULL DEFAULT TRUE;
CREATE INDEX posts_pinned_idx ON posts (author_id, pinned_at DESC) WHERE pinned_at IS NOT NULL AND deleted_at IS NULL;

ALTER TABLE poll_votes DROP CONSTRAINT poll_votes_pkey;
ALTER TABLE poll_votes ADD PRIMARY KEY (post_id, user_id, option_id);

ALTER TABLE post_likes ADD COLUMN reaction VARCHAR(16) NOT NULL DEFAULT '💜';
ALTER TABLE reel_likes ADD COLUMN reaction VARCHAR(16) NOT NULL DEFAULT '💜';

ALTER TABLE comments ADD COLUMN like_count INTEGER NOT NULL DEFAULT 0;
CREATE TABLE comment_likes (
    comment_id  UUID NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (comment_id, user_id)
);

CREATE TABLE hidden_posts (
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id     UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, post_id)
);

-- ---------------------------------------------------------------------
-- Reels: audience, comments switch, location, sound
-- ---------------------------------------------------------------------
ALTER TABLE reels DROP CONSTRAINT reels_visibility;
ALTER TABLE reels
    ADD CONSTRAINT reels_visibility CHECK (visibility IN ('public','followers','close_friends','private')),
    ADD COLUMN comments_enabled  BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN place_id          UUID REFERENCES places(id) ON DELETE SET NULL,
    ADD COLUMN sound_id          UUID REFERENCES sounds(id) ON DELETE SET NULL;
CREATE INDEX reels_sound_idx ON reels (sound_id, created_at DESC) WHERE sound_id IS NOT NULL;

-- ---------------------------------------------------------------------
-- Hashtags
-- ---------------------------------------------------------------------
CREATE TABLE hashtags (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tag         VARCHAR(100) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX hashtags_tag_trgm_idx ON hashtags USING GIN (tag gin_trgm_ops);

CREATE TABLE post_hashtags (
    post_id     UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    hashtag_id  UUID NOT NULL REFERENCES hashtags(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (post_id, hashtag_id)
);
CREATE INDEX post_hashtags_tag_idx ON post_hashtags (hashtag_id, created_at DESC);

CREATE TABLE reel_hashtags (
    reel_id     UUID NOT NULL REFERENCES reels(id) ON DELETE CASCADE,
    hashtag_id  UUID NOT NULL REFERENCES hashtags(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (reel_id, hashtag_id)
);
CREATE INDEX reel_hashtags_tag_idx ON reel_hashtags (hashtag_id, created_at DESC);

-- ---------------------------------------------------------------------
-- Saved collections
-- ---------------------------------------------------------------------
CREATE TABLE collections (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(60) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, name)
);

CREATE TABLE collection_items (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    collection_id  UUID NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
    post_id        UUID REFERENCES posts(id) ON DELETE CASCADE,
    reel_id        UUID REFERENCES reels(id) ON DELETE CASCADE,
    added_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (collection_id, post_id),
    UNIQUE (collection_id, reel_id),
    CONSTRAINT collection_items_one_target CHECK (num_nonnulls(post_id, reel_id) = 1)
);
CREATE INDEX collection_items_page_idx ON collection_items (collection_id, added_at DESC, id DESC);

-- ---------------------------------------------------------------------
-- Reports: reasons shown on the Report screen
-- ---------------------------------------------------------------------
ALTER TABLE reports DROP CONSTRAINT reports_reason;
ALTER TABLE reports ADD CONSTRAINT reports_reason CHECK (reason IN (
    'spam','harassment','nudity','violence','hate','underage','scam','fake_profile','false_info','illegal_goods','other'));

-- ---------------------------------------------------------------------
-- Stories: text moments and sharing a post as a moment
-- ---------------------------------------------------------------------
ALTER TABLE stories
    ALTER COLUMN media_id DROP NOT NULL,
    ADD COLUMN text_content     VARCHAR(500),
    ADD COLUMN text_background  VARCHAR(20),
    ADD COLUMN shared_post_id   UUID REFERENCES posts(id) ON DELETE CASCADE,
    ADD CONSTRAINT stories_content CHECK (num_nonnulls(media_id, text_content, shared_post_id) >= 1);

-- ---------------------------------------------------------------------
-- Messaging: notes, reactions, voice notes
-- ---------------------------------------------------------------------
CREATE TABLE user_notes (
    user_id     UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    text        VARCHAR(60) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX user_notes_expiry_idx ON user_notes (expires_at);

CREATE TABLE message_reactions (
    message_id  UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    emoji       VARCHAR(16) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (message_id, user_id)
);

ALTER TABLE messages DROP CONSTRAINT messages_type;
ALTER TABLE messages ADD CONSTRAINT messages_type
    CHECK (type IN ('text','image','voice','intro','story_reply','post_share','reel_share','system'));
CREATE INDEX messages_media_idx ON messages (conversation_id, created_at DESC) WHERE media_id IS NOT NULL;

-- ---------------------------------------------------------------------
-- Dating: prompts and prompt answers
-- ---------------------------------------------------------------------
CREATE TABLE dating_prompts (
    id          SMALLSERIAL PRIMARY KEY,
    text_en     VARCHAR(80)  NOT NULL UNIQUE,
    text_km     VARCHAR(160),
    sort_order  SMALLINT NOT NULL DEFAULT 0,
    is_active   BOOLEAN  NOT NULL DEFAULT TRUE
);

INSERT INTO dating_prompts (text_en, text_km, sort_order) VALUES
    ('My perfect Sunday',               'ថ្ងៃអាទិត្យដ៏ល្អឥតខ្ចោះរបស់ខ្ញុំ',          1),
    ('Best street food in town',        'អាហារតាមផ្លូវឆ្ងាញ់បំផុតក្នុងក្រុង',         2),
    ('I get way too excited about',     'ខ្ញុំរំភើបខ្លាំងពេកចំពោះ',                  3),
    ('A place I want to take you',      'កន្លែងដែលខ្ញុំចង់នាំអ្នកទៅ',                4),
    ('My simple pleasure',              'សេចក្តីសុខសាមញ្ញរបស់ខ្ញុំ',                 5),
    ('The song I play on repeat',       'បទចម្រៀងដែលខ្ញុំស្តាប់ម្តងហើយម្តងទៀត',      6);

CREATE TABLE dating_prompt_answers (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID     NOT NULL REFERENCES dating_profiles(user_id) ON DELETE CASCADE,
    prompt_id   SMALLINT NOT NULL REFERENCES dating_prompts(id) ON DELETE CASCADE,
    answer      VARCHAR(300) NOT NULL,
    position    SMALLINT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, prompt_id),
    CONSTRAINT dating_prompt_answers_position CHECK (position BETWEEN 0 AND 2)
);
CREATE INDEX dating_prompt_answers_user_idx ON dating_prompt_answers (user_id, position);

ALTER TABLE dating_intros
    ADD COLUMN prompt_answer_id UUID REFERENCES dating_prompt_answers(id) ON DELETE SET NULL;

-- ---------------------------------------------------------------------
-- Events ("What's on")
-- ---------------------------------------------------------------------
CREATE TABLE events (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    host_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title             VARCHAR(120) NOT NULL,
    description       VARCHAR(2000),
    category          VARCHAR(12)  NOT NULL DEFAULT 'other',
    cover_media_id    UUID UNIQUE REFERENCES media_assets(id) ON DELETE SET NULL,
    starts_at         TIMESTAMPTZ  NOT NULL,
    ends_at           TIMESTAMPTZ,
    place_id          UUID REFERENCES places(id) ON DELETE SET NULL,
    venue_name        VARCHAR(120),
    is_free           BOOLEAN      NOT NULL DEFAULT TRUE,
    price_note        VARCHAR(80),
    visibility        VARCHAR(10)  NOT NULL DEFAULT 'public',
    show_guest_list   BOOLEAN      NOT NULL DEFAULT TRUE,
    interested_count  INTEGER      NOT NULL DEFAULT 0,
    going_count       INTEGER      NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    cancelled_at      TIMESTAMPTZ,
    CONSTRAINT events_category   CHECK (category IN ('festival','music','food','sports','market','community','other')),
    CONSTRAINT events_visibility CHECK (visibility IN ('public','followers')),
    CONSTRAINT events_times      CHECK (ends_at IS NULL OR ends_at > starts_at)
);
CREATE INDEX events_starts_idx ON events (starts_at, id) WHERE cancelled_at IS NULL;
CREATE INDEX events_host_idx   ON events (host_id, starts_at DESC);

CREATE TABLE event_rsvps (
    event_id       UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status         VARCHAR(10) NOT NULL,
    day_reminded   BOOLEAN NOT NULL DEFAULT FALSE,
    hour_reminded  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, user_id),
    CONSTRAINT event_rsvps_status CHECK (status IN ('interested','going'))
);
CREATE INDEX event_rsvps_user_idx ON event_rsvps (user_id);

ALTER TABLE notifications DROP CONSTRAINT notifications_type;
ALTER TABLE notifications ADD CONSTRAINT notifications_type CHECK (type IN (
    'follow','follow_request','follow_accepted','post_like','reel_like','comment','comment_like',
    'mention','match','dating_intro','message','story_reply','sticker_response',
    'countdown_ended','event_reminder','report_resolved','moderation_notice'));
ALTER TABLE notifications DROP CONSTRAINT notifications_target;
ALTER TABLE notifications ADD CONSTRAINT notifications_target CHECK (target_type IS NULL OR target_type IN (
    'user','post','reel','comment','story','story_sticker','match','conversation','report','event'));
