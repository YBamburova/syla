package com.syla.core.workouts;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkoutView(@NotNull UUID id, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long revision, @NotNull ActivityInput activity, @NotNull Instant createdAt,
                          BigDecimal distanceMeters, BigDecimal averageSpeedKph,
                          BigDecimal paceSecondsPerKm, BigDecimal swimPaceSecondsPer100m,
                          @NotNull String visibility) {
    public record Page(@NotNull List<WorkoutView> items, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long total, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int page, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int size) {}
}
