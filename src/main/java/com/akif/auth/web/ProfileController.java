package com.akif.auth.web;

import com.akif.auth.api.ProfileService;
import com.akif.auth.api.UserDto;
import com.akif.auth.internal.dto.ProfileUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Profile Management", description = "User profile management APIs")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/profile")
    @Operation(summary = "Get user profile", description = "Get current user's profile information")
    public ResponseEntity<UserDto> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("GET /api/users/me/profile - Getting profile for user: {}", userDetails.getUsername());

        UserDto profile = profileService.getProfile(userDetails.getUsername());

        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    @Operation(summary = "Update user profile", description = "Update current user's profile information")
    public ResponseEntity<UserDto> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProfileUpdateRequest request) {
        log.info("PUT /api/users/me/profile - Updating profile for user: {}", userDetails.getUsername());

        UserDto updatedProfile = profileService.updateProfile(userDetails.getUsername(), request);

        log.info("Profile updated successfully for user: {}", userDetails.getUsername());
        return ResponseEntity.ok(updatedProfile);
    }
}
