create schema if not exists identity;
create schema if not exists workouts;
create table identity.athlete (
    id uuid primary key,
    identity_key varchar(64) not null unique,
    state varchar(16) not null check (state in ('ACTIVE', 'DELETING', 'DELETED')),
    consent_version varchar(40),
    consented_at timestamptz,
    deleted_at timestamptz
);
create table workouts.activity (
    id uuid primary key,
    athlete_id uuid not null,
    revision bigint not null default 0,
    sport varchar(16) not null check (sport in ('RUN', 'SWIM', 'RIDE', 'WORKOUT')),
    title varchar(120) not null,
    training_date date not null,
    start_time time,
    time_zone varchar(80) not null,
    duration_seconds integer not null check (duration_seconds between 1 and 604800),
    metrics jsonb not null,
    effort integer check (effort between 1 and 10),
    description varchar(5000),
    created_at timestamptz not null
);
create index activity_owner_history on workouts.activity(athlete_id, training_date desc, created_at desc);
create table workouts.submission (
    id varchar(64) primary key,
    athlete_id uuid not null,
    payload_hash varchar(64) not null,
    workout_id uuid not null,
    created_at timestamptz not null
);
create index submission_owner on workouts.submission(athlete_id);
