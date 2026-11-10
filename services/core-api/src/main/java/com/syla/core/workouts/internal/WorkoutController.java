package com.syla.core.workouts.internal;

import java.net.URI;
import java.util.UUID;
import com.syla.core.workouts.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
class WorkoutController implements WorkoutsApi {
    private final WorkoutService workouts;
    WorkoutController(WorkoutService workouts) { this.workouts = workouts; }

    @Override
    public ResponseEntity<WorkoutView> createWorkout(UUID key, WorkoutInput input) {
        var record = workouts.create(key, input);
        return ResponseEntity.created(URI.create("/api/v1/workouts/" + record.id()))
                .eTag(Long.toString(record.revision())).body(record);
    }
    @Override
    public ResponseEntity<WorkoutPage> listWorkouts(Integer limit, String cursor) {
        return ResponseEntity.ok(workouts.list(limit, cursor));
    }
    @Override
    public ResponseEntity<WorkoutView> getWorkout(UUID id) {
        var record = workouts.get(id);
        return ResponseEntity.ok().eTag(Long.toString(record.revision())).body(record);
    }
    @Override
    public ResponseEntity<WorkoutView> replaceWorkout(UUID id, WorkoutInput input, String ifMatch) {
        var record = workouts.replace(id, ifMatch, input);
        return ResponseEntity.ok().eTag(Long.toString(record.revision())).body(record);
    }
    @Override
    public ResponseEntity<Void> deleteWorkout(UUID id, String ifMatch) {
        workouts.delete(id, ifMatch);
        return ResponseEntity.noContent().build();
    }
}
