package com.roberto.coach.service;

import com.roberto.coach.dto.UserProfileDto;
import com.roberto.coach.model.UserProfile;
import com.roberto.coach.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserProfileRepository userRepository;

    /**
     * Retrieves a user profile from the database using their unique user ID.
     * * @param userId The unique identifier.
     * @return The mapped UserProfileDto.
     * @throws RuntimeException if the user profile does not exist in the system.
     */
    @Transactional(readOnly = true)
    public UserProfileDto findByUserId(String userId) {
        log.info("Attempting to find user profile for user ID: {}", userId);

        UserProfile userProfile = userRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("User profile not found for user ID: {}", userId);
                    return new RuntimeException("User profile not found for user ID: " + userId);
                });

        return mapToDto(userProfile);
    }

    /**
     * Saves a new user profile or updates an existing one if the user ID already exists.
     * This achieves an "upsert" mechanism seamlessly.
     * * @param dto The data transfer object containing updated profile parameters.
     * @return The updated and persisted UserProfileDto context.
     */
    @Transactional
    public UserProfileDto saveOrUpdate(UserProfileDto dto) {
        log.info("Processing save/update request for user ID: {}", dto.userId());

        // Find existing record or initialize a new blank Entity state
        UserProfile userProfile = userRepository.findByUserId(dto.userId())
                .orElseGet(() -> {
                    log.info("No existing profile found. Initializing a new record for user ID: {}", dto.userId());
                    UserProfile newProfile = new UserProfile();
                    newProfile.setUserId(dto.userId());
                    return newProfile;
                });

        List<String> equipments = new java.util.ArrayList<>(dto.equipment());
        equipments.add("BODYWEIGHT");

        // Map updated fields from DTO to Entity
        userProfile.setFitnessLevel(dto.fitnessLevel());
        userProfile.setAvailableTimeMinutes(dto.availableTimeMinutes());
        userProfile.setTrainingDaysPerWeek(dto.trainingDaysPerWeek());
        userProfile.setPhysicalLimitations(dto.physicalLimitations());
        userProfile.setEquipment(equipments);
        userProfile.setPreferredLocation(dto.preferredLocation());

        UserProfile savedEntity = userRepository.save(userProfile);
        log.info("Successfully persisted profile changes for user ID: {}", dto.userId());

        return mapToDto(savedEntity);
    }

    /**
     * Deletes a user profile from the system by their unique user ID.
     * * @param userId The unique identifier of the user to be removed.
     */
    @Transactional
    public void deleteByUserId(String userId) {
        log.info("Processing deletion request for user ID: {}", userId);

        if (!userRepository.existsByUserId(userId)) {
            log.warn("Aborting deletion. No profile exists for user ID: {}", userId);
            throw new RuntimeException("Cannot delete. User not found for user ID: " + userId);
        }

        userRepository.deleteByUserId(userId);
        log.info("Successfully removed user profile for user ID: {}", userId);
    }

    /**
     * Internal helper method to map a UserProfile database entity into a clean, immutable UserProfileDto Record.
     */
    private UserProfileDto mapToDto(UserProfile entity) {
        return new UserProfileDto(
                entity.getUserId(),
                entity.getFitnessLevel(),
                entity.getTrainingDaysPerWeek(),
                entity.getAvailableTimeMinutes(),
                entity.getSpeedModifier(),
                entity.getEquipment(),
                entity.getPhysicalLimitations(),
                entity.getPreferredLocation()
        );
    }
}