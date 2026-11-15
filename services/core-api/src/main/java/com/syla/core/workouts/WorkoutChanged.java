package com.syla.core.workouts;

import java.util.UUID;

public record WorkoutChanged(UUID athleteId, UUID workoutId, long revision, String change) {}
