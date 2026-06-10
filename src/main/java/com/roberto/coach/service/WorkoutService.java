package com.roberto.coach.service;

import com.roberto.coach.dto.AiWorkoutJsonDto;
import com.roberto.coach.dto.WorkoutResponseDto;
import com.roberto.coach.model.UserProfile;
import com.roberto.coach.model.WorkoutPlan;
import com.roberto.coach.model.WorkoutExercise;
import com.roberto.coach.model.ExerciseCatalog;
import com.roberto.coach.repository.UserProfileRepository;
import com.roberto.coach.repository.WorkoutPlanRepository;
import com.roberto.coach.repository.ExerciseCatalogRepository; // New dependency
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ExerciseCatalogRepository exerciseCatalogRepository; // Injected for catalog resolution

    /**
     * Orchestrates workout plan generation, handles dynamic catalog expansion,
     * and persists the relational entity tree.
     */
    @Transactional
    public WorkoutResponseDto generateWorkout(String userId) {
        log.info("Initiating database-persisted workout generation pipeline for user ID: {}", userId);

        // 1. Fetch managed UserProfile context
        UserProfile userProfileEntity = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("UserProfile not found for ID: " + userId));

        var userProfileDto = userService.findByUserId(userId);

        // 2. Fetch available IDs from the catalog (In production, replace with exerciseCatalogRepository.findAllIds())
        List<String> availableCatalogIds = List.of("ex-squat-01", "ex-pushup-02", "ex-plank-03", "ex-lunges-04");

        // 3. Generate content via AI Router
        AiWorkoutJsonDto aiWorkoutDto = aiRoutingService.generateWorkoutStructure(userProfileDto, availableCatalogIds);
        String motivationalMessage = aiRoutingService.generateMotivationalMessage(userProfileDto);

        // 4. Build the parent WorkoutPlan entity
        WorkoutPlan workoutPlan = WorkoutPlan.builder()
                .userProfile(userProfileEntity)
                .name(aiWorkoutDto.workoutName())
                .splitType("UPPER_BODY") // Map dynamically based on your application needs
                .motivationalMessage(motivationalMessage)
                .difficultyRatingRequest(userProfileEntity.getFitnessLevel())
                .build();

        // 5. Map and resolve ExerciseCatalog dependencies for each item
        List<WorkoutExercise> exerciseEntities = aiWorkoutDto.exercises().stream()
                .map(aiExercise -> {

                    // Resolve the ExerciseCatalog entity (fetch existing or dynamically create newly discovered ones)
                    ExerciseCatalog catalogEntity = exerciseCatalogRepository.findById(aiExercise.id())
                            .orElseGet(() -> {
                                // If not found in database, check if Llama provided structural details for it
                                return aiWorkoutDto.discoveredExercises().stream()
                                        .filter(d -> d.id().equals(aiExercise.id()))
                                        .findFirst()
                                        .map(discovered -> {
                                            log.info("AI discovered a new exercise [{}]. Dynamically persisting to catalog.", discovered.name());
                                            ExerciseCatalog newCatalogItem = ExerciseCatalog.builder()
                                                    .id(discovered.id())
                                                    .name(discovered.name())
                                                    .muscleGroup(discovered.muscleGroup())
                                                    .equipmentNeeded(discovered.equipmentNeeded())
                                                    .build();
                                            // Persist right away so it obtains database presence for the upcoming foreign key bind
                                            return exerciseCatalogRepository.save(newCatalogItem);
                                        })
                                        .orElseThrow(() -> new IllegalStateException(
                                                "Exercise ID " + aiExercise.id() + " neither exists in catalog nor in AI discovered list."
                                        ));
                            });

                    // Construct the structural Join Entity using its Lombok Builder
                    return WorkoutExercise.builder()
                            .workoutPlan(workoutPlan)
                            .exerciseCatalog(catalogEntity)
                            .sets(aiExercise.sets())
                            .reps(aiExercise.reps())
                            .restSeconds(aiExercise.restSeconds())
                            .build();
                }).toList();

        // 6. Bind child collection safely
        workoutPlan.getExercises().addAll(exerciseEntities);

        // 7. Save the relational tree to PostgreSQL
        WorkoutPlan savedPlan = workoutPlanRepository.save(workoutPlan);
        log.info("Successfully persisted WorkoutPlan with relational exercises. ID: {}", savedPlan.getId());

        // 8. Map to unified outbound Response DTO, pulling names directly from the managed catalog reference
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
}