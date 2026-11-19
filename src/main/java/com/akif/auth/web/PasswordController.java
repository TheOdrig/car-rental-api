package com.akif.auth.web;

import com.akif.auth.api.PasswordService;
import com.akif.auth.internal.dto.PasswordChangeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@Tag(name = "Password Management", description = "User password management APIs")
@SecurityRequirement(name = "bearerAuth")
public class PasswordController {

    private final PasswordService passwordService;

    @PostMapping("/password")
    @Operation(summary = "Change password", description = "Change the current user's password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PasswordChangeRequest request) {

        passwordService.changePassword(
                userDetails.getUsername(),
                request.currentPassword(),
                request.newPassword());

        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
}
