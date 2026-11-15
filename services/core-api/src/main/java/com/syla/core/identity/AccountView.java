package com.syla.core.identity;

import java.time.Instant;
import java.util.UUID;

public record AccountView(boolean authenticated, UUID athleteId, String displayName, String state,
                          boolean consented, String consentVersion, Instant consentedAt) {}
