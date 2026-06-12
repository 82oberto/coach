package com.roberto.coach.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "user_profile")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true, length = 100)
    private String userId;

    @Column(name = "fitness_level", nullable = false)
    private int fitnessLevel;

    @Column(name = "training_days_per_week", nullable = false)
    private int trainingDaysPerWeek;

    @Column(name = "available_time_minutes", nullable = false)
    private int availableTimeMinutes;

    @Column(name = "speed_modifier", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal speedModifier = BigDecimal.valueOf(1.00);

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "equipment", columnDefinition = "jsonb", nullable = false)
    private List<String> equipment;

    @Column(name = "physical_limitations", columnDefinition = "text")
    private String physicalLimitations;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_location", nullable = false)
    @Builder.Default
    private TrainingLocation preferredLocation = TrainingLocation.GYM;
}