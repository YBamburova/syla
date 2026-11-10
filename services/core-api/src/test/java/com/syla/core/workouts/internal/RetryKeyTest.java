package com.syla.core.workouts.internal;

import java.time.*;
import java.util.UUID;
import com.syla.core.ApiFailure;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class RetryKeyTest {
    private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    @Test void acceptsUuidV7AndUsesItsTimestampForExpiry() {
        assertThat(RetryKey.expiresAt(key(NOW), CLOCK)).isEqualTo(NOW.plus(Duration.ofHours(24)));
    }
    @Test void expiredKeysCannotBecomeNewCreatesAfterTheirLedgerIsPurged() {
        assertThatThrownBy(() -> RetryKey.expiresAt(key(NOW.minus(Duration.ofHours(24))), CLOCK)).isInstanceOf(ApiFailure.class);
    }
    @Test void rejectsOtherVersionsAndExcessFutureClockSkew() {
        assertThatThrownBy(() -> RetryKey.expiresAt(UUID.randomUUID(), CLOCK)).isInstanceOf(ApiFailure.class);
        assertThatThrownBy(() -> RetryKey.expiresAt(key(NOW.plusSeconds(301)), CLOCK)).isInstanceOf(ApiFailure.class);
    }
    private UUID key(Instant time) { return new UUID((time.toEpochMilli() << 16) | 0x7000L, 0x8000000000000000L); }
}
