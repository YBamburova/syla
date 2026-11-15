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
    private final com.syla.core.identity.AthleteDirectory directory;
    private final com.syla.core.social.SocialData social;
    private final WorkoutRepository workouts;
    private final SubmissionRepository submissions;
    private final ObjectMapper mapper;
    private final ApplicationEventPublisher events;

    WorkoutService(AthleteAccess access, WorkoutRepository workouts, SubmissionRepository submissions, ObjectMapper mapper, ApplicationEventPublisher events, com.syla.core.identity.AthleteDirectory directory, com.syla.core.social.SocialData social) {
        this.access = access; this.workouts = workouts; this.submissions = submissions; this.mapper = mapper; this.events = events; this.directory=directory; this.social=social;
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

    WorkoutView share(Authentication auth, UUID id, SharingInput input, String ifMatch) {
        UUID owner=access.lockActive(auth, true);
        var workout=owned(id,owner);
        revision(workout,ifMatch);
        if (input.visibility()==SharingInput.Visibility.FOLLOWERS && directory.own(auth)==null)
            throw new ApiProblem(409,"Create your athlete profile before sharing activities.");
        workout.visibility=input.visibility().name();
        workouts.flush();
        events.publishEvent(new WorkoutChanged(owner,id,workout.revision,"UPDATED"));
        return view(workout);
    }

    SharedActivityView.SharedPage sharedList(Authentication auth, UUID owner, int page, int size) {
        UUID viewer=directory.actor(auth);
        directory.lockParticipants(viewer,owner);
        if (!social.canRead(viewer,owner)) throw new ApiProblem(404,"Activities not found.");
        if (page<0 || size<1 || size>100) throw new ApiProblem(400,"Invalid page or size.");
        var result=workouts.findByAthleteIdAndVisibility(owner,"FOLLOWERS",PageRequest.of(page,size,Sort.by(Sort.Order.desc("trainingDate"),Sort.Order.desc("id"))));
        return new SharedActivityView.SharedPage(result.stream().map(this::sharedView).toList(),result.getTotalElements(),page,size);
    }

    SharedActivityView sharedGet(Authentication auth, UUID id) {
        UUID viewer=directory.actor(auth);
        // Lock before the final read so revocation/deletion and sharing changes serialize with it.
        UUID owner=workouts.ownerOf(id).orElseThrow(() -> new ApiProblem(404,"Activity not found."));
        directory.lockParticipants(viewer,owner);
        var workout=workouts.findById(id).orElseThrow(() -> new ApiProblem(404,"Activity not found."));
        if (!workout.visibility.equals("FOLLOWERS") || !social.canRead(viewer,owner)) throw new ApiProblem(404,"Activity not found.");
        return sharedView(workout);
    }

    private SharedActivityView sharedView(Workout workout) {
        var summary=view(workout);
        return new SharedActivityView(workout.id,workout.athleteId,workout.sport,workout.title,workout.trainingDate,
                workout.durationSeconds,summary.distanceMeters(),summary.averageSpeedKph(),summary.paceSecondsPerKm(),summary.swimPaceSecondsPer100m());
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
                pace == null ? null : pace.movePointLeft(1), workout.visibility);
    }

    @Override public List<WorkoutView> export(Authentication auth) {
        return workouts.findByAthleteIdOrderByTrainingDateDescCreatedAtDesc(access.lockActive(auth, false)).stream().map(this::view).toList();
    }

    @Override public void deleteAllForAccount(UUID owner) {
        workouts.deleteByAthleteId(owner); submissions.deleteByAthleteId(owner);
    }
}
