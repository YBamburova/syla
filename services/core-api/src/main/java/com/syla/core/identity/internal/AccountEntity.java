package com.syla.core.identity.internal;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "accounts", schema = "identity")
class AccountEntity {
    @Id UUID id;
    @Column(nullable = false) String issuer;
    @Column(nullable = false) String subject;
    @Column(nullable = false) String state;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "deleted_at") Instant deletedAt;
    protected AccountEntity() {}
}
