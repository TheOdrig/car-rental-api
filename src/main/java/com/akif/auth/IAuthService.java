package com.akif.auth;

public interface IAuthService {

    UserDto getUserById(Long id);

    UserDto getUserByUsername(String username);

    UserDto getUserByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);


    AuthResponse register(RegisterRequest registerRequest);

    AuthResponse login(LoginRequest loginRequest);

    AuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest);
}