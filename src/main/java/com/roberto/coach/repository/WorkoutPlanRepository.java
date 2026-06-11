package com.roberto.coach.repository;

import com.roberto.coach.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

// 3. Workout Plan
public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, Long> {
    // Finds the absolute latest session completed by the user to determine split rotation
    Optional<WorkoutPlan> findFirstByUserProfileIdOrderByGeneratedAtDesc(Long userId);
}