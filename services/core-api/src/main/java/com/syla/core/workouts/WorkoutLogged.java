package com.syla.core.workouts;

import java.util.UUID;

/** Transactional internal fact. No workout payload is exposed to listeners or logs. */
public record WorkoutLogged(UUID workoutId, long revision) {}
