package com.akif.service;

import com.akif.dto.request.LoginRequestDto;
import com.akif.dto.request.RefreshTokenRequestDto;
import com.akif.dto.request.RegisterRequestDto;
import com.akif.dto.response.AuthResponseDto;
import com.akif.enums.Role;
import com.akif.exception.UserAlreadyExistsException;
import com.akif.exception.InvalidTokenException;
import com.akif.exception.TokenExpiredException;
import com.akif.model.User;
import com.akif.repository.UserRepository;
import com.akif.security.JwtTokenProvider;
import com.akif.service.impl.AuthServiceImpl;
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
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequestDto registerRequest;
    private LoginRequestDto loginRequest;
    private RefreshTokenRequestDto refreshTokenRequest;
    private User testUser;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequestDto.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .build();

        loginRequest = LoginRequestDto.builder()
                .username("testuser")
                .password("password123")
                .build();

        refreshTokenRequest = RefreshTokenRequestDto.builder()
                .refreshToken("valid.refresh.token")
                .build();

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
                Set.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
        );
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
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(tokenProvider.generateAccessToken(authentication)).thenReturn("access.token");
            when(tokenProvider.generateRefreshToken(authentication)).thenReturn("refresh.token");
            when(tokenProvider.getExpirationTime("access.token")).thenReturn(900000L);

            AuthResponseDto response = authService.register(registerRequest);

            assertThat(response).isNotNull();
            assertThat(response.getUsername()).isEqualTo("testuser");
            assertThat(response.getAccessToken()).isEqualTo("access.token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh.token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getExpiresIn()).isEqualTo(900000L);

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
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(tokenProvider.generateAccessToken(authentication)).thenReturn("access.token");
            when(tokenProvider.generateRefreshToken(authentication)).thenReturn("refresh.token");
            when(tokenProvider.getExpirationTime("access.token")).thenReturn(900000L);

            AuthResponseDto response = authService.login(loginRequest);

            assertThat(response).isNotNull();
            assertThat(response.getUsername()).isEqualTo("testuser");
            assertThat(response.getAccessToken()).isEqualTo("access.token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh.token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(tokenProvider).generateAccessToken(authentication);
            verify(tokenProvider).generateRefreshToken(authentication);
        }

        @Test
        @DisplayName("Should throw exception for invalid credentials")
        void shouldThrowExceptionForInvalidCredentials() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Invalid credentials"));

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid credentials");

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(tokenProvider, never()).generateAccessToken(any(Authentication.class));
        }
    }

    @Nested
    @DisplayName("Refresh Token Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh token successfully")
        void shouldRefreshTokenSuccessfully() {
            when(tokenProvider.validateToken("valid.refresh.token")).thenReturn(true);
            when(tokenProvider.getUsernameFromToken("valid.refresh.token")).thenReturn("testuser");
            when(userRepository.findByUsername("testuser")).thenReturn(java.util.Optional.of(testUser));
            when(tokenProvider.generateAccessToken(any(Authentication.class))).thenReturn("new.access.token");
            when(tokenProvider.generateRefreshToken(any(Authentication.class))).thenReturn("new.refresh.token");
            when(tokenProvider.getExpirationTime("new.access.token")).thenReturn(900000L);

            AuthResponseDto response = authService.refreshToken(refreshTokenRequest);

            assertThat(response).isNotNull();
            assertThat(response.getUsername()).isEqualTo("testuser");
            assertThat(response.getAccessToken()).isEqualTo("new.access.token");
            assertThat(response.getRefreshToken()).isEqualTo("new.refresh.token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");

            verify(tokenProvider).validateToken("valid.refresh.token");
            verify(tokenProvider).getUsernameFromToken("valid.refresh.token");
            verify(userRepository).findByUsername("testuser");
        }

        @Test
        @DisplayName("Should throw exception for invalid refresh token")
        void shouldThrowExceptionForInvalidRefreshToken() {
            RefreshTokenRequestDto invalidRefreshRequest = RefreshTokenRequestDto.builder()
                    .refreshToken("invalid.refresh.token")
                    .build();
            when(tokenProvider.validateToken("invalid.refresh.token")).thenReturn(false);

            assertThatThrownBy(() -> authService.refreshToken(invalidRefreshRequest))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessage("Invalid refresh token");

            verify(tokenProvider).validateToken("invalid.refresh.token");
            verify(userRepository, never()).findByUsername(anyString());
        }

        @Test
        @DisplayName("Should throw exception for expired refresh token")
        void shouldThrowExceptionForExpiredRefreshToken() {
            RefreshTokenRequestDto expiredRefreshRequest = RefreshTokenRequestDto.builder()
                    .refreshToken("expired.refresh.token")
                    .build();
            when(tokenProvider.validateToken("expired.refresh.token")).thenReturn(true);
            when(tokenProvider.isTokenExpired("expired.refresh.token")).thenReturn(true);

            assertThatThrownBy(() -> authService.refreshToken(expiredRefreshRequest))
                    .isInstanceOf(TokenExpiredException.class)
                    .hasMessage("Refresh token has expired");

            verify(tokenProvider).validateToken("expired.refresh.token");
            verify(tokenProvider).isTokenExpired("expired.refresh.token");
            verify(userRepository, never()).findByUsername(anyString());
        }

        @Test
        @DisplayName("Should throw exception when user not found during refresh")
        void shouldThrowExceptionWhenUserNotFoundDuringRefresh() {
            when(tokenProvider.validateToken("valid.refresh.token")).thenReturn(true);
            when(tokenProvider.getUsernameFromToken("valid.refresh.token")).thenReturn("nonexistent");
            when(userRepository.findByUsername("nonexistent")).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> authService.refreshToken(refreshTokenRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");

            verify(tokenProvider).validateToken("valid.refresh.token");
            verify(tokenProvider).getUsernameFromToken("valid.refresh.token");
            verify(userRepository).findByUsername("nonexistent");
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
