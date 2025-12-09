package com.akif.auth.web;

import com.akif.auth.LoginRequest;
import com.akif.auth.RefreshTokenRequest;
import com.akif.auth.RegisterRequest;
import com.akif.auth.AuthResponse;
import com.akif.auth.IAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final IAuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Create a new user account")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("POST /api/auth/register - Registering user: {}", registerRequest.username());
        
        AuthResponse response = authService.register(registerRequest);
        
        log.info("User registered successfully: {}", registerRequest.username());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT tokens")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("POST /api/auth/login - User login attempt: {}", loginRequest.username());
        
        AuthResponse response = authService.login(loginRequest);
        
        log.info("User logged in successfully: {}", loginRequest.username());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Get new access token using refresh token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        log.info("POST /api/auth/refresh - Refreshing token");
        
        AuthResponse response = authService.refreshToken(refreshTokenRequest);
        
        log.info("Token refreshed successfully");
        return ResponseEntity.ok(response);
    }
}
