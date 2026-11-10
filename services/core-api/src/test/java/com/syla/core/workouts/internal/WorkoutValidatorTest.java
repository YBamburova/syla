package com.syla.core.workouts.internal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.syla.core.ApiFailure;
import com.syla.core.workouts.*;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class WorkoutValidatorTest {
    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final ValidatorFactory BEANS = Validation.buildDefaultValidatorFactory();
    private final WorkoutValidator validator = new WorkoutValidator(BEANS.getValidator(),
            Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneOffset.UTC));
    @AfterAll static void close() { BEANS.close(); }

    @Test void normalizesWholeAdditionalRounds() throws Exception {
        var input = input("amrap"); ((ObjectNode)input.at("/section/result")).put("additionalReps", 38);
        var result = (AmrapSection)normalize(input).section();
        assertThat(result.result().rounds()).isEqualTo(13);
        assertThat(result.result().additionalReps()).isEqualTo(8);
        assertThat(WorkoutValidator.totalReps(result)).isEqualTo(398);
    }
    @Test void preservesDecimalLoadsAndAnExplicitZeroAttempt() throws Exception {
        var input = input("strength"); ((ObjectNode)input.at("/section/actualSets/4")).put("reps", 0);
        var result = (StrengthSection)normalize(input).section();
        assertThat(result.actualSets().get(3).load().amount()).isEqualByComparingTo("62.5");
        assertThat(result.actualSets().get(4).reps()).isZero();
    }
    @Test void unreportedStrengthSetsAreNotZeroAttempts() throws Exception {
        var input = input("strength"); ((ObjectNode)input.path("section")).putArray("actualSets");
        assertThatThrownBy(() -> normalize(input)).isInstanceOf(ApiFailure.class);
        ((ObjectNode)input.path("section")).put("scoreState", "UNSCORED");
        assertThat(((StrengthSection)normalize(input).section()).actualSets()).isEmpty();
    }
    @Test void changedMovementCannotBeRxButCanBeScaled() throws Exception {
        var input = input("amrap"); ((ObjectNode)input.at("/section/actual/0")).put("movement", "ring-row");
        assertThatThrownBy(() -> normalize(input)).isInstanceOf(ApiFailure.class);
        ((ObjectNode)input.path("section")).put("scaling", "SCALED");
        assertThat(WorkoutValidator.totalReps(normalize(input).section())).isEqualTo(368);
    }
    @Test void unscoredConditioningHasNoResult() throws Exception {
        var input = input("amrap"); ((ObjectNode)input.path("section")).put("scoreState", "UNSCORED");
        assertThatThrownBy(() -> normalize(input)).isInstanceOf(ApiFailure.class);
        ((ObjectNode)input.path("section")).remove("result");
        assertThat(WorkoutValidator.totalReps(normalize(input).section())).isNull();
    }
    @Test void aCompletedTimeCannotExceedItsCap() throws Exception {
        var input = input("for-time"); ((ObjectNode)input.path("section")).put("capSeconds", 461);
        assertThatThrownBy(() -> normalize(input)).isInstanceOf(ApiFailure.class);
        ((ObjectNode)input.path("section")).put("capSeconds", 462);
        assertThat(normalize(input)).isNotNull();
    }
    @Test void rejectsFutureTrainingDatesAndUnknownZones() throws Exception {
        var input = input("strength"); input.put("trainingDate", "2026-11-11");
        assertThatThrownBy(() -> normalize(input)).isInstanceOf(ApiFailure.class);
        input.put("trainingDate", "2026-01-01").put("timeZone", "Not/AZone");
        assertThatThrownBy(() -> normalize(input)).isInstanceOf(ApiFailure.class);
    }
    private ObjectNode input(String name) throws Exception {
        return (ObjectNode)JSON.readTree(Files.readString(Path.of("src/test/resources/" + name + ".json")));
    }
    private WorkoutInput normalize(ObjectNode input) throws Exception {
        return validator.normalize(JSON.treeToValue(input, WorkoutInput.class));
    }
}
