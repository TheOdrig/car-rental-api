package com.akif.auth.web;

import com.akif.auth.internal.dto.LoginRequest;
import com.akif.auth.internal.dto.RefreshTokenRequest;
import com.akif.auth.internal.dto.RegisterRequest;
import com.akif.auth.internal.dto.ForgotPasswordRequest;
import com.akif.auth.internal.dto.ResetPasswordRequest;
import com.akif.auth.api.AuthResponse;
import com.akif.auth.internal.service.AuthService;
import com.akif.auth.api.PasswordResetService;
import com.akif.shared.security.ratelimit.ClientIpResolver;
import com.akif.shared.security.ratelimit.RateLimitCategory;
import com.akif.shared.security.ratelimit.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final RateLimitService rateLimitService;
    private final ClientIpResolver clientIpResolver;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Create a new user account")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {

        AuthResponse response = authService.register(registerRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT tokens")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest,
                                              HttpServletRequest httpRequest) {

        String account = authService.loginThrottleKey(loginRequest.username());
        String attempt = RateLimitService.pair(account, clientIpResolver.resolve(httpRequest));
        rateLimitService.reserve(RateLimitCategory.LOGIN_USERNAME_IP, attempt);
        try {
            rateLimitService.reserve(RateLimitCategory.LOGIN_USERNAME, account);
        } catch (RuntimeException e) {
            rateLimitService.refund(RateLimitCategory.LOGIN_USERNAME_IP, attempt);
            throw e;
        }

        AuthResponse response = authService.login(loginRequest);
        rateLimitService.refund(RateLimitCategory.LOGIN_USERNAME_IP, attempt);
        rateLimitService.refund(RateLimitCategory.LOGIN_USERNAME, account);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Get new access token using refresh token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {

        AuthResponse response = authService.refreshToken(refreshTokenRequest);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out", description = "Revoke the refresh token and its whole token family")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        authService.logout(refreshTokenRequest.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset", description = "Send password reset email to the user")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                              HttpServletRequest httpRequest) {

        rateLimitService.enforce(RateLimitCategory.PASSWORD_RESET_EMAIL_IP,
                RateLimitService.pair(request.email(), clientIpResolver.resolve(httpRequest)));
        rateLimitService.enforce(RateLimitCategory.PASSWORD_RESET_USERNAME, request.email());

        passwordResetService.requestPasswordReset(request.email());

        return ResponseEntity.ok(Map.of("message", "If the email exists, a password reset link has been sent"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset password using the token received in email")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {

        passwordResetService.resetPassword(request.token(), request.newPassword());

        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully"));
    }
}
