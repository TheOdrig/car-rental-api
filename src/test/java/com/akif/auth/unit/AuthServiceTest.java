package com.akif.auth.unit;

import com.akif.auth.api.AuthResponse;
import com.akif.auth.domain.User;
import com.akif.auth.internal.dto.LoginRequest;
import com.akif.auth.internal.dto.RefreshTokenRequest;
import com.akif.auth.internal.dto.RegisterRequest;
import com.akif.auth.internal.exception.UserAlreadyExistsException;
import com.akif.auth.internal.mapper.UserMapper;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.auth.internal.service.AuthServiceImpl;
import com.akif.shared.enums.Role;
import com.akif.shared.exception.InvalidTokenException;
import com.akif.shared.exception.TokenExpiredException;
import com.akif.shared.security.JwtTokenService;
import com.akif.auth.internal.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenService tokenProvider;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private RefreshTokenRequest refreshTokenRequest;
    private User testUser;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("testuser", "test@example.com", "password123", "Test", "User");

        loginRequest = new LoginRequest("testuser", "password123");

        refreshTokenRequest = new RefreshTokenRequest("valid.refresh.token");

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();

        authentication = new UsernamePasswordAuthenticationToken(
                "testuser",
                "password123",
                Set.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
    }

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register user successfully")
        void shouldRegisterUserSuccessfully() {
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userRepository.findByUsername("testuser")).thenReturn(java.util.Optional.of(testUser));
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(tokenProvider.generateAccessToken(any(Authentication.class), any(Long.class)))
                    .thenReturn("access.token");
            when(refreshTokenService.issue(1L)).thenReturn("refresh.token");
            when(tokenProvider.accessTokenTtl()).thenReturn(java.time.Duration.ofMillis(900000L));

            AuthResponse response = authService.register(registerRequest);

            assertThat(response).isNotNull();
            assertThat(response.username()).isEqualTo("testuser");
            assertThat(response.accessToken()).isEqualTo("access.token");
            assertThat(response.refreshToken()).isEqualTo("refresh.token");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(900L);

            verify(userRepository).existsByUsername("testuser");
            verify(userRepository).existsByEmail("test@example.com");
            verify(passwordEncoder).encode("password123");
            verify(userRepository).save(any(User.class));
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("Should throw exception when username already exists")
        void shouldThrowExceptionWhenUsernameAlreadyExists() {
            when(userRepository.existsByUsername("testuser")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessage("Username is already taken!");

            verify(userRepository).existsByUsername("testuser");
            verify(userRepository, never()).existsByEmail(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessage("Email is already in use!");

            verify(userRepository).existsByUsername("testuser");
            verify(userRepository).existsByEmail("test@example.com");
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login user successfully")
        void shouldLoginUserSuccessfully() {
            when(userRepository.findByUsername("testuser")).thenReturn(java.util.Optional.of(testUser));
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(tokenProvider.generateAccessToken(any(Authentication.class), any(Long.class)))
                    .thenReturn("access.token");
            when(refreshTokenService.issue(1L)).thenReturn("refresh.token");
            when(tokenProvider.accessTokenTtl()).thenReturn(java.time.Duration.ofMillis(900000L));

            AuthResponse response = authService.login(loginRequest);

            assertThat(response).isNotNull();
            assertThat(response.username()).isEqualTo("testuser");
            assertThat(response.accessToken()).isEqualTo("access.token");
            assertThat(response.refreshToken()).isEqualTo("refresh.token");
            assertThat(response.tokenType()).isEqualTo("Bearer");

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(tokenProvider).generateAccessToken(any(Authentication.class), any(Long.class));
            verify(refreshTokenService).issue(1L);
            verify(userRepository, times(2)).findByUsername("testuser");
        }

        @Test
        @DisplayName("Should throw exception for invalid credentials")
        void shouldThrowExceptionForInvalidCredentials() {
            when(userRepository.findByUsername("testuser")).thenReturn(java.util.Optional.of(testUser));
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Invalid credentials"));

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid credentials");

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(tokenProvider, never()).generateAccessToken(any(Authentication.class), any(Long.class));
        }
    }

    @Nested
    @DisplayName("Refresh Token Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should rotate the refresh token and issue a new access token")
        void shouldRefreshTokenSuccessfully() {
            when(refreshTokenService.rotate("valid.refresh.token"))
                    .thenReturn(new RefreshTokenService.Rotation(1L, "new.refresh.token"));
            when(userRepository.findWithRolesById(1L)).thenReturn(java.util.Optional.of(testUser));
            when(tokenProvider.generateAccessToken(any(Authentication.class), any(Long.class)))
                    .thenReturn("new.access.token");
            when(tokenProvider.accessTokenTtl()).thenReturn(java.time.Duration.ofMillis(900000L));

            AuthResponse response = authService.refreshToken(refreshTokenRequest);

            assertThat(response.username()).isEqualTo("testuser");
            assertThat(response.accessToken()).isEqualTo("new.access.token");
            assertThat(response.refreshToken()).isEqualTo("new.refresh.token");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(900L);
            verify(refreshTokenService).rotate("valid.refresh.token");
        }

        @Test
        @DisplayName("Should propagate an invalid refresh token")
        void shouldThrowExceptionForInvalidRefreshToken() {
            when(refreshTokenService.rotate("invalid.refresh.token"))
                    .thenThrow(new InvalidTokenException("Invalid refresh token"));

            assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest("invalid.refresh.token")))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessage("Invalid refresh token");

            verify(userRepository, never()).findWithRolesById(any());
        }

        @Test
        @DisplayName("Should propagate an expired refresh token")
        void shouldThrowExceptionForExpiredRefreshToken() {
            when(refreshTokenService.rotate("expired.refresh.token"))
                    .thenThrow(new TokenExpiredException("Refresh token has expired"));

            assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest("expired.refresh.token")))
                    .isInstanceOf(TokenExpiredException.class)
                    .hasMessage("Refresh token has expired");

            verify(userRepository, never()).findWithRolesById(any());
        }

        @Test
        @DisplayName("Should reject the refresh when the user no longer exists")
        void shouldThrowExceptionWhenUserNotFoundDuringRefresh() {
            when(refreshTokenService.rotate("valid.refresh.token"))
                    .thenReturn(new RefreshTokenService.Rotation(99L, "new.refresh.token"));
            when(userRepository.findWithRolesById(99L)).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> authService.refreshToken(refreshTokenRequest))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("Should revoke every token and reject the refresh when the user is disabled")
        void shouldRevokeAllTokensWhenUserDisabled() {
            User disabled = User.builder().id(1L).username("testuser").email("test@example.com")
                    .roles(Set.of(Role.USER)).enabled(false).build();
            when(refreshTokenService.rotate("valid.refresh.token"))
                    .thenReturn(new RefreshTokenService.Rotation(1L, "new.refresh.token"));
            when(userRepository.findWithRolesById(1L)).thenReturn(java.util.Optional.of(disabled));

            assertThatThrownBy(() -> authService.refreshToken(refreshTokenRequest))
                    .isInstanceOf(InvalidTokenException.class);

            verify(refreshTokenService).revokeAllForUser(1L);
            verify(tokenProvider, never()).generateAccessToken(any(Authentication.class), any(Long.class));
        }

        @Test
        @DisplayName("Should revoke the refresh token on logout")
        void shouldRevokeOnLogout() {
            authService.logout("valid.refresh.token");

            verify(refreshTokenService).revoke("valid.refresh.token");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle null register request")
        void shouldHandleNullRegisterRequest() {
            assertThatThrownBy(() -> authService.register(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should handle null login request")
        void shouldHandleNullLoginRequest() {
            assertThatThrownBy(() -> authService.login(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should handle null refresh token request")
        void shouldHandleNullRefreshTokenRequest() {
            assertThatThrownBy(() -> authService.refreshToken(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
