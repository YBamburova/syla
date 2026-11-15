package com.syla.core.workouts.internal;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "submission", schema = "workouts")
class Submission {
    @Id String id;
    @Column(name = "athlete_id") UUID athleteId;
    @Column(name = "payload_hash") String payloadHash;
    @Column(name = "workout_id") UUID workoutId;
    @Column(name = "created_at") Instant createdAt;
    protected Submission() {}
}
