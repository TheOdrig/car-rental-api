package com.akif.auth.internal.service;

import com.akif.auth.internal.dto.LoginRequest;
import com.akif.auth.internal.dto.RefreshTokenRequest;
import com.akif.auth.internal.dto.RegisterRequest;
import com.akif.auth.api.AuthResponse;
import com.akif.auth.api.UserDto;
import com.akif.auth.domain.enums.AuthProvider;
import com.akif.auth.internal.mapper.UserMapper;
import com.akif.shared.enums.Role;
import com.akif.auth.internal.exceptipn.UserAlreadyExistsException;
import com.akif.shared.exception.InvalidTokenException;
import com.akif.auth.internal.exceptipn.SocialLoginRequiredException;
import com.akif.shared.exception.TokenExpiredException;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.shared.security.JwtTokenProvider;
import com.akif.auth.api.AuthService;
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
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;

    
    @Override
    public UserDto getUserByUsername(String username) {
        log.debug("Getting user by username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        return userMapper.toDto(user);
    }
    
    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        log.info("Registering new user with username: {}", registerRequest.username());

        if (userRepository.existsByUsername(registerRequest.username())) {
            throw new UserAlreadyExistsException("Username is already taken!");
        }

        if (userRepository.existsByEmail(registerRequest.email())) {
            throw new UserAlreadyExistsException("Email is already in use!");
        }

        User user = User.builder()
                .username(registerRequest.username())
                .email(registerRequest.email())
                .password(passwordEncoder.encode(registerRequest.password()))
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        registerRequest.username(),
                        registerRequest.password()
                )
        );

        return generateTokenResponse(authentication);
    }

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        log.info("User attempting to login with username: {}", loginRequest.username());

        Optional<User> userOpt = userRepository.findByUsername(loginRequest.username());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (isSocialOnlyUser(user)) {
                log.warn("Social-only user attempted password login: {}", loginRequest.username());
                String provider = user.getAuthProvider() != null ? user.getAuthProvider().name().toLowerCase() : "social";
                throw new SocialLoginRequiredException(provider, 
                    "This account was created via social login. Please use " + provider + " to sign in.");
            }
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.username(),
                            loginRequest.password()
                    )
            );

            log.info("User logged in successfully: {}", loginRequest.username());
            return generateTokenResponse(authentication);

        } catch (AuthenticationException e) {
            log.warn("Login failed for user: {}", loginRequest.username());
            throw e;
        }
    }

    private boolean isSocialOnlyUser(User user) {
        return user.getPassword() == null && 
               user.getAuthProvider() != null && 
               user.getAuthProvider() != AuthProvider.LOCAL;
    }


    @Override
    public AuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        log.info("Refreshing token");

        if (!tokenProvider.validateToken(refreshTokenRequest.refreshToken())) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        if (tokenProvider.isTokenExpired(refreshTokenRequest.refreshToken())) {
            throw new TokenExpiredException("Refresh token has expired");
        }

        String username = tokenProvider.getUsernameFromToken(refreshTokenRequest.refreshToken());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername(), null, user.getRoles().stream()
                        .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role.name()))
                        .toList()
        );

        return generateTokenResponse(authentication);
    }

    private AuthResponse generateTokenResponse(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = tokenProvider.generateAccessToken(authentication, user.getId());
        String refreshToken = tokenProvider.generateRefreshToken(authentication);
        Long expiresIn = tokenProvider.getExpirationTime(accessToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .username(authentication.getName())
                .build();
    }
}
