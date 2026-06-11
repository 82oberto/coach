package com.roberto.coach.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record FeedbackRequestDto(
        @JsonProperty("workout_id") Long workoutId,
        @JsonProperty("exercises_feedback") List<ExerciseFeedbackItem> exercisesFeedback
) {
    public record ExerciseFeedbackItem(
            @JsonProperty("exercise_id") String exerciseId,
            @JsonProperty("difficulty_rating") String difficultyRating, // TOO_EASY, PERFECT, TOO_HARD
            @JsonProperty("pain_or_discomfort") boolean painOrDiscomfort,
            @JsonProperty("user_notes") String userNotes
    ) {}
}