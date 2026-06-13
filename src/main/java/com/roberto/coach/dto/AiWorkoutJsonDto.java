package com.roberto.coach.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AiWorkoutJsonDto(
        @JsonProperty("workout_name") String workoutName,
        @JsonProperty("exercises") List<AiExerciseItem> exercises,
        @JsonProperty("discovered_exercises") List<DiscoveredExerciseItem> discoveredExercises
) {
    public record AiExerciseItem(
            @JsonProperty("id") String id,
            @JsonProperty("sets") int sets,
            @JsonProperty("reps") int reps,
            @JsonProperty("rest_seconds") int restSeconds
    ) {}

    public record DiscoveredExerciseItem(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("muscle_group") String muscleGroup,
            @JsonProperty("equipment_needed") String equipmentNeeded,
            @JsonProperty("is_home_friendly") boolean isHomeFriendly,
            @JsonProperty("description") String description
    ) {}
}