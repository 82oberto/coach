package com.roberto.coach.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.roberto.coach.model.TrainingLocation;

import java.math.BigDecimal;
import java.util.List;

public record UserProfileDto(
        @JsonProperty("user_id") String userId,
        @JsonProperty("fitness_level") int fitnessLevel,
        @JsonProperty("training_days_per_week") int trainingDaysPerWeek,
        @JsonProperty("available_time_minutes") int availableTimeMinutes,
        @JsonProperty("speed_modifier") BigDecimal speedModifier,
        @JsonProperty("equipment") List<String> equipment,
        @JsonProperty("physical_limitations") String physicalLimitations,
        @JsonProperty("preferred_location") TrainingLocation preferredLocation
) {}