package com.syla.core.privacy;

import java.util.UUID;

/** Read consent under the caller's active actor lock in the current transaction. */
public interface TrainingConsent {
    void requireGranted(UUID actorId);
}
