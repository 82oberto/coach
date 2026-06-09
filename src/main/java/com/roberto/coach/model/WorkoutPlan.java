package com.roberto.coach.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workout_plan")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "exercises")
public class WorkoutPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserProfile userProfile;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "split_type", nullable = false, length = 50)
    private String splitType; // UPPER_BODY, LOWER_BODY, CORE

    @Column(name = "motivational_message", columnDefinition = "text")
    private String motivationalMessage;

    @Column(name = "difficulty_rating_request", nullable = false)
    private int difficultyRatingRequest;

    @Column(name = "generated_at", updatable = false, insertable = false)
    private LocalDateTime generatedAt;

    @OneToMany(mappedBy = "workoutPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkoutExercise> exercises = new ArrayList<>();
}