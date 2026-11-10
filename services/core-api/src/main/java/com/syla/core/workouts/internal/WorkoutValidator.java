package com.syla.core.workouts.internal;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.syla.core.ApiFailure;
import com.syla.core.workouts.*;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
class WorkoutValidator {
    private final Validator beans;
    private final Clock clock;
    WorkoutValidator(Validator beans, Clock clock) { this.beans = beans; this.clock = clock; }

    WorkoutInput normalize(WorkoutInput input) {
        check(input != null && beans.validate(input).isEmpty(), "Check required fields and limits.");
        check(ZoneId.getAvailableZoneIds().contains(input.timeZone()), "Use an IANA time zone.");
        check(!input.trainingDate().isAfter(LocalDate.now(clock.withZone(ZoneId.of(input.timeZone())))),
                "Future training dates are not supported.");
        WorkoutSection section = switch (input.section()) {
            case StrengthSection strength -> strength(strength);
            case AmrapSection amrap -> amrap(amrap);
            case ForTimeSection forTime -> forTime(forTime);
            default -> throw invalid("Unsupported workout format.");
        };
        return new WorkoutInput(input.trainingDate(), input.timeZone(), input.title(), input.notes(), section);
    }

    private StrengthSection strength(StrengthSection section) {
        check(section.scaling() != Scaling.RX, "Strength has no RX prescription in this slice.");
        check(section.sets().stream().allMatch(java.util.Objects::nonNull), "A prescribed set cannot be null.");
        var positions = new HashSet<Integer>();
        for (var set : section.actualSets()) {
            check(set != null && set.setIndex() < section.sets().size() && positions.add(set.setIndex()),
                    "Actual set positions must be unique and belong to the prescription.");
            load(set.load(), section.movement());
        }
        check(section.scoreState() != ScoreState.SCORED || !section.actualSets().isEmpty(),
                "Scored strength requires at least one reported set.");
        return section;
    }

    private AmrapSection amrap(AmrapSection section) {
        items(section.prescription()); items(section.actual());
        samePositions(section.prescription(), section.actual());
        check(section.scaling() != Scaling.RX || sameWork(section.prescription(), section.actual()),
                "A changed prescription cannot be RX.");
        score(section.scoreState(), section.result());
        if (section.result() == null) return section;
        int roundSize = section.actual().stream().mapToInt(WorkItem::reps).sum();
        int rounds = section.result().rounds() + section.result().additionalReps() / roundSize;
        check(rounds <= 1000000, "Round count is too large.");
        return new AmrapSection(section.format(), section.scoreState(), section.scaling(), section.durationSeconds(),
                section.prescription(), section.actual(), new AmrapResult(rounds, section.result().additionalReps() % roundSize));
    }

    private ForTimeSection forTime(ForTimeSection section) {
        check(section.prescription().stream().allMatch(round -> round != null)
                && section.actual().stream().allMatch(round -> round != null), "A round cannot be null.");
        var prescribed = section.prescription().stream().flatMap(round -> round.items().stream()).toList();
        var actual = section.actual().stream().flatMap(round -> round.items().stream()).toList();
        items(prescribed); items(actual); samePositions(prescribed, actual);
        check(section.prescription().size() == section.actual().size(), "Preserve the prescribed rounds.");
        for (int i = 0; i < section.actual().size(); i++) {
            samePositions(section.prescription().get(i).items(), section.actual().get(i).items());
        }
        check(section.scaling() != Scaling.RX || sameWork(prescribed, actual),
                "A changed prescription cannot be RX.");
        score(section.scoreState(), section.result());
        if (section.result() instanceof CompletedResult completed) {
            check(section.capSeconds() == null || completed.elapsedSeconds() <= section.capSeconds(),
                    "Completion cannot exceed the cap.");
        } else if (section.result() instanceof CappedResult capped) {
            check(section.capSeconds() != null, "A capped result requires a time cap.");
            check(capped.completedReps() < actual.stream().mapToLong(WorkItem::reps).sum(),
                    "A capped result cannot claim all prescribed work.");
        }
        return section;
    }

    private void score(ScoreState state, Object result) {
        check((state == ScoreState.SCORED) == (result != null), "Scored entries require a result; unscored entries have none.");
    }

    private void items(List<WorkItem> items) {
        Set<String> positions = new HashSet<>();
        for (var item : items) {
            check(item != null && positions.add(item.occurrenceId()), "Movement occurrence IDs must be unique.");
            load(item.load(), item.movement());
        }
    }

    private boolean sameWork(List<WorkItem> prescribed, List<WorkItem> actual) {
        for (int i = 0; i < prescribed.size(); i++) {
            var left = prescribed.get(i); var right = actual.get(i);
            if (left.movement() != right.movement() || !left.reps().equals(right.reps())) return false;
            var a = left.load(); var b = right.load();
            if (a.kind() != b.kind() || a.unit() != b.unit() || a.basis() != b.basis()) return false;
            if (a.amount() != null && (b.amount() == null || a.amount().compareTo(b.amount()) != 0)) return false;
            if (a.amount() == null && b.amount() != null) return false;
        }
        return true;
    }

    private void samePositions(List<WorkItem> prescribed, List<WorkItem> actual) {
        check(prescribed.stream().map(WorkItem::occurrenceId).toList()
                .equals(actual.stream().map(WorkItem::occurrenceId).toList()), "Preserve movement positions and order.");
    }

    private void load(Load load, Movement movement) {
        check(load != null, "Specify a load kind.");
        boolean external = load.kind() == Load.KindEnum.EXTERNAL;
        check(external ? load.amount() != null && load.unit() != null && load.basis() != null
                : load.amount() == null && load.unit() == null && load.basis() == null,
                "External loads need amount/unit/basis; bodyweight has no external load fields.");
        check(load.amount() == null || load.amount().stripTrailingZeros().scale() <= 3,
                "Loads support at most three decimal places.");
        check(external == (movement == Movement.BACK_SQUAT || movement == Movement.THRUSTER),
                "The selected movement uses an incompatible load kind in this slice.");
    }

    static Long totalReps(WorkoutSection section) {
        if (section instanceof AmrapSection amrap && amrap.result() != null) {
            return amrap.result().rounds() * amrap.actual().stream().mapToLong(WorkItem::reps).sum()
                    + amrap.result().additionalReps();
        }
        if (section instanceof ForTimeSection forTime) {
            if (forTime.result() instanceof CappedResult capped) return capped.completedReps().longValue();
            if (forTime.result() instanceof CompletedResult) return forTime.actual().stream()
                    .flatMap(round -> round.items().stream()).mapToLong(WorkItem::reps).sum();
        }
        return null;
    }

    private void check(boolean valid, String message) { if (!valid) throw invalid(message); }
    private ApiFailure invalid(String message) { return new ApiFailure(HttpStatus.BAD_REQUEST, "invalid_workout", message); }
}
