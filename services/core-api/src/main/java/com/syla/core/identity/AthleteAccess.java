package com.syla.core.identity;

import java.util.UUID;
import org.springframework.security.core.Authentication;

public interface AthleteAccess {
    AccountView current(Authentication authentication);
    /** Lock the active account in the caller transaction; optionally require recorded training consent. */
    UUID lockActive(Authentication authentication, boolean consentRequired);
    void consent(Authentication authentication);
    /** Persist a deletion barrier before other modules or the identity provider are cleaned up. */
    UUID beginDeletion(Authentication authentication);
    void completeDeletion(Authentication authentication);
}
