package com.syla.core.workouts.internal;

import com.syla.core.workouts.ActivityInput.Sport;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface WorkoutRepository extends JpaRepository<Workout, UUID> {
    Optional<Workout> findByIdAndAthleteId(UUID id, UUID athleteId);
    Page<Workout> findByAthleteId(UUID athleteId, Pageable pageable);
    Page<Workout> findByAthleteIdAndSport(UUID athleteId, Sport sport, Pageable pageable);
    List<Workout> findByAthleteIdOrderByTrainingDateDescCreatedAtDesc(UUID athleteId);
    void deleteByAthleteId(UUID athleteId);
}
