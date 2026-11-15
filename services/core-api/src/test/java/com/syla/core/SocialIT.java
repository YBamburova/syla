package com.syla.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
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

@SpringBootTest @AutoConfigureMockMvc @Import(DatabaseTestConfiguration.class)
class SocialIT {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean IdentityLifecycle lifecycle;
    record Actor(String subject, String id, String handle) {}
    RequestPostProcessor auth(Actor actor) {
        return oidcLogin().idToken(t -> t.subject(actor.subject()).claim("iss","http://localhost:8081/realms/syla"))
                .userInfoToken(t -> t.subject(actor.subject()).claim("preferred_username",actor.handle()).claim("given_name","Given"+actor.subject().substring(0,8)).claim("family_name","Family"+actor.subject().substring(0,8)));
    }
    Actor actor(boolean setup, boolean privateProfile) throws Exception {
        String subject=UUID.randomUUID().toString();
        Actor initial=new Actor(subject,"","athlete_"+subject.replace("-", "").substring(0,12));
        mvc.perform(post("/api/v1/session/consent").with(auth(initial)).with(csrf())).andExpect(status().isOk()).andReturn().getResponse();
        var session=mvc.perform(get("/api/v1/session").with(auth(initial))).andReturn().getResponse();
        Actor result=new Actor(subject,mapper.readTree(session.getContentAsString()).path("athleteId").asText(),initial.handle());
        if(setup) profile(result,privateProfile).andExpect(status().isOk());
        return result;
    }
    org.springframework.test.web.servlet.ResultActions profile(Actor actor, boolean privateProfile) throws Exception {
        return mvc.perform(put("/api/v1/profile").with(auth(actor)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("handle",actor.handle(),"displayName","Synthetic athlete","bio","Training together","privateProfile",privateProfile))));
    }
    void action(Actor actor, Actor target, String action) throws Exception {
        mvc.perform(post("/api/v1/social/"+target.id()+"/"+action).with(auth(actor)).with(csrf())).andExpect(status().isNoContent());
    }
    String workout(Actor owner) throws Exception {
        var result=mvc.perform(post("/api/v1/workouts").with(auth(owner)).with(csrf()).header("Idempotency-Key",UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"sport":"RUN","title":"Synthetic run","trainingDate":"2026-01-15","startTime":"07:30:00","timeZone":"Europe/Kyiv",
                "durationSeconds":1800,"metrics":{"venue":"OUTDOOR","distance":5,"distanceUnit":"KM","averageHeartRate":140},"effort":5,"description":"Private health notes"}
                """)).andExpect(status().isCreated()).andReturn().getResponse();
        return mapper.readTree(result.getContentAsString()).path("id").asText();
    }
    void share(Actor owner,String id,String visibility,int revision) throws Exception {
        mvc.perform(patch("/api/v1/workouts/"+id+"/visibility").with(auth(owner)).with(csrf()).header("If-Match","\""+revision+"\"")
                .contentType(MediaType.APPLICATION_JSON).content("{\"visibility\":\""+visibility+"\"}")).andExpect(status().isOk());
    }
    @Test void registrationProfilesAreReadyWithoutSetupAndNamesAreSearchable() throws Exception {
        var hidden=actor(false,true); var viewer=actor(true,false);
        mvc.perform(post("/api/v1/social/"+viewer.id()+"/follow").with(auth(viewer)).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/social/"+viewer.id()+"/follow").with(auth(hidden))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/social/"+viewer.id()+"/follow").with(auth(hidden)).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/athletes/by-handle/"+hidden.handle()).with(auth(viewer))).andExpect(status().isOk()).andExpect(jsonPath("$.profile.privateProfile").value(true));
        mvc.perform(get("/api/v1/profile").with(auth(hidden))).andExpect(jsonPath("$.profile.displayName").value("Given"+hidden.subject().substring(0,8)+" Family"+hidden.subject().substring(0,8)));
        for (String term : List.of(hidden.handle(), "Given"+hidden.subject().substring(0,8), "Family"+hidden.subject().substring(0,8), "Given"+hidden.subject().substring(0,8)+" Family"+hidden.subject().substring(0,8))) {
            mvc.perform(get("/api/v1/athletes").param("query",term.toUpperCase(Locale.ROOT)).with(auth(viewer))).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(hidden.id()));
        }
        mvc.perform(put("/api/v1/profile").with(auth(hidden)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"handle\":\"Bad Handle\",\"displayName\":\"\",\"bio\":\"\",\"privateProfile\":true}")).andExpect(status().isBadRequest());
        var collision=new Actor(hidden.subject(),hidden.id(),viewer.handle());
        profile(collision,true).andExpect(status().isConflict());
        profile(hidden,true).andExpect(status().isOk());
        // Registration names remain searchable after customizing the display name.
        mvc.perform(get("/api/v1/athletes").param("query","Family"+hidden.subject().substring(0,8)).with(auth(viewer))).andExpect(jsonPath("$[0].id").value(hidden.id()));
        var renamed=new Actor(hidden.subject(),hidden.id(),hidden.handle()+"x");
        profile(renamed,true).andExpect(status().isOk());
        mvc.perform(get("/api/v1/session").with(auth(renamed))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/profile").with(auth(renamed))).andExpect(jsonPath("$.profile.displayName").value("Synthetic athlete"));
        mvc.perform(get("/api/v1/athletes?query="+renamed.handle()).with(auth(viewer))).andExpect(jsonPath("$[0].handle").value(renamed.handle()));
        mvc.perform(get("/api/v1/athletes?query=a").with(auth(viewer))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/athletes?query=athlete")).andExpect(status().isUnauthorized());
    }
    @Test void followRequestsWorkWithoutEitherAthleteEditingAProfile() throws Exception {
        var owner=actor(false,true); var follower=actor(false,true);
        action(follower,owner,"follow");
        mvc.perform(get("/api/v1/social/requests").with(auth(owner))).andExpect(jsonPath("$[0].id").value(follower.id()));
        mvc.perform(get("/api/v1/social/pending").with(auth(follower))).andExpect(jsonPath("$[0].id").value(owner.id()));
        action(owner,follower,"accept");
        mvc.perform(get("/api/v1/social/followers").with(auth(owner))).andExpect(jsonPath("$[0].id").value(follower.id()));
        mvc.perform(get("/api/v1/account/export").with(auth(owner))).andExpect(jsonPath("$.account.firstName").exists()).andExpect(jsonPath("$.account.username").value(owner.handle()));
    }
    @Test void privateRequestsAndEveryRevocationProtectDirectActivityReads() throws Exception {
        var owner=actor(true,true);var follower=actor(true,false);var stranger=actor(true,false);
        String id=workout(owner);
        mvc.perform(get("/api/v1/workouts/"+id).with(auth(owner))).andExpect(jsonPath("$.visibility").value("PRIVATE"));
        mvc.perform(get("/api/v1/shared-activities/"+id).with(auth(follower))).andExpect(status().isNotFound());
        share(owner,id,"FOLLOWERS",0);
        action(follower,owner,"follow"); action(follower,owner,"follow");
        mvc.perform(get("/api/v1/athletes/by-handle/"+owner.handle()).with(auth(follower))).andExpect(jsonPath("$.relationship").value("PENDING"));
        mvc.perform(get("/api/v1/shared-activities/"+id).with(auth(follower))).andExpect(status().isNotFound());
        profile(owner,false).andExpect(status().isOk());
        action(follower,owner,"follow");
        mvc.perform(get("/api/v1/athletes/by-handle/"+owner.handle()).with(auth(follower))).andExpect(jsonPath("$.relationship").value("PENDING"));
        profile(owner,true).andExpect(status().isOk());
        action(stranger,follower,"accept");
        mvc.perform(get("/api/v1/shared-activities/"+id).with(auth(follower))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/social/requests").with(auth(owner))).andExpect(jsonPath("$[0].id").value(follower.id()));
        action(owner,follower,"accept");
        var summary=mvc.perform(get("/api/v1/shared-activities/"+id).with(auth(follower))).andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceMeters").value(5000)).andReturn().getResponse().getContentAsString();
        assertThat(summary).doesNotContain("description","heartRate","metrics","startTime","timeZone","effort","Private health notes");
        mvc.perform(get("/api/v1/workouts/"+id).with(auth(follower))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/athletes/"+owner.id()+"/activities").with(auth(follower))).andExpect(jsonPath("$.total").value(1));
        action(owner,follower,"remove");
        mvc.perform(get("/api/v1/shared-activities/"+id).with(auth(follower))).andExpect(status().isNotFound());
        action(follower,owner,"follow");action(owner,follower,"remove"); // reject request
        mvc.perform(get("/api/v1/social/requests").with(auth(owner))).andExpect(jsonPath("$.length()").value(0));
        action(follower,owner,"follow");action(follower,owner,"unfollow"); // cancel request
        profile(owner,false).andExpect(status().isOk());
        action(follower,owner,"follow");
        share(owner,id,"PRIVATE",1);
        mvc.perform(get("/api/v1/shared-activities/"+id).with(auth(follower))).andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/workouts/"+id+"/visibility").with(auth(owner)).with(csrf()).header("If-Match","\"1\"")
                .contentType(MediaType.APPLICATION_JSON).content("{\"visibility\":\"FOLLOWERS\"}")).andExpect(status().isConflict());
        mvc.perform(patch("/api/v1/workouts/"+id+"/visibility").with(auth(follower)).with(csrf()).header("If-Match","\"2\"")
                .contentType(MediaType.APPLICATION_JSON).content("{\"visibility\":\"FOLLOWERS\"}")).andExpect(status().isNotFound());
    }
    @Test void blockingRemovesBothDirectionsAndUnblockingDoesNotRestoreThem() throws Exception {
        var a=actor(true,false);var b=actor(true,false);
        String id=workout(a);share(a,id,"FOLLOWERS",0);
        action(a,b,"follow");action(b,a,"follow");action(a,b,"block");
        for(var pair:List.of(List.of(a,b),List.of(b,a))) {
            mvc.perform(get("/api/v1/athletes/by-handle/"+pair.get(1).handle()).with(auth(pair.get(0)))).andExpect(status().isNotFound());
            mvc.perform(get("/api/v1/athletes?query="+pair.get(1).handle()).with(auth(pair.get(0)))).andExpect(jsonPath("$.length()").value(0));
            mvc.perform(get("/api/v1/social/following").with(auth(pair.get(0)))).andExpect(jsonPath("$.length()").value(0));
        }
        mvc.perform(get("/api/v1/shared-activities/"+id).with(auth(b))).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/social/"+a.id()+"/follow").with(auth(b)).with(csrf())).andExpect(status().isNotFound());
        action(a,b,"unblock");
        mvc.perform(get("/api/v1/athletes/by-handle/"+a.handle()).with(auth(b))).andExpect(jsonPath("$.relationship").value("NONE"));
        mvc.perform(get("/api/v1/shared-activities/"+id).with(auth(b))).andExpect(status().isNotFound());
    }
    @Test void exportAndDeletionIncludeSocialDataAndPreventResurrection() throws Exception {
        var owner=actor(true,false);var follower=actor(true,false);action(follower,owner,"follow");
        mvc.perform(get("/api/v1/account/export").with(auth(owner))).andExpect(jsonPath("$.profile.handle").value(owner.handle()))
                .andExpect(jsonPath("$.relationships[0].athleteId").value(follower.id()));
        mvc.perform(delete("/api/v1/account").with(auth(owner)).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/athletes/by-handle/"+owner.handle()).with(auth(follower))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/social/following").with(auth(follower))).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post("/api/v1/social/"+owner.id()+"/follow").with(auth(follower)).with(csrf())).andExpect(status().isNotFound());
        profile(owner,false).andExpect(status().isForbidden());
    }
    @Test void reciprocalRequestsDoNotDeadlock() throws Exception {
        var a=actor(true,false);var b=actor(true,false);
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first=pool.submit(() -> { action(a,b,"follow");return true; });
            var second=pool.submit(() -> { action(b,a,"follow");return true; });
            assertThat(first.get(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(second.get(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        }
    }
}
