package com.syla.core.workouts.internal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "sessions", schema = "workouts")
class WorkoutEntity {
    @Id UUID id;
    @Column(name = "owner_id", nullable = false) UUID ownerId;
    @Column(name = "section_id", nullable = false) UUID sectionId;
    @Version long revision;
    @Column(name = "training_date", nullable = false) LocalDate trainingDate;
    @Column(name = "time_zone", nullable = false) String timeZone;
    String title;
    String notes;
    @Column(nullable = false) String format;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "section_data", columnDefinition = "jsonb", nullable = false) String sectionData;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
    protected WorkoutEntity() {}
}
