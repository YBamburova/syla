package com.syla.core.identity;

import java.time.Instant;
import java.util.UUID;

/** Query the current verified actor inside a transaction. Holds its row lock until commit. */
public interface ActorAccess {
    /** Requires an active account; never provisions an account from an API request. */
    Actor lockActive();
    /** Allows a pending deletion so export and retry remain possible. */
    Actor lockForAccountLifecycle();

    record Actor(UUID id, String issuer, String subject, String state, Instant createdAt) {}
}
