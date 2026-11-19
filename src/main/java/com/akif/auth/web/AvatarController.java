package com.akif.auth.web;

import com.akif.auth.api.AvatarStorageService;
import com.akif.auth.api.AvatarUploadResponse;
import com.akif.auth.internal.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.akif.shared.security.AuthenticatedUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@Tag(name = "Avatar Management", description = "User avatar management APIs")
@SecurityRequirement(name = "bearerAuth")
public class AvatarController {

    private final AvatarStorageService avatarStorageService;
    private final ProfileService profileService;

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload avatar", description = "Upload a new avatar image for the current user")
    public ResponseEntity<AvatarUploadResponse> uploadAvatar(
            @AuthenticationPrincipal AuthenticatedUser caller,
            @RequestParam("avatar") MultipartFile file) {

        String avatarKey = avatarStorageService.storeAvatar(caller.id(), file);

        profileService.updateAvatarUrl(caller.id(), avatarKey);

        String avatarUrl = avatarStorageService.generateAvatarUrl(avatarKey, 60);

        return ResponseEntity.ok(AvatarUploadResponse.success(avatarUrl));
    }

    @DeleteMapping("/avatar")
    @Operation(summary = "Delete avatar", description = "Delete the current user's avatar image")
    public ResponseEntity<AvatarUploadResponse> deleteAvatar(
            @AuthenticationPrincipal AuthenticatedUser caller) {

        profileService.removeAvatar(caller.id());

        return ResponseEntity.ok(AvatarUploadResponse.deleted());
    }
}
