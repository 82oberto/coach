package com.roberto.coach.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exercise_catalog")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseCatalog {

    @Id
    @Column(length = 100)
    private String id; // e.g., "diamond-push-up"

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "muscle_group", nullable = false, length = 50)
    private String muscleGroup; // UPPER_BODY, LOWER_BODY, CORE, FULL_BODY

    @Column(name = "equipment_needed", nullable = false, length = 50)
    private String equipmentNeeded; // BODYWEIGHT, DUMBBELLS, etc.

    @Column(name = "is_home_friendly", nullable = false)
    @Builder.Default
    private boolean homeFriendly = true;
}