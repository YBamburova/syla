package com.syla.core.workouts;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
/** Explicit allowlist: never expose the private input/metrics object through social reads. */
public record SharedActivityView(UUID id, UUID ownerId, ActivityInput.Sport sport, String title,
        LocalDate trainingDate, int durationSeconds, BigDecimal distanceMeters,
        BigDecimal averageSpeedKph, BigDecimal paceSecondsPerKm, BigDecimal swimPaceSecondsPer100m) {
    public record SharedPage(List<SharedActivityView> items, long total, int page, int size) {}
}
