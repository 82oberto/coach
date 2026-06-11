package com.roberto.coach.controller;

import com.roberto.coach.dto.UserProfileDto;
import com.roberto.coach.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<UserProfileDto> getUserByUserId(@PathVariable String userId) {
        log.info("REST request to get UserProfile for user ID: {}", userId);

        UserProfileDto userProfile = userService.findByUserId(userId);
        return ResponseEntity.ok(userProfile);
    }

    @PostMapping("/profile")
    public ResponseEntity<UserProfileDto> saveOrUpdateProfile(@Valid @RequestBody UserProfileDto userProfileDto) {
        log.info("REST request to save/update UserProfile for user ID: {}", userProfileDto.userId());

        UserProfileDto savedProfile = userService.saveOrUpdate(userProfileDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProfile);
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteUserByUserId(@PathVariable String userId) {
        log.info("REST request to delete UserProfile for user ID: {}", userId);

        userService.deleteByUserId(userId);
        return ResponseEntity.noContent().build();
    }
}