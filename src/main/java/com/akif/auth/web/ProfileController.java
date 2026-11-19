package com.akif.auth.web;

import com.akif.auth.internal.service.ProfileService;
import com.akif.auth.api.UserDto;
import com.akif.auth.internal.dto.ProfileUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.akif.shared.security.AuthenticatedUser;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@Tag(name = "Profile Management", description = "User profile management APIs")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/profile")
    @Operation(summary = "Get user profile", description = "Get current user's profile information")
    public ResponseEntity<UserDto> getProfile(@AuthenticationPrincipal AuthenticatedUser caller) {

        UserDto profile = profileService.getProfile(caller.id());

        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    @Operation(summary = "Update user profile", description = "Update current user's profile information")
    public ResponseEntity<UserDto> updateProfile(
            @AuthenticationPrincipal AuthenticatedUser caller,
            @Valid @RequestBody ProfileUpdateRequest request) {

        UserDto updatedProfile = profileService.updateProfile(caller.id(), request);

        return ResponseEntity.ok(updatedProfile);
    }
}
