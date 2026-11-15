package com.syla.core.identity;

import jakarta.validation.constraints.*;

public record ProfileInput(
        @NotBlank @Pattern(regexp = "[a-z][a-z0-9_]{2,29}") String handle,
        @NotBlank @Size(max = 80) String displayName,
        @NotNull @Size(max = 500) String bio,
        @NotNull Boolean privateProfile) {}
