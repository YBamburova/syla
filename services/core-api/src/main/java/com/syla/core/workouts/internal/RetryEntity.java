package com.syla.core.workouts.internal;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "create_requests", schema = "workouts")
class RetryEntity {
    @Id UUID id;
    @Column(name = "actor_id", nullable = false) UUID actorId;
    @Column(name = "request_key", nullable = false) UUID key;
    @Column(nullable = false) String fingerprint;
    @Column(name = "workout_id", nullable = false) UUID workoutId;
    @Column(name = "outcome_revision", nullable = false) long outcomeRevision;
    @Column(name = "expires_at", nullable = false) Instant expiresAt;
    protected RetryEntity() {}
}
