package com.roberto.coach.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workout_feedback")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserProfile userProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_id", nullable = false)
    private WorkoutPlan workoutPlan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private ExerciseCatalog exerciseCatalog;

    @Column(name = "difficulty_rating", nullable = false, length = 50)
    private String difficultyRating; // TOO_EASY, PERFECT, TOO_HARD

    @Column(name = "pain_or_discomfort", nullable = false)
    @Builder.Default
    private boolean painOrDiscomfort = false;

    @Column(name = "user_notes", columnDefinition = "text")
    private String userNotes;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;
}