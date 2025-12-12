package com.akif.auth.api;

import com.akif.auth.internal.dto.LoginRequest;
import com.akif.auth.internal.dto.RefreshTokenRequest;
import com.akif.auth.internal.dto.RegisterRequest;

public interface AuthService {

    UserDto getUserByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);


    AuthResponse register(RegisterRequest registerRequest);

    AuthResponse login(LoginRequest loginRequest);

    AuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest);
}