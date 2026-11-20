package com.akif.auth.unit;

import java.time.Duration;
import com.akif.auth.api.PasswordResetRequestedEvent;
import com.akif.auth.api.PasswordService;
import com.akif.auth.domain.PasswordResetToken;
import com.akif.auth.internal.repository.PasswordResetTokenRepository;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.auth.internal.service.PasswordResetServiceImpl;
import com.akif.auth.domain.User;
import com.akif.shared.exception.InvalidTokenException;
import com.akif.auth.internal.service.RefreshTokenService;
import com.akif.auth.internal.service.TokenHashing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import java.time.Clock;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetServiceImpl Unit Tests")
class PasswordResetServiceImplTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordService passwordService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<PasswordResetRequestedEvent> eventCaptor;

    @Spy
    private Clock clock = Clock.systemDefaultZone();

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private PasswordResetServiceImpl passwordResetService;

    private User testUser;
    private PasswordResetToken validToken;
    private PasswordResetToken expiredToken;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "baseUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(passwordResetService, "tokenExpiryHours", 1);

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("$2a$10$encoded")
                .build();

        validToken = PasswordResetToken.builder()
                .id(1L)
                .token("valid-token-123")
                .email("test@example.com")
                .expiryDate(Instant.now().plus(Duration.ofHours(1)))
                .used(false)
                .createdAt(Instant.now())
                .build();

        expiredToken = PasswordResetToken.builder()
                .id(2L)
                .token("expired-token-123")
                .email("test@example.com")
                .expiryDate(Instant.now().minus(Duration.ofHours(1)))
                .used(false)
                .createdAt(Instant.now().minus(Duration.ofHours(2)))
                .build();
    }

    @Nested
    @DisplayName("Request Password Reset")
    class RequestPasswordReset {

        @Test
        @DisplayName("Should create token and publish event when email exists")
        void shouldCreateTokenAndPublishEventWhenEmailExists() {
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
            when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(i -> i.getArgument(0));

            passwordResetService.requestPasswordReset("test@example.com");

            verify(tokenRepository).deleteUnusedTokensByEmail("test@example.com");
            verify(tokenRepository).save(any(PasswordResetToken.class));
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            PasswordResetRequestedEvent event = eventCaptor.getValue();
            assertThat(event.email()).isEqualTo("test@example.com");
            assertThat(event.resetLink()).startsWith("http://localhost:3000/reset-password?token=");
        }

        @Test
        @DisplayName("Should store only a hash of the emailed token")
        void shouldStoreOnlyHashOfEmailedToken() {
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
            when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(i -> i.getArgument(0));
            ArgumentCaptor<PasswordResetToken> stored = ArgumentCaptor.forClass(PasswordResetToken.class);

            passwordResetService.requestPasswordReset("test@example.com");

            verify(tokenRepository).save(stored.capture());
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            String emailedToken = eventCaptor.getValue().resetLink().substring(
                    eventCaptor.getValue().resetLink().indexOf("token=") + "token=".length());
            assertThat(stored.getValue().getToken())
                    .isNotEqualTo(emailedToken)
                    .isEqualTo(TokenHashing.sha256Hex(emailedToken))
                    .hasSize(64);
        }

        @Test
        @DisplayName("Should not create token when email does not exist")
        void shouldNotCreateTokenWhenEmailDoesNotExist() {
            when(userRepository.existsByEmail("nonexistent@example.com")).thenReturn(false);

            passwordResetService.requestPasswordReset("nonexistent@example.com");

            verify(tokenRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any(Object.class));
        }
    }

    @Nested
    @DisplayName("Reset Password")
    class ResetPassword {

        @Test
        @DisplayName("Should reset password successfully with valid token")
        void shouldResetPasswordSuccessfullyWithValidToken() {
            when(tokenRepository.findByTokenAndUsedFalse(TokenHashing.sha256Hex("valid-token-123"))).thenReturn(Optional.of(validToken));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(passwordService.hashPassword("newPassword123")).thenReturn("$2a$10$newEncoded");

            passwordResetService.resetPassword("valid-token-123", "newPassword123");

            verify(userRepository).save(argThat(user -> "$2a$10$newEncoded".equals(user.getPassword())));
            verify(tokenRepository).save(argThat(PasswordResetToken::getUsed));
        }

        @Test
        @DisplayName("Should throw exception when token not found")
        void shouldThrowExceptionWhenTokenNotFound() {
            when(tokenRepository.findByTokenAndUsedFalse(TokenHashing.sha256Hex("invalid-token"))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> passwordResetService.resetPassword("invalid-token", "newPassword"))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessage("Invalid password reset token");
        }

        @Test
        @DisplayName("Should throw exception when token is expired")
        void shouldThrowExceptionWhenTokenIsExpired() {
            when(tokenRepository.findByTokenAndUsedFalse(TokenHashing.sha256Hex("expired-token-123"))).thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> passwordResetService.resetPassword("expired-token-123", "newPassword"))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("expired");
        }
    }

    @Nested
    @DisplayName("Validate Token")
    class ValidateToken {

        @Test
        @DisplayName("Should return true for valid token")
        void shouldReturnTrueForValidToken() {
            when(tokenRepository.findByTokenAndUsedFalse(TokenHashing.sha256Hex("valid-token-123"))).thenReturn(Optional.of(validToken));

            boolean result = passwordResetService.validateToken("valid-token-123");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for non-existent token")
        void shouldReturnFalseForNonExistentToken() {
            when(tokenRepository.findByTokenAndUsedFalse(TokenHashing.sha256Hex("invalid-token"))).thenReturn(Optional.empty());

            boolean result = passwordResetService.validateToken("invalid-token");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for expired token")
        void shouldReturnFalseForExpiredToken() {
            when(tokenRepository.findByTokenAndUsedFalse(TokenHashing.sha256Hex("expired-token-123"))).thenReturn(Optional.of(expiredToken));

            boolean result = passwordResetService.validateToken("expired-token-123");

            assertThat(result).isFalse();
        }
    }
}
