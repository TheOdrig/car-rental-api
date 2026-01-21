package com.akif.auth.unit;

import com.akif.auth.domain.User;
import com.akif.auth.internal.exceptipn.InvalidPasswordException;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.auth.internal.service.PasswordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordServiceImpl Unit Tests")
class PasswordServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordServiceImpl passwordService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("$2a$10$encodedOldPassword")
                .build();
    }

    @Nested
    @DisplayName("Change Password")
    class ChangePassword {

        @Test
        @DisplayName("Should change password successfully")
        void shouldChangePasswordSuccessfully() {
            String currentPassword = "oldPassword123";
            String newPassword = "newPassword456";

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(currentPassword, testUser.getPassword())).thenReturn(true);
            when(passwordEncoder.encode(newPassword)).thenReturn("$2a$10$encodedNewPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            passwordService.changePassword("testuser", currentPassword, newPassword);

            verify(userRepository).save(argThat(user -> "$2a$10$encodedNewPassword".equals(user.getPassword())));
        }

        @Test
        @DisplayName("Should throw exception when current password is incorrect")
        void shouldThrowExceptionWhenCurrentPasswordIsIncorrect() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongPassword", testUser.getPassword())).thenReturn(false);

            assertThatThrownBy(() -> passwordService.changePassword("testuser", "wrongPassword", "newPassword456"))
                    .isInstanceOf(InvalidPasswordException.class)
                    .hasMessageContaining("Current password is incorrect");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when new password is same as current")
        void shouldThrowExceptionWhenNewPasswordIsSameAsCurrent() {
            String samePassword = "samePassword123";

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(samePassword, testUser.getPassword())).thenReturn(true);

            assertThatThrownBy(() -> passwordService.changePassword("testuser", samePassword, samePassword))
                    .isInstanceOf(InvalidPasswordException.class)
                    .hasMessageContaining("New password must be different");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> passwordService.changePassword("nonexistent", "current", "new"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("Verify Password")
    class VerifyPassword {

        @Test
        @DisplayName("Should return true when password matches")
        void shouldReturnTrueWhenPasswordMatches() {
            when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);

            boolean result = passwordService.verifyPassword("rawPassword", "encodedPassword");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when password does not match")
        void shouldReturnFalseWhenPasswordDoesNotMatch() {
            when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

            boolean result = passwordService.verifyPassword("wrongPassword", "encodedPassword");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when encoded password is null")
        void shouldReturnFalseWhenEncodedPasswordIsNull() {
            boolean result = passwordService.verifyPassword("rawPassword", null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when encoded password is blank")
        void shouldReturnFalseWhenEncodedPasswordIsBlank() {
            boolean result = passwordService.verifyPassword("rawPassword", "   ");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Hash Password")
    class HashPassword {

        @Test
        @DisplayName("Should hash password using encoder")
        void shouldHashPasswordUsingEncoder() {
            when(passwordEncoder.encode("rawPassword")).thenReturn("$2a$10$hashedPassword");

            String result = passwordService.hashPassword("rawPassword");

            assertThat(result).isEqualTo("$2a$10$hashedPassword");
            verify(passwordEncoder).encode("rawPassword");
        }
    }
}
