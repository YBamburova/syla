package com.syla.core.identity.internal;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "athlete", schema = "identity")
class Athlete {
    @Id UUID id;
    @Column(name = "identity_key", nullable = false, unique = true) String identityKey;
    @Column(nullable = false) String state;
    @Column(name = "consent_version") String consentVersion;
    @Column(name = "consented_at") Instant consentedAt;
    @Column(name = "deleted_at") Instant deletedAt;
    String username;
    @Column(name = "first_name") String firstName;
    @Column(name = "last_name") String lastName;
    protected Athlete() {}
}
