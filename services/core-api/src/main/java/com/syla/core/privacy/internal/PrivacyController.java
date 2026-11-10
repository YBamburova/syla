package com.syla.core.privacy.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syla.core.identity.ActorAccess;
import com.syla.core.privacy.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.support.TransactionTemplate;

@Controller
class PrivacyController implements PrivacyApi {
    private final ConsentService consents;
    private final ActorAccess actors;
    private final ApplicationEventPublisher events;
    private final ObjectMapper mapper;
    private final TransactionTemplate transactions;
    PrivacyController(ConsentService consents, ActorAccess actors, ApplicationEventPublisher events,
                      ObjectMapper mapper, TransactionTemplate transactions) {
        this.consents = consents; this.actors = actors; this.events = events; this.mapper = mapper; this.transactions = transactions;
    }

    @Override
    public ResponseEntity<ConsentView> setTrainingConsent(ConsentInput input) { return ResponseEntity.ok(consents.set(input)); }
    @Override
    public ResponseEntity<ConsentView> getTrainingConsent() { return ResponseEntity.ok(consents.current()); }

    @Override
    public ResponseEntity<Resource> exportAccount() {
        byte[] data = transactions.execute(status -> {
            var actor = actors.lockForAccountLifecycle();
            var query = new PersonalDataExport(actor.id());
            query.add("account", actor);
            events.publishEvent(query);
            try { return mapper.writeValueAsBytes(query.sections()); }
            catch (JsonProcessingException failure) { throw new IllegalStateException("Export serialization failed", failure); }
        });
        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=syla-export.json")
                .body(new ByteArrayResource(data));
    }
}
