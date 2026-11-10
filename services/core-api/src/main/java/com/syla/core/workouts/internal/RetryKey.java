package com.syla.core.workouts.internal;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import com.syla.core.ApiFailure;
import org.springframework.http.HttpStatus;

final class RetryKey {
    private static final Duration RETRY_WINDOW = Duration.ofHours(24);
    private static final Duration FUTURE_SKEW = Duration.ofMinutes(5);
    private RetryKey() {}

    static Instant expiresAt(UUID key, Clock clock) {
        if (key.version() != 7 || key.variant() != 2) throw invalid();
        var created = Instant.ofEpochMilli(key.getMostSignificantBits() >>> 16);
        var expiry = created.plus(RETRY_WINDOW);
        if (created.isAfter(clock.instant().plus(FUTURE_SKEW)) || !expiry.isAfter(clock.instant())) throw invalid();
        return expiry;
    }

    private static ApiFailure invalid() {
        return new ApiFailure(HttpStatus.BAD_REQUEST, "invalid_retry_key", "Use a fresh UUIDv7 key for a new save; retain it for retries within 24 hours.");
    }
}
