package com.syla.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.test.web.servlet.MockMvc;

@org.springframework.context.annotation.Import(DatabaseTestConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PlatformIT {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void reportsProcessAvailabilityWithoutExposingConfiguration() throws Exception {
        mvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().json("{\"status\":\"UP\",\"service\":\"syla-core-api\"}", true));
    }

    @Test
    void servesSwaggerUiAndItsConfiguration() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.url").value("/v3/api-docs"));
    }

    @Test
    void exportsOpenApiAndDetectsContractDrift() throws Exception {
        String body = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/health'].get.operationId").value("getHealth"))
                .andReturn().getResponse().getContentAsString();
        var contract = mapper.readTree(body);
        assertThat(contract.path("paths").has("/api/v1/workouts")).isTrue();
        Files.createDirectories(Path.of("target"));
        mapper.writerWithDefaultPrettyPrinter().writeValue(Path.of("target/openapi.json").toFile(), contract);
        assertThat(contract).as("Copy target/openapi.json to openapi.json after reviewing API changes")
                .isEqualTo(mapper.readTree(Path.of("openapi.json").toFile()));
    }

    @Test
    void rejectsUnknownEndpointsAndMutations() throws Exception {
        mvc.perform(get("/api/v1/workouts"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(post("/api/v1/health")).andExpect(status().isForbidden());
    }

    @Test
    void preservesModuleBoundaries() {
        ApplicationModules.of(CoreApplication.class).verify();
    }
}
