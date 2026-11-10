package com.syla.core.workouts;
import java.util.UUID;
/** Transactional internal fact; contains no training payload. */
public record WorkoutDeleted(UUID workoutId, long revision) {}
