package com.akif.auth.internal.service;

import com.akif.auth.api.AuthResponse;
import com.akif.auth.internal.dto.LoginRequest;
import com.akif.auth.internal.dto.RefreshTokenRequest;
import com.akif.auth.internal.dto.RegisterRequest;

public interface AuthService {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);


    AuthResponse register(RegisterRequest registerRequest);

    AuthResponse login(LoginRequest loginRequest);

    AuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest);

    void logout(String refreshToken);
}