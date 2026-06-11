package com.roberto.coach.repository;

import com.roberto.coach.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

// 1. User Profile
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserId(String userId);

    boolean existsByUserId(String userId);

    void deleteByUserId(String userId);
}