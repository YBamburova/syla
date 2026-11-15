package com.syla.core.workouts;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface WorkoutData {
    List<WorkoutView> export(Authentication authentication);
    void deleteAllForAccount(UUID athleteId);
}
