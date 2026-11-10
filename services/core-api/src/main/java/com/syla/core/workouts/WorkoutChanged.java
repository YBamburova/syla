package com.syla.core.workouts;
import java.util.UUID;
/** Transactional internal fact; consumers are added only with real derived behavior. */
public record WorkoutChanged(UUID workoutId, long revision) {}
