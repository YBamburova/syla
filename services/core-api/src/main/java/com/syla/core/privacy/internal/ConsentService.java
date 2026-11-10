package com.syla.core.privacy.internal;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import com.syla.core.ApiFailure;
import com.syla.core.identity.ActorAccess;
import com.syla.core.identity.AccountDeletionStarted;
import com.syla.core.privacy.*;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class ConsentService implements TrainingConsent {
    private static final String PURPOSE = "Store private training history for the local demo";
    private final ConsentRepository consents;
    private final ActorAccess actors;
    ConsentService(ConsentRepository consents, ActorAccess actors) { this.consents = consents; this.actors = actors; }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void requireGranted(UUID actorId) {
        if (!consents.findFirstByActorIdOrderByRecordedAtDescIdDesc(actorId)
                .map(consent -> consent.granted && consent.version.equals("v1")).orElse(false)) {
            throw new ApiFailure(HttpStatus.FORBIDDEN, "consent_required", "Grant training consent before accessing workouts.");
        }
    }

    @Transactional
    public ConsentView set(ConsentInput input) {
        var actor = actors.lockActive();
        var consent = new ConsentEntity();
        consent.id = UUID.randomUUID(); consent.actorId = actor.id(); consent.version = "v1";
        consent.granted = input.granted(); consent.purpose = PURPOSE; consent.recordedAt = Instant.now();
        return view(consents.save(consent));
    }

    @Transactional
    public ConsentView current() {
        var actor = actors.lockActive();
        return consents.findFirstByActorIdOrderByRecordedAtDescIdDesc(actor.id()).map(this::view)
                .orElseThrow(() -> new ApiFailure(HttpStatus.NOT_FOUND, "consent_missing", "No training consent is recorded."));
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void export(PersonalDataExport query) {
        query.add("consents", consents.findByActorIdOrderByRecordedAtAscIdAsc(query.actorId()).stream().map(this::view).toList());
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void delete(AccountDeletionStarted event) { consents.deleteByActorId(event.actorId()); }

    private ConsentView view(ConsentEntity consent) {
        return new ConsentView(consent.version, consent.granted, consent.purpose, consent.recordedAt.atOffset(ZoneOffset.UTC));
    }
}
