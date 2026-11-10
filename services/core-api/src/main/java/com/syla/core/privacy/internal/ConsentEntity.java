package com.syla.core.privacy.internal;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "consents", schema = "privacy")
class ConsentEntity {
    @Id UUID id;
    @Column(name = "actor_id", nullable = false) UUID actorId;
    @Column(nullable = false) String version;
    @Column(nullable = false) boolean granted;
    @Column(nullable = false) String purpose;
    @Column(name = "recorded_at", nullable = false) Instant recordedAt;
    protected ConsentEntity() {}
}
