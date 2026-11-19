package com.akif.auth.internal.service;

import com.akif.auth.internal.exception.UserNotFoundException;
import com.akif.auth.internal.dto.LoginRequest;
import com.akif.auth.internal.dto.RefreshTokenRequest;
import com.akif.auth.internal.dto.RegisterRequest;
import com.akif.auth.api.AuthResponse;
import com.akif.auth.api.UserLookup;
import com.akif.auth.api.UserDto;
import com.akif.auth.domain.enums.AuthProvider;
import com.akif.auth.internal.mapper.UserMapper;
import com.akif.shared.enums.Role;
import com.akif.auth.internal.exception.UserAlreadyExistsException;
import com.akif.auth.internal.exception.SocialLoginRequiredException;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.shared.security.JwtTokenService;
import com.akif.shared.exception.InvalidTokenException;
import com.akif.shared.exception.TokenExpiredException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService, UserLookup {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;

    
    @Override
    public UserDto getUserByUsername(String username) {
        log.debug("Getting user by username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));
        return userMapper.toDto(user);
    }
    
    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    @Override
    public String loginThrottleKey(String usernameOrEmail) {
        String normalized = usernameOrEmail.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(normalized))
                .map(user -> "user:" + user.getId())
                .orElse("name:" + normalized);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        log.info("Registering new user");

        if (userRepository.existsByUsername(registerRequest.username())) {
            throw new UserAlreadyExistsException("Username is already taken!");
        }

        if (userRepository.existsByEmail(registerRequest.email())) {
            throw new UserAlreadyExistsException("Email is already in use!");
        }

        User user = User.builder()
                .username(registerRequest.username())
                .firstName(registerRequest.firstName())
                .lastName(registerRequest.lastName())
                .email(registerRequest.email().trim().toLowerCase(Locale.ROOT))
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
    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        log.debug("Login attempt received");

        Optional<User> userOpt = userRepository.findByUsername(loginRequest.username());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (isSocialOnlyUser(user)) {
                log.warn("Social-only account attempted password login");
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

            log.debug("Password login succeeded");
            return generateTokenResponse(authentication);

        } catch (AuthenticationException e) {
            log.warn("Login failed");
            throw e;
        }
    }

    private boolean isSocialOnlyUser(User user) {
        return user.getPassword() == null && 
               user.getAuthProvider() != null && 
               user.getAuthProvider() != AuthProvider.LOCAL;
    }


    @Override
    @Transactional(noRollbackFor = {InvalidTokenException.class, TokenExpiredException.class})
    public AuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        log.info("Refreshing token");

        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(refreshTokenRequest.refreshToken());

        User user = userRepository.findWithRolesById(rotation.userId())
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (!user.isUsable()) {
            refreshTokenService.revokeAllForUser(user.getId());
            throw new InvalidTokenException("Invalid refresh token");
        }

        return buildResponse(user, rotation.refreshToken());
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private AuthResponse generateTokenResponse(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return buildResponse(user, refreshTokenService.issue(user.getId()));
    }

    private AuthResponse buildResponse(User user, String refreshToken) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername(), null, user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                        .toList());

        return AuthResponse.builder()
                .accessToken(tokenService.generateAccessToken(authentication, user.getId()))
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenService.accessTokenTtl().toSeconds())
                .username(user.getUsername())
                .build();
    }
}
