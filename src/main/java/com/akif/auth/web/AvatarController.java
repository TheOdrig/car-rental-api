package com.akif.auth.web;

import com.akif.auth.api.AvatarStorageService;
import com.akif.auth.api.AvatarUploadResponse;
import com.akif.auth.api.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Avatar Management", description = "User avatar management APIs")
@SecurityRequirement(name = "bearerAuth")
public class AvatarController {

    private final AvatarStorageService avatarStorageService;
    private final ProfileService profileService;

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload avatar", description = "Upload a new avatar image for the current user")
    public ResponseEntity<AvatarUploadResponse> uploadAvatar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("avatar") MultipartFile file) {
        log.info("POST /api/users/me/avatar - Uploading avatar for user: {}", userDetails.getUsername());

        String avatarKey = avatarStorageService.storeAvatar(userDetails.getUsername(), file);

        profileService.updateAvatarUrl(userDetails.getUsername(), avatarKey);

        String avatarUrl = avatarStorageService.generateAvatarUrl(avatarKey, 60);

        log.info("Avatar uploaded successfully for user: {}", userDetails.getUsername());
        return ResponseEntity.ok(AvatarUploadResponse.success(avatarUrl));
    }

    @DeleteMapping("/avatar")
    @Operation(summary = "Delete avatar", description = "Delete the current user's avatar image")
    public ResponseEntity<AvatarUploadResponse> deleteAvatar(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("DELETE /api/users/me/avatar - Deleting avatar for user: {}", userDetails.getUsername());

        var profile = profileService.getProfile(userDetails.getUsername());

        if (profile.avatarUrl() != null) {
            avatarStorageService.deleteAvatar(profile.avatarUrl());
        }

        profileService.removeAvatar(userDetails.getUsername());

        log.info("Avatar deleted successfully for user: {}", userDetails.getUsername());
        return ResponseEntity.ok(AvatarUploadResponse.deleted());
    }
}
