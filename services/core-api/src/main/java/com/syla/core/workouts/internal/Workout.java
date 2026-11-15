package com.syla.core.workouts.internal;

import com.syla.core.workouts.ActivityInput;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "activity", schema = "workouts")
class Workout {
    @Id UUID id;
    @Column(name = "athlete_id", nullable = false) UUID athleteId;
    @Version long revision;
    @Enumerated(EnumType.STRING) ActivityInput.Sport sport;
    String title;
    @Column(name = "training_date") LocalDate trainingDate;
    @Column(name = "start_time") LocalTime startTime;
    @Column(name = "time_zone") String timeZone;
    @Column(name = "duration_seconds") int durationSeconds;
    @JdbcTypeCode(SqlTypes.JSON) ActivityInput.Metrics metrics;
    String visibility = "PRIVATE";
    Integer effort;
    String description;
    @Column(name = "created_at") Instant createdAt;
    protected Workout() {}

    void replace(ActivityInput input) {
        sport = input.sport();
        title = input.title() == null || input.title().isBlank() ? switch (sport) {
            case RUN -> "Run"; case SWIM -> "Swim"; case RIDE -> "Ride"; case WORKOUT -> "Workout";
        } : input.title().strip();
        trainingDate = input.trainingDate(); startTime = input.startTime(); timeZone = input.timeZone();
        durationSeconds = input.durationSeconds(); metrics = input.metrics(); effort = input.effort();
        description = input.description();
    }

    ActivityInput input() { return new ActivityInput(sport, title, trainingDate, startTime, timeZone, durationSeconds, metrics, effort, description); }
}
