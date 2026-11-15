package com.syla.core.workouts.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syla.core.identity.AthleteAccess;
import com.syla.core.platform.ApiProblem;
import com.syla.core.workouts.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class WorkoutService implements WorkoutData {
    private final AthleteAccess access;
    private final WorkoutRepository workouts;
    private final SubmissionRepository submissions;
    private final ObjectMapper mapper;
    private final ApplicationEventPublisher events;

    WorkoutService(AthleteAccess access, WorkoutRepository workouts, SubmissionRepository submissions, ObjectMapper mapper, ApplicationEventPublisher events) {
        this.access = access; this.workouts = workouts; this.submissions = submissions; this.mapper = mapper; this.events = events;
    }

    private String hash(byte[] input) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input)); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    WorkoutView create(Authentication auth, ActivityInput input, UUID key) {
        UUID owner = access.lockActive(auth, true);
        ActivityRules.validate(input, Clock.systemUTC());
        String submissionId = hash((owner + ":" + key).getBytes(StandardCharsets.UTF_8));
        String payloadHash;
        try { payloadHash = hash(mapper.writeValueAsBytes(input)); }
        catch (JsonProcessingException exception) { throw new IllegalStateException(exception); }
        var prior = submissions.findById(submissionId);
        if (prior.isPresent()) {
            if (!prior.get().payloadHash.equals(payloadHash)) throw new ApiProblem(409, "This save key was already used for different activity details.");
            return view(workouts.findByIdAndAthleteId(prior.get().workoutId, owner)
                    .orElseThrow(() -> new ApiProblem(410, "This activity was deleted. Start a new entry to log it again.")));
        }
        Workout workout = new Workout();
        workout.id = UUID.randomUUID(); workout.athleteId = owner; workout.createdAt = Instant.now(); workout.replace(input);
        workouts.saveAndFlush(workout);
        Submission submission = new Submission();
        submission.id = submissionId; submission.athleteId = owner; submission.payloadHash = payloadHash;
        submission.workoutId = workout.id; submission.createdAt = Instant.now();
        submissions.save(submission);
        events.publishEvent(new WorkoutChanged(owner, workout.id, workout.revision, "CREATED"));
        return view(workout);
    }

    WorkoutView.Page list(Authentication auth, ActivityInput.Sport sport, int page, int size) {
        UUID owner = access.lockActive(auth, false);
        if (page < 0 || size < 1 || size > 100) throw new ApiProblem(400, "Page must be nonnegative and page size must be 1–100.");
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("trainingDate"), Sort.Order.desc("startTime"), Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        var result = sport == null ? workouts.findByAthleteId(owner, pageable) : workouts.findByAthleteIdAndSport(owner, sport, pageable);
        return new WorkoutView.Page(result.map(this::view).toList(), result.getTotalElements(), page, size);
    }

    WorkoutView get(Authentication auth, UUID id) {
        return view(owned(id, access.lockActive(auth, false)));
    }

    private Workout owned(UUID id, UUID owner) {
        return workouts.findByIdAndAthleteId(id, owner).orElseThrow(() -> new ApiProblem(404, "Activity not found."));
    }

    WorkoutView update(Authentication auth, UUID id, ActivityInput input, String ifMatch) {
        UUID owner = access.lockActive(auth, true);
        Workout workout = owned(id, owner);
        revision(workout, ifMatch);
        ActivityRules.validate(input, Clock.systemUTC());
        workout.replace(input); workouts.flush();
        events.publishEvent(new WorkoutChanged(owner, id, workout.revision, "UPDATED"));
        return view(workout);
    }

    void delete(Authentication auth, UUID id, String ifMatch) {
        UUID owner = access.lockActive(auth, false);
        var result = workouts.findByIdAndAthleteId(id, owner);
        if (result.isEmpty()) throw new ApiProblem(404, "Activity not found.");
        Workout workout = result.get(); revision(workout, ifMatch);
        workouts.delete(workout);
        events.publishEvent(new WorkoutChanged(owner, id, workout.revision, "DELETED"));
    }

    private void revision(Workout workout, String value) {
        if (value == null) throw new ApiProblem(428, "Reload the activity before changing it.");
        if (!value.equals("\"" + workout.revision + "\"")) throw new ApiProblem(409, "This activity changed elsewhere. Reload it before saving; your entered values have been kept.");
    }

    private WorkoutView view(Workout workout) {
        var input = workout.input();
        BigDecimal distance = ActivityRules.distance(input);
        int seconds = input.metrics().movingSeconds() == null ? input.durationSeconds() : input.metrics().movingSeconds();
        BigDecimal speed = distance == null ? null : ActivityRules.ratio(distance.multiply(new BigDecimal("3.6")), BigDecimal.valueOf(seconds));
        BigDecimal pace = distance == null ? null : ActivityRules.ratio(BigDecimal.valueOf(seconds * 1000L), distance);
        return new WorkoutView(workout.id, workout.revision, input, workout.createdAt, distance, speed, pace,
                pace == null ? null : pace.movePointLeft(1), "PRIVATE");
    }

    @Override public List<WorkoutView> export(Authentication auth) {
        return workouts.findByAthleteIdOrderByTrainingDateDescCreatedAtDesc(access.lockActive(auth, false)).stream().map(this::view).toList();
    }

    @Override public void deleteAllForAccount(UUID owner) {
        workouts.deleteByAthleteId(owner); submissions.deleteByAthleteId(owner);
    }
}
