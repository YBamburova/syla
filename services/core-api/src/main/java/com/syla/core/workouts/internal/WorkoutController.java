package com.syla.core.workouts.internal;

import com.syla.core.workouts.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/workouts", produces = "application/json")
@Tag(name = "Workouts", description = "Private manual running, swimming, cycling, and general workout logs")
class WorkoutController {
    private final WorkoutService workouts;
    WorkoutController(WorkoutService workouts) { this.workouts = workouts; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "createWorkout", summary = "Save a private activity; retry with the same UUID idempotency key")
    ResponseEntity<WorkoutView> create(Authentication auth, @Valid @RequestBody ActivityInput input, @RequestHeader("Idempotency-Key") UUID key) {
        var result = workouts.create(auth, input, key);
        return ResponseEntity.created(URI.create("/api/v1/workouts/" + result.id())).eTag(String.valueOf(result.revision())).cacheControl(CacheControl.noStore()).body(result);
    }

    @GetMapping
    @Operation(operationId = "listWorkouts", summary = "List your activities, newest training date first")
    WorkoutView.Page list(Authentication auth, @RequestParam(required = false) ActivityInput.Sport sport,
                          @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return workouts.list(auth, sport, page, size);
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getWorkout", summary = "Read one owned activity")
    ResponseEntity<WorkoutView> get(Authentication auth, @PathVariable UUID id) {
        var result = workouts.get(auth, id);
        return ResponseEntity.ok().eTag(String.valueOf(result.revision())).body(result);
    }

    @PutMapping("/{id}")
    @Operation(operationId = "updateWorkout", summary = "Replace an activity using its quoted revision in If-Match")
    WorkoutView update(Authentication auth, @PathVariable UUID id, @Valid @RequestBody ActivityInput input,
                       @RequestHeader(value = "If-Match", required = false) String revision) {
        return workouts.update(auth, id, input, revision);
    }

    @PatchMapping("/{id}/visibility")
    @Operation(operationId="shareWorkout", summary="Explicitly share a safe summary with approved followers, or make it private")
    WorkoutView share(Authentication auth, @PathVariable UUID id, @Valid @RequestBody SharingInput input,
                      @RequestHeader(value="If-Match",required=false) String revision) {
        return workouts.share(auth,id,input,revision);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "deleteWorkout", summary = "Delete an owned activity using its quoted revision in If-Match")
    void delete(Authentication auth, @PathVariable UUID id, @RequestHeader(value = "If-Match", required = false) String revision) {
        workouts.delete(auth, id, revision);
    }
}
