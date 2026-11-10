CREATE TABLE workouts.sessions (
    id uuid PRIMARY KEY,
    owner_id uuid NOT NULL,
    section_id uuid NOT NULL,
    revision bigint NOT NULL DEFAULT 0 CHECK (revision >= 0),
    training_date date NOT NULL,
    time_zone varchar(64) NOT NULL,
    title varchar(120),
    notes varchar(2000),
    format varchar(16) NOT NULL CHECK (format IN ('STRENGTH','AMRAP','FOR_TIME')),
    section_data jsonb NOT NULL CHECK (jsonb_typeof(section_data) = 'object'),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);
CREATE INDEX sessions_history ON workouts.sessions (owner_id, training_date DESC, id DESC);
CREATE TABLE workouts.create_requests (
    id uuid PRIMARY KEY,
    actor_id uuid NOT NULL,
    request_key uuid NOT NULL,
    fingerprint varchar(64) NOT NULL,
    workout_id uuid NOT NULL,
    outcome_revision bigint NOT NULL,
    expires_at timestamptz NOT NULL,
    UNIQUE (actor_id, request_key)
);
CREATE INDEX create_requests_expiry ON workouts.create_requests (expires_at);
