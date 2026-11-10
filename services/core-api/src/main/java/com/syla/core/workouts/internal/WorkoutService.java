package com.syla.core.workouts.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.syla.core.ApiFailure;
import com.syla.core.identity.ActorAccess;
import com.syla.core.identity.AccountDeletionStarted;
import com.syla.core.privacy.TrainingConsent;
import com.syla.core.privacy.PersonalDataExport;
import com.syla.core.workouts.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class WorkoutService {
    private final WorkoutRepository workouts;
    private final RetryRepository retries;
    private final ActorAccess actors;
    private final TrainingConsent consents;
    private final WorkoutValidator validator;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    WorkoutService(WorkoutRepository workouts, RetryRepository retries, ActorAccess actors, TrainingConsent consents,
                   WorkoutValidator validator, ObjectMapper mapper, Clock clock, ApplicationEventPublisher events) {
        this.workouts = workouts; this.retries = retries; this.actors = actors; this.consents = consents;
        this.validator = validator; this.mapper = mapper; this.clock = clock; this.events = events;
    }

    @Transactional
    public WorkoutView create(UUID key, WorkoutInput request) {
        var actor = authorizedActor();
        var expires = RetryKey.expiresAt(key, clock);
        var normalized = validator.normalize(request);
        var fingerprint = fingerprint(normalized);
        var previous = retries.findByActorIdAndKey(actor, key);
        if (previous.isPresent()) {
            var replay = previous.get();
            if (!replay.fingerprint.equals(fingerprint)) throw conflict("retry_payload_changed");
            var record = workouts.findByIdAndOwnerId(replay.workoutId, actor)
                    .orElseThrow(() -> conflict("retry_record_deleted"));
            if (record.revision != replay.outcomeRevision) throw conflict("retry_record_changed");
            return view(record);
        }
        var record = new WorkoutEntity();
        record.id = UUID.randomUUID(); record.ownerId = actor; record.sectionId = UUID.randomUUID();
        record.createdAt = clock.instant().truncatedTo(ChronoUnit.MICROS);
        apply(record, normalized);
        record = workouts.saveAndFlush(record);
        var saved = new RetryEntity();
        saved.id = UUID.randomUUID(); saved.actorId = actor; saved.key = key; saved.fingerprint = fingerprint;
        saved.workoutId = record.id; saved.outcomeRevision = record.revision; saved.expiresAt = expires;
        retries.saveAndFlush(saved);
        events.publishEvent(new WorkoutLogged(record.id, record.revision));
        return view(record);
    }

    @Transactional
    public WorkoutView get(UUID id) { return view(owned(id, authorizedActor())); }

    @Transactional
    public WorkoutPage list(int limit, String cursor) {
        var actor = authorizedActor();
        if (limit < 1 || limit > 100) throw invalidCursor();
        LocalDate date = null;
        UUID id = null;
        if (cursor != null) {
            try {
                var parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split("/", -1);
                if (parts.length != 2) throw invalidCursor();
                date = LocalDate.parse(parts[0]); id = UUID.fromString(parts[1]);
            } catch (IllegalArgumentException | java.time.DateTimeException failure) { throw invalidCursor(); }
        }
        var records = workouts.page(actor, date, id, limit + 1);
        var items = records.stream().limit(limit).map(this::view).toList();
        String next = null;
        if (records.size() > limit) {
            var last = records.get(limit - 1);
            next = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    (last.trainingDate + "/" + last.id).getBytes(StandardCharsets.UTF_8));
        }
        return new WorkoutPage(items, next);
    }

    @Transactional
    public WorkoutView replace(UUID id, String revision, WorkoutInput request) {
        var record = owned(id, authorizedActor());
        expectedRevision(record, revision);
        apply(record, validator.normalize(request));
        workouts.flush();
        events.publishEvent(new WorkoutChanged(record.id, record.revision));
        return view(record);
    }

    @Transactional
    public void delete(UUID id, String revision) {
        var record = owned(id, authorizedActor());
        expectedRevision(record, revision);
        workouts.delete(record);
        events.publishEvent(new WorkoutDeleted(record.id, record.revision));
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteAccount(AccountDeletionStarted event) {
        retries.deleteByActorId(event.actorId());
        workouts.deleteByOwnerId(event.actorId());
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void export(PersonalDataExport query) {
        query.add("workouts", workouts.findByOwnerIdOrderByTrainingDateDescIdDesc(query.actorId()).stream().map(this::view).toList());
        query.add("createRequests", retries.findByActorId(query.actorId()).stream()
                .map(key -> new RetryAudit(key.key, key.fingerprint, key.workoutId, key.outcomeRevision, key.expiresAt)).toList());
    }

    @Scheduled(fixedDelayString = "${syla.workouts.cleanup-delay-ms:3600000}")
    @Transactional
    public void purgeRetryRecords() { retries.purgeExpired(clock.instant()); }

    private UUID authorizedActor() {
        var actor = actors.lockActive();
        consents.requireGranted(actor.id());
        return actor.id();
    }

    private WorkoutEntity owned(UUID id, UUID actor) {
        return workouts.findByIdAndOwnerId(id, actor).orElseThrow(() -> new ApiFailure(HttpStatus.NOT_FOUND,
                "workout_not_found", "Workout not found."));
    }

    private void expectedRevision(WorkoutEntity record, String header) {
        if (header == null) throw new ApiFailure(HttpStatus.PRECONDITION_REQUIRED, "revision_required", "Send If-Match with the current quoted revision.");
        try {
            if (!header.matches("\"[0-9]+\"")) throw conflict("invalid_revision");
            long value = Long.parseLong(header.substring(1, header.length() - 1));
            if (value != record.revision) throw conflict("stale_revision");
        } catch (NumberFormatException failure) { throw conflict("invalid_revision"); }
    }

    private void apply(WorkoutEntity record, WorkoutInput input) {
        record.trainingDate = input.trainingDate(); record.timeZone = input.timeZone();
        record.title = input.title(); record.notes = input.notes(); record.format = input.section().getFormat().toString();
        record.sectionData = json(input.section());
        record.updatedAt = clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    private WorkoutView view(WorkoutEntity record) {
        try {
            var section = mapper.readValue(record.sectionData, WorkoutSection.class);
            var input = new WorkoutInput(record.trainingDate, record.timeZone, record.title, record.notes, section);
            return new WorkoutView(record.id, record.sectionId, record.revision,
                    record.createdAt.atOffset(ZoneOffset.UTC), record.updatedAt.atOffset(ZoneOffset.UTC),
                    input, WorkoutValidator.totalReps(section));
        } catch (JsonProcessingException failure) { throw new IllegalStateException("Stored workout schema is invalid", failure); }
    }

    private String fingerprint(WorkoutInput input) {
        try {
            var normalized = normalizeNumbers(mapper.valueToTree(input));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(normalized)));
        } catch (java.security.NoSuchAlgorithmException | JsonProcessingException failure) {
            throw new IllegalStateException("Request hashing failed", failure);
        }
    }

    private JsonNode normalizeNumbers(JsonNode node) {
        if (node.isNumber()) return DecimalNode.valueOf(node.decimalValue().stripTrailingZeros());
        if (node instanceof ObjectNode object) {
            object.properties().forEach(entry -> object.set(entry.getKey(), normalizeNumbers(entry.getValue())));
        } else if (node instanceof ArrayNode array) {
            for (int i = 0; i < array.size(); i++) array.set(i, normalizeNumbers(array.get(i)));
        }
        return node;
    }

    private String json(Object object) {
        try { return mapper.writeValueAsString(object); }
        catch (JsonProcessingException failure) { throw new IllegalStateException("Workout serialization failed", failure); }
    }
    private ApiFailure conflict(String code) { return new ApiFailure(HttpStatus.CONFLICT, code, "Reload the record or resolve the earlier save before retrying."); }
    private ApiFailure invalidCursor() { return new ApiFailure(HttpStatus.BAD_REQUEST, "invalid_cursor", "Invalid history cursor or limit."); }
    private record RetryAudit(UUID key, String fingerprint, UUID workoutId, long revision, Instant expiresAt) {}
}
