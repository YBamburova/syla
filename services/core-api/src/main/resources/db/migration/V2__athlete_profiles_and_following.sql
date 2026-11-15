create table identity.profile (
    athlete_id uuid primary key,
    handle varchar(30) not null unique,
    display_name varchar(80) not null,
    bio varchar(500) not null,
    private_profile boolean not null default true
);
create schema social;
create table social.follow (
    follower_id uuid not null,
    athlete_id uuid not null,
    status varchar(8) not null check (status in ('PENDING', 'ACCEPTED')),
    created_at timestamptz not null default now(),
    primary key (follower_id, athlete_id),
    check (follower_id <> athlete_id)
);
create index follow_target on social.follow(athlete_id, status);
create table social.block (
    blocker_id uuid not null,
    blocked_id uuid not null,
    created_at timestamptz not null default now(),
    primary key (blocker_id, blocked_id),
    check (blocker_id <> blocked_id)
);
create index block_target on social.block(blocked_id);
alter table workouts.activity add column visibility varchar(9) not null default 'PRIVATE'
    check (visibility in ('PRIVATE', 'FOLLOWERS'));
create index activity_shared_owner on workouts.activity(athlete_id, training_date desc, id) where visibility='FOLLOWERS';
