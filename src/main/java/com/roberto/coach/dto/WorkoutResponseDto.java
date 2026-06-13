package com.roberto.coach.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public record WorkoutResponseDto(
        @JsonProperty("workout_id") Long workoutId,
        @JsonProperty("name") String name,
        @JsonProperty("split_type") String splitType,
        @JsonProperty("motivational_message") String motivationalMessage,
        @JsonProperty("difficulty_level") int difficultyLevel,
        @JsonProperty("generated_at") LocalDateTime generatedAt,
        @JsonProperty("exercises") List<ResponseExerciseItem> exercises
) {
    public record ResponseExerciseItem(
            @JsonProperty("exercise_id") String exerciseId,
            @JsonProperty("name") String name,
            @JsonProperty("muscle_group") String muscleGroup,
            @JsonProperty("equipment_needed") String equipmentNeeded,
            @JsonProperty("sets") int sets,
            @JsonProperty("reps") int reps,
            @JsonProperty("rest_seconds") int restSeconds,
            @JsonProperty("is_home_friendly") boolean isHomeFriendly,
            @JsonProperty("description") String description
    ) {}
}