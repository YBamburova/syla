package com.syla.core.identity;

import java.util.UUID;

/** Synchronous transaction event: consumers purge owned data before the deletion barrier commits. */
public record AccountDeletionStarted(UUID actorId) {}
