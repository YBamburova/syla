package com.syla.core.workouts;
import jakarta.validation.constraints.NotNull;
public record SharingInput(@NotNull Visibility visibility) {
    public enum Visibility { PRIVATE, FOLLOWERS }
}
