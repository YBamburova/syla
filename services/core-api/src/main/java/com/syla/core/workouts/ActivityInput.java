package com.syla.core.workouts;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record ActivityInput(
        @NotNull Sport sport,
        @Size(max = 120) String title,
        @NotNull LocalDate trainingDate,
        LocalTime startTime,
        @NotBlank @Size(max = 80) String timeZone,
        @NotNull @Min(1) @Max(604800) Integer durationSeconds,
        @NotNull @Valid Metrics metrics,
        @Min(1) @Max(10) Integer effort,
        @Size(max = 5000) String description) {
    public enum Sport { RUN, SWIM, RIDE, WORKOUT }
    public enum Venue { OUTDOOR, INDOOR, POOL, OPEN_WATER }
    public enum DistanceUnit { KM, MI, M, YD }
    public enum ElevationUnit { M, FT }
    public enum Stroke { FREESTYLE, BREASTSTROKE, BACKSTROKE, BUTTERFLY, MIXED, OTHER }

    public record Metrics(
            @NotNull Venue venue,
            @DecimalMin("0.001") @DecimalMax("1000000") BigDecimal distance,
            DistanceUnit distanceUnit,
            @Min(1) @Max(604800) Integer movingSeconds,
            @DecimalMin("0") @DecimalMax("100000") BigDecimal elevationGain,
            ElevationUnit elevationUnit,
            @Min(1) @Max(300) Integer averageHeartRate,
            @Min(1) @Max(300) Integer maxHeartRate,
            @Min(1) @Max(100000) Integer calories,
            @Min(1) @Max(300) Integer cadence,
            @Min(1) @Max(3000) Integer averagePower,
            @DecimalMin("1") @DecimalMax("200") BigDecimal poolLength,
            DistanceUnit poolLengthUnit,
            @Min(1) @Max(100000) Integer lengths,
            Stroke stroke,
            @Size(max = 120) String gear) {}
}
