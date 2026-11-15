package com.syla.core.identity;

import java.util.UUID;

/** Deliberately excludes provider identity, consent and training data. */
public record ProfileView(UUID id, String handle, String displayName, String bio, boolean privateProfile) {}
