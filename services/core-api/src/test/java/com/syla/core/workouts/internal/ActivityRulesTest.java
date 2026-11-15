package com.syla.core.workouts.internal;

import com.syla.core.platform.ApiProblem;
import com.syla.core.workouts.ActivityInput;
import com.syla.core.workouts.ActivityInput.*;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ActivityRulesTest {
    @Test void convertsImperialDistanceExactly() {
        assertThat(ActivityRules.meters(BigDecimal.ONE,DistanceUnit.MI)).isEqualByComparingTo("1609.344");
        assertThat(ActivityRules.meters(BigDecimal.valueOf(25),DistanceUnit.YD)).isEqualByComparingTo("22.86");
    }

    @Test void rejectsNonexistentDstTimeAndFutureTrainingDate() {
        var metrics=new Metrics(Venue.OUTDOOR,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null);
        var input=new ActivityInput(Sport.RUN,"Run",LocalDate.of(2026,3,29),LocalTime.of(3,30),"Europe/Kyiv",1800,metrics,null,null);
        assertThatThrownBy(() -> ActivityRules.validate(input,Clock.fixed(Instant.parse("2026-11-11T12:00:00Z"),ZoneOffset.UTC))).isInstanceOf(ApiProblem.class);
    }
}
