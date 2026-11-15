package com.syla.core.workouts.internal;

import com.syla.core.platform.ApiProblem;
import com.syla.core.workouts.ActivityInput;
import com.syla.core.workouts.ActivityInput.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.LinkedHashMap;
import java.util.Map;

final class ActivityRules {
    private ActivityRules() {}

    static void validate(ActivityInput input, Clock clock) {
        Map<String, String> errors = new LinkedHashMap<>();
        ZoneId zone = null;
        try { zone = ZoneId.of(input.timeZone()); }
        catch (DateTimeException exception) { errors.put("timeZone", "Choose a valid IANA time zone."); }
        if (zone != null) {
            var now = ZonedDateTime.now(clock.withZone(zone));
            if (input.trainingDate().isAfter(now.toLocalDate()) || input.trainingDate().isBefore(LocalDate.of(1900, 1, 1))) {
                errors.put("trainingDate", "Choose today or a past date after 1900.");
            }
            if (input.startTime() != null) {
                var local = LocalDateTime.of(input.trainingDate(), input.startTime());
                if (local.isAfter(now.toLocalDateTime())) errors.put("startTime", "Start time cannot be in the future.");
                if (zone.getRules().getValidOffsets(local).isEmpty()) errors.put("startTime", "This time does not exist because of a daylight-saving change.");
            }
        }
        Metrics m = input.metrics();
        if ((m.distance() == null) != (m.distanceUnit() == null)) errors.put("metrics.distance", "Enter a distance and its unit together.");
        if (m.movingSeconds() != null && m.movingSeconds() > input.durationSeconds()) errors.put("metrics.movingSeconds", "Moving time cannot exceed total duration.");
        if (m.averageHeartRate() != null && m.maxHeartRate() != null && m.averageHeartRate() > m.maxHeartRate()) errors.put("metrics.maxHeartRate", "Maximum heart rate cannot be below average heart rate.");
        if ((m.elevationGain() == null) != (m.elevationUnit() == null)) errors.put("metrics.elevationGain", "Enter elevation and its unit together.");
        boolean swim = input.sport() == Sport.SWIM;
        boolean pool = swim && m.venue() == Venue.POOL;
        if (swim != (m.venue() == Venue.POOL || m.venue() == Venue.OPEN_WATER)) errors.put("metrics.venue", "Choose a setting that matches this sport.");
        if (input.sport() == Sport.WORKOUT && (m.distance() != null || m.movingSeconds() != null)) errors.put("metrics.distance", "General workouts use duration; choose a distance sport to log distance.");
        if ((swim || input.sport() == Sport.WORKOUT || m.venue() == Venue.INDOOR) && m.elevationGain() != null) errors.put("metrics.elevationGain", "Elevation is available for outdoor runs and rides.");
        if (input.sport() != Sport.RIDE && m.averagePower() != null) errors.put("metrics.averagePower", "Power is available for cycling.");
        if (input.sport() != Sport.RUN && input.sport() != Sport.RIDE && m.cadence() != null) errors.put("metrics.cadence", "Cadence is available for running and cycling.");
        if (!swim && m.stroke() != null) errors.put("metrics.stroke", "Stroke is available for swimming.");
        if (!pool && (m.poolLength() != null || m.poolLengthUnit() != null || m.lengths() != null)) errors.put("metrics.poolLength", "Pool details are available for pool swimming.");
        if (pool && (m.poolLength() != null || m.poolLengthUnit() != null || m.lengths() != null)) {
            if (m.poolLength() == null || m.poolLengthUnit() == null || m.lengths() == null) errors.put("metrics.poolLength", "Enter pool length, its unit, and lengths swum together.");
            else if (m.poolLengthUnit() != DistanceUnit.M && m.poolLengthUnit() != DistanceUnit.YD) errors.put("metrics.poolLengthUnit", "Pool length uses meters or yards.");
            else if (m.distance() != null && m.distanceUnit() != null) {
                var expected = meters(m.poolLength().multiply(BigDecimal.valueOf(m.lengths())), m.poolLengthUnit());
                if (expected.subtract(meters(m.distance(), m.distanceUnit())).abs().compareTo(new BigDecimal("0.01")) > 0) errors.put("metrics.distance", "Distance must match pool length × lengths swum.");
            }
        }
        if (!errors.isEmpty()) throw new ApiProblem(400, "Check the highlighted fields.", errors);
    }

    static BigDecimal meters(BigDecimal value, DistanceUnit unit) {
        if (value == null || unit == null) return null;
        return value.multiply(switch (unit) {
            case M -> BigDecimal.ONE;
            case KM -> BigDecimal.valueOf(1000);
            case MI -> new BigDecimal("1609.344");
            case YD -> new BigDecimal("0.9144");
        });
    }

    static BigDecimal distance(ActivityInput input) {
        var m = input.metrics();
        if (m.distance() != null) return meters(m.distance(), m.distanceUnit());
        if (m.poolLength() != null && m.lengths() != null) return meters(m.poolLength().multiply(BigDecimal.valueOf(m.lengths())), m.poolLengthUnit());
        return null;
    }

    static BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP);
    }
}
