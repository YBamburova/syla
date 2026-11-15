package com.syla.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.syla.core.identity.IdentityLifecycle;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.mockito.Mockito.*;

@SpringBootTest @AutoConfigureMockMvc @Import(DatabaseTestConfiguration.class)
class WorkoutIT {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean IdentityLifecycle lifecycle;

    RequestPostProcessor athlete(String subject) {
        return oidcLogin().idToken(token -> token.subject(subject).claim("iss", "http://localhost:8081/realms/syla"))
                .userInfoToken(user -> user.subject(subject).claim("preferred_username", "Test athlete"));
    }

    String newAthlete() throws Exception {
        String id = UUID.randomUUID().toString();
        mvc.perform(post("/api/v1/session/consent").with(athlete(id)).with(csrf())).andExpect(status().isOk());
        return id;
    }

    ObjectNode activity(String sport) throws Exception {
        return (ObjectNode) mapper.readTree("""
                {"sport":"%s","title":"Morning training","trainingDate":"2026-01-15","startTime":"07:30:00","timeZone":"Europe/Kyiv",
                 "durationSeconds":1800,"metrics":{"venue":"%s","distance":5,"distanceUnit":"KM"},"effort":5,"description":"Synthetic test"}
                """.formatted(sport, sport.equals("SWIM") ? "OPEN_WATER" : "OUTDOOR"));
    }

    JsonNode create(String actor, ObjectNode input, String key) throws Exception {
        String body = mvc.perform(post("/api/v1/workouts").with(athlete(actor)).with(csrf()).header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(input.toString()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body);
    }

    @Test void storesAllFourSportsAndPreservesUnitsAndPoolDetails() throws Exception {
        String actor = newAthlete();
        var run = activity("RUN"); ((ObjectNode) run.get("metrics")).put("distance", 3.1).put("distanceUnit", "MI");
        JsonNode logged = create(actor, run, UUID.randomUUID().toString());
        assertThat(logged.path("activity").path("metrics").path("distanceUnit").asText()).isEqualTo("MI");
        assertThat(logged.path("distanceMeters").decimalValue()).isEqualByComparingTo("4988.9664");
        create(actor, activity("RIDE"), UUID.randomUUID().toString());
        var swim = activity("SWIM");
        ((ObjectNode) swim.get("metrics")).remove(List.of("distance", "distanceUnit"));
        ((ObjectNode) swim.get("metrics")).put("venue", "POOL").put("poolLength",25).put("poolLengthUnit","M").put("lengths",40);
        var result = create(actor, swim, UUID.randomUUID().toString());
        assertThat(result.path("distanceMeters").decimalValue()).isEqualByComparingTo("1000");
        assertThat(result.path("swimPaceSecondsPer100m").decimalValue()).isEqualByComparingTo("180");
        var workout = activity("WORKOUT"); ((ObjectNode) workout.get("metrics")).remove(List.of("distance", "distanceUnit"));
        create(actor, workout, UUID.randomUUID().toString());
        mvc.perform(get("/api/v1/workouts").with(athlete(actor))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(4));
        mvc.perform(get("/api/v1/workouts?sport=RUN").with(athlete(actor))).andExpect(jsonPath("$.total").value(1));
    }

    @Test void isolatesOwnershipAndRequiresConsentAndCsrf() throws Exception {
        String actor = newAthlete();
        var input = activity("RUN");
        var logged = create(actor, input, UUID.randomUUID().toString());
        String other = newAthlete();
        String path = "/api/v1/workouts/"+logged.path("id").asText();
        mvc.perform(get(path).with(athlete(other))).andExpect(status().isNotFound());
        mvc.perform(put(path).with(athlete(other)).with(csrf()).header("If-Match","\"0\"").contentType(MediaType.APPLICATION_JSON).content(input.toString())).andExpect(status().isNotFound());
        mvc.perform(delete(path).with(athlete(other)).with(csrf()).header("If-Match","\"0\"")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/workouts").with(athlete(actor)).contentType(MediaType.APPLICATION_JSON).content(input.toString())).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/workouts").with(athlete("unconsented")).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(input.toString())).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/workouts")).andExpect(status().isUnauthorized());
    }

    @Test void retriesWithoutDuplicatesAndRejectsConflictsAndResurrection() throws Exception {
        String actor = newAthlete(); var input = activity("RUN"); String key=UUID.randomUUID().toString();
        var logged = create(actor,input,key); assertThat(create(actor,input,key).path("id")).isEqualTo(logged.path("id"));
        var modified=input.deepCopy(); modified.put("title","Edited training");
        mvc.perform(post("/api/v1/workouts").with(athlete(actor)).with(csrf()).header("Idempotency-Key",key).contentType(MediaType.APPLICATION_JSON).content(modified.toString())).andExpect(status().isConflict());
        String path="/api/v1/workouts/"+logged.path("id").asText();
        mvc.perform(put(path).with(athlete(actor)).with(csrf()).header("If-Match","\"0\"").contentType(MediaType.APPLICATION_JSON).content(modified.toString())).andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(1));
        mvc.perform(delete(path).with(athlete(actor)).with(csrf()).header("If-Match","\"0\"")).andExpect(status().isConflict());
        mvc.perform(delete(path).with(athlete(actor)).with(csrf()).header("If-Match","\"1\"")).andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/workouts").with(athlete(actor)).with(csrf()).header("Idempotency-Key",key).contentType(MediaType.APPLICATION_JSON).content(input.toString())).andExpect(status().isGone());
    }

    @Test void validatesConditionalFieldsWithoutEchoingValues() throws Exception {
        String actor = newAthlete(); var input = activity("RUN");
        ((ObjectNode) input.get("metrics")).put("movingSeconds",2000).put("averagePower",150);
        mvc.perform(post("/api/v1/workouts").with(athlete(actor)).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(input.toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors['metrics.movingSeconds']").exists()).andExpect(jsonPath("$.errors['metrics.averagePower']").exists());
    }

    @Test void exportsOwnedDataAndBlocksAllMutationsDuringRetryableDeletion() throws Exception {
        String actor=newAthlete(); create(actor,activity("RUN"),UUID.randomUUID().toString());
        mvc.perform(get("/api/v1/account/export").with(athlete(actor))).andExpect(status().isOk()).andExpect(jsonPath("$.activities.length()").value(1)).andExpect(jsonPath("$.account.consentedAt").exists());
        doThrow(new com.syla.core.platform.ApiProblem(503,"Retry cleanup")).doNothing().when(lifecycle).deleteProviderAccount(any());
        mvc.perform(delete("/api/v1/account").with(athlete(actor)).with(csrf())).andExpect(status().isServiceUnavailable());
        mvc.perform(get("/api/v1/workouts").with(athlete(actor))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/session/consent").with(athlete(actor)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/account").with(athlete(actor)).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/session").with(athlete(actor))).andExpect(jsonPath("$.state").value("DELETED"));
        mvc.perform(get("/api/v1/workouts").with(athlete(actor))).andExpect(status().isForbidden());
    }
    @Test void concurrentRetriesCreateExactlyOneRecord() throws Exception {
        String actor = newAthlete();
        ObjectNode input = activity("RUN");
        String key = UUID.randomUUID().toString();
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> create(actor, input, key));
            var second = pool.submit(() -> create(actor, input, key));
            assertThat(first.get().path("id")).isEqualTo(second.get().path("id"));
        }
        mvc.perform(get("/api/v1/workouts").with(athlete(actor)))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test void expiresSessionsAtTheAbsoluteLifetime() throws Exception {
        mvc.perform(get("/api/v1/workouts").with(athlete(newAthlete()))
                        .sessionAttr("syla.loginAt", java.time.Instant.now().getEpochSecond() - 43201))
                .andExpect(status().isUnauthorized());
    }

}
