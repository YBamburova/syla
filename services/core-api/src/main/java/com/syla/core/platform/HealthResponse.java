package com.syla.core.platform;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Process availability only; does not indicate database or identity-provider readiness.")
public record HealthResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "UP") Status status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "syla-core-api") String service) {
    public enum Status { UP }
}
