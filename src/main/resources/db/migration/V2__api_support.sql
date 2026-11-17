-- =====================================================================
-- Zolie — V2: tables and rules the API needs on top of the V1 schema
-- =====================================================================

-- Interests a user picks at onboarding (PUT /me/interests).
CREATE TABLE user_interests (
    user_id      UUID     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    interest_id  SMALLINT NOT NULL REFERENCES interests(id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, interest_id)
);

-- Idempotency-Key header: a retried POST returns the first result instead of a duplicate.
CREATE TABLE idempotency_keys (
    user_id        UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    key            UUID        NOT NULL,
    resource_type  VARCHAR(20) NOT NULL,
    resource_id    UUID        NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, key)
);
CREATE INDEX idempotency_keys_created_idx ON idempotency_keys (created_at);

-- clientMessageId: stops double messages when the app retries on a bad network.
ALTER TABLE messages ADD COLUMN client_message_id UUID;
CREATE UNIQUE INDEX messages_client_id_uq ON messages (sender_id, client_message_id)
    WHERE client_message_id IS NOT NULL;

-- Database-level safety net: nobody under 13 can register.
CREATE FUNCTION users_min_age() RETURNS trigger AS $$
BEGIN
    IF age_years(NEW.date_of_birth) < 13 THEN
        RAISE EXCEPTION 'Users must be at least 13 years old';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER users_min_age
    BEFORE INSERT ON users
    FOR EACH ROW EXECUTE FUNCTION users_min_age();

-- Keyset paging indexes (created_at, id) for the feeds.
CREATE INDEX posts_feed_keyset_idx ON posts (created_at DESC, id DESC) WHERE deleted_at IS NULL;
CREATE INDEX reels_feed_keyset_idx ON reels (created_at DESC, id DESC) WHERE status = 'ready' AND deleted_at IS NULL;

-- The fixed interest list, in English and Khmer.
INSERT INTO interests (name_en, name_km) VALUES
    ('Music',        'តន្ត្រី'),
    ('Travel',       'ការធ្វើដំណើរ'),
    ('Food',         'អាហារ'),
    ('Coffee',       'កាហ្វេ'),
    ('Movies',       'ភាពយន្ត'),
    ('Photography',  'ការថតរូប'),
    ('Sports',       'កីឡា'),
    ('Football',     'បាល់ទាត់'),
    ('Fitness',      'ហាត់ប្រាណ'),
    ('Reading',      'ការអាន'),
    ('Gaming',       'ហ្គេម'),
    ('Art',          'សិល្បៈ'),
    ('Dancing',      'ការរាំ'),
    ('Cooking',      'ការធ្វើម្ហូប'),
    ('Technology',   'បច្ចេកវិទ្យា'),
    ('Fashion',      'ម៉ូដ'),
    ('Nature',       'ធម្មជាតិ'),
    ('Pets',         'សត្វចិញ្ចឹម'),
    ('Volunteering', 'ការងារស្ម័គ្រចិត្ត'),
    ('Business',     'អាជីវកម្ម');
