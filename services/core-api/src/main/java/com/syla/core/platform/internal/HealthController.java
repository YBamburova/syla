package com.syla.core.platform.internal;

import com.syla.core.platform.HealthResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Platform", description = "Public service availability")
class HealthController {
    @GetMapping(value = "/api/v1/health", produces = "application/json")
    @Operation(operationId = "getHealth", summary = "Check whether the API process is responding")
    ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new HealthResponse(HealthResponse.Status.UP, "syla-core-api"));
    }
}
