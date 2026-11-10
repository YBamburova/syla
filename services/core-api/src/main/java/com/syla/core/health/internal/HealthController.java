package com.syla.core.health.internal;

import com.syla.core.health.HealthApi;
import com.syla.core.health.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
class HealthController implements HealthApi {
    @Override
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse(HealthResponse.StatusEnum.UP));
    }
}
