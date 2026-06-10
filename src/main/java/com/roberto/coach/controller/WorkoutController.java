package com.roberto.coach.controller;

import com.roberto.coach.dto.WorkoutResponseDto;
import com.roberto.coach.service.WorkoutService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workouts")
@RequiredArgsConstructor
public class WorkoutController {

    private static final Logger log = LoggerFactory.getLogger(WorkoutController.class);
    private final WorkoutService workoutService;

    /**
     * Exposes the REST endpoint to trigger AI workout generation.
     *
     * @param userId The unique identifier of the target user.
     * @return A ResponseEntity containing the fully populated WorkoutResponseDto payload.
     */
    @GetMapping("/generate/{userId}")
    public ResponseEntity<WorkoutResponseDto> generateWorkout(@PathVariable String userId) {
        log.info("REST request received to generate AI workout orchestration for user ID: {}", userId);

        WorkoutResponseDto response = workoutService.generateWorkout(userId);

        return ResponseEntity.ok(response);
    }
}