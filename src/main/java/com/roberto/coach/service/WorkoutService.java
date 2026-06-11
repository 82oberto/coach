package com.roberto.coach.service;

import com.roberto.coach.dto.AiWorkoutJsonDto;
import com.roberto.coach.dto.UserProfileDto;
import com.roberto.coach.dto.WorkoutResponseDto;
import com.roberto.coach.model.UserProfile;
import com.roberto.coach.model.WorkoutPlan;
import com.roberto.coach.model.WorkoutExercise;
import com.roberto.coach.model.ExerciseCatalog;
import com.roberto.coach.repository.UserProfileRepository;
import com.roberto.coach.repository.WorkoutPlanRepository;
import com.roberto.coach.repository.ExerciseCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkoutService {

    private static final Logger log = LoggerFactory.getLogger(WorkoutService.class);

    private final AiRoutingService aiRoutingService;
    private final UserService userService;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final UserProfileRepository userRepository;
    private final ExerciseCatalogRepository exerciseCatalogRepository;

    @Transactional
    public WorkoutResponseDto generateWorkout(String userId) {
        log.info("Initiating database-persisted workout generation pipeline for user ID: {}", userId);

        // 1. Fetch managed UserProfile context
        UserProfile userProfileEntity = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("UserProfile not found for ID: " + userId));

        UserProfileDto userProfileDto = userService.findByUserId(userId);

        List<String> availableCatalogIds = exerciseCatalogRepository.findAll().stream()
                .filter(exercise -> userProfileDto.equipment().contains(exercise.getEquipmentNeeded()))
                .map(ExerciseCatalog::getId)
                .toList();

        int targetExercises = calculateTargetExercises(userProfileEntity);
        log.debug("Target exercises: {}", targetExercises);

        // 3. Generate content via AI Router
        AiWorkoutJsonDto aiWorkoutDto = aiRoutingService.generateWorkoutStructure(userProfileDto, targetExercises, availableCatalogIds);
        String motivationalMessage = aiRoutingService.generateMotivationalMessage(userProfileDto);

        if (aiWorkoutDto.discoveredExercises() != null && !aiWorkoutDto.discoveredExercises().isEmpty()) {
            for (var discovered : aiWorkoutDto.discoveredExercises()) {
                if (!exerciseCatalogRepository.existsById(discovered.id())) {
                    log.info("AI discovered a new exercise [{}]. Dynamically persisting to catalog.", discovered.name());
                    ExerciseCatalog newCatalogItem = ExerciseCatalog.builder()
                            .id(discovered.id())
                            .name(discovered.name())
                            .muscleGroup(discovered.muscleGroup())
                            .equipmentNeeded(discovered.equipmentNeeded())
                            .build();

                    exerciseCatalogRepository.save(newCatalogItem);
                }
            }
        }

        // 5. Build the parent WorkoutPlan entity
        WorkoutPlan workoutPlan = WorkoutPlan.builder()
                .userProfile(userProfileEntity)
                .name(aiWorkoutDto.workoutName())
                .splitType("UPPER_BODY")
                .motivationalMessage(motivationalMessage)
                .difficultyRatingRequest(userProfileEntity.getFitnessLevel())
                .build();

        // 6. Map and resolve ExerciseCatalog dependencies safely
        List<WorkoutExercise> exerciseEntities = aiWorkoutDto.exercises().stream()
                .map(aiExercise -> {
                    ExerciseCatalog catalogEntity = exerciseCatalogRepository.findById(aiExercise.id())
                            .orElseThrow(() -> new IllegalStateException(
                                    "Critical error: ID " + aiExercise.id() + " is not found in the database and the AI did not provide metadata in 'discovered_exercises'."
                            ));

                    return WorkoutExercise.builder()
                            .workoutPlan(workoutPlan)
                            .exerciseCatalog(catalogEntity)
                            .sets(aiExercise.sets())
                            .reps(aiExercise.reps())
                            .restSeconds(aiExercise.restSeconds())
                            .build();
                }).toList();

        // 7. Bind child collection safely
        workoutPlan.getExercises().addAll(exerciseEntities);

        // 8. Save the relational tree to PostgreSQL
        WorkoutPlan savedPlan = workoutPlanRepository.save(workoutPlan);
        log.info("Successfully persisted WorkoutPlan with relational exercises. ID: {}", savedPlan.getId());

        // 9. Map to outbound Response DTO
        List<WorkoutResponseDto.ResponseExerciseItem> responseExercises = savedPlan.getExercises().stream()
                .map(e -> new WorkoutResponseDto.ResponseExerciseItem(
                        e.getExerciseCatalog().getId(),
                        e.getExerciseCatalog().getName(),
                        e.getExerciseCatalog().getMuscleGroup(),
                        e.getExerciseCatalog().getEquipmentNeeded(),
                        e.getSets(),
                        e.getReps(),
                        e.getRestSeconds()
                )).toList();

        return new WorkoutResponseDto(
                savedPlan.getId(),
                savedPlan.getName(),
                savedPlan.getSplitType(),
                savedPlan.getMotivationalMessage(),
                savedPlan.getDifficultyRatingRequest(),
                savedPlan.getGeneratedAt() != null ? savedPlan.getGeneratedAt() : LocalDateTime.now(),
                responseExercises
        );
    }

    public int calculateTargetExercises(UserProfile userProfile) {
        BigDecimal modifier = (userProfile.getSpeedModifier() != null)
                ? userProfile.getSpeedModifier()
                : BigDecimal.valueOf(1.00);
        BigDecimal availableTime = BigDecimal.valueOf(userProfile.getAvailableTimeMinutes());
        BigDecimal averageMinutesPerExercise = BigDecimal.valueOf(8.0);
        BigDecimal baseExercises = availableTime.divide(averageMinutesPerExercise, 2, RoundingMode.HALF_UP);
        BigDecimal targetDecimal = baseExercises.multiply(modifier);
        int targetExercises = targetDecimal.setScale(0, RoundingMode.HALF_UP).intValue();
        return Math.clamp(targetExercises, 2, 10);
    }
}