CREATE TABLE privacy.consents (
    id uuid PRIMARY KEY,
    actor_id uuid NOT NULL,
    version varchar(20) NOT NULL,
    granted boolean NOT NULL,
    purpose varchar(200) NOT NULL,
    recorded_at timestamptz NOT NULL
);
CREATE INDEX consents_actor ON privacy.consents (actor_id, recorded_at DESC, id DESC);
