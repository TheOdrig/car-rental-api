package com.akif.service.impl;

import com.akif.dto.request.LoginRequestDto;
import com.akif.dto.request.RefreshTokenRequestDto;
import com.akif.dto.request.RegisterRequestDto;
import com.akif.dto.response.AuthResponseDto;
import com.akif.shared.enums.AuthProvider;
import com.akif.shared.enums.Role;
import com.akif.exception.UserAlreadyExistsException;
import com.akif.shared.exception.InvalidTokenException;
import com.akif.exception.SocialLoginRequiredException;
import com.akif.shared.exception.TokenExpiredException;
import com.akif.model.User;
import com.akif.repository.UserRepository;
import com.akif.shared.security.JwtTokenProvider;
import com.akif.service.IAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements IAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Override
    @Transactional
    public AuthResponseDto register(RegisterRequestDto registerRequest) {
        log.info("Registering new user with username: {}", registerRequest.getUsername());

        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new UserAlreadyExistsException("Username is already taken!");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new UserAlreadyExistsException("Email is already in use!");
        }

        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        registerRequest.getUsername(),
                        registerRequest.getPassword()
                )
        );

        return generateTokenResponse(authentication);
    }

    @Override
    public AuthResponseDto login(LoginRequestDto loginRequest) {
        log.info("User attempting to login with username: {}", loginRequest.getUsername());

        Optional<User> userOpt = userRepository.findByUsername(loginRequest.getUsername());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (isSocialOnlyUser(user)) {
                log.warn("Social-only user attempted password login: {}", loginRequest.getUsername());
                String provider = user.getAuthProvider() != null ? user.getAuthProvider().name().toLowerCase() : "social";
                throw new SocialLoginRequiredException(provider, 
                    "This account was created via social login. Please use " + provider + " to sign in.");
            }
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            log.info("User logged in successfully: {}", loginRequest.getUsername());
            return generateTokenResponse(authentication);

        } catch (AuthenticationException e) {
            log.warn("Login failed for user: {}", loginRequest.getUsername());
            throw e;
        }
    }

    private boolean isSocialOnlyUser(User user) {
        return user.getPassword() == null && 
               user.getAuthProvider() != null && 
               user.getAuthProvider() != AuthProvider.LOCAL;
    }


    @Override
    public AuthResponseDto refreshToken(RefreshTokenRequestDto refreshTokenRequest) {
        log.info("Refreshing token");

        if (!tokenProvider.validateToken(refreshTokenRequest.getRefreshToken())) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        if (tokenProvider.isTokenExpired(refreshTokenRequest.getRefreshToken())) {
            throw new TokenExpiredException("Refresh token has expired");
        }

        String username = tokenProvider.getUsernameFromToken(refreshTokenRequest.getRefreshToken());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername(), null, user.getRoles().stream()
                        .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role.name()))
                        .toList()
        );

        return generateTokenResponse(authentication);
    }

    private AuthResponseDto generateTokenResponse(Authentication authentication) {
        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);
        Long expiresIn = tokenProvider.getExpirationTime(accessToken);

        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .username(authentication.getName())
                .build();
    }
}
