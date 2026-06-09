package com.roberto.coach.repository;

import com.roberto.coach.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

// 4. Workout Feedback
public interface WorkoutFeedbackRepository extends JpaRepository<WorkoutFeedback, Long> {
    // Fetches the feedback history of a user's last session to check for difficulty adjustments
    List<WorkoutFeedback> findByUserProfileIdAndWorkoutPlanId(Long userId, Long workoutPlanId);
}