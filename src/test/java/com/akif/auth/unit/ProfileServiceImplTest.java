package com.akif.auth.unit;

import com.akif.auth.api.UserDto;
import com.akif.auth.domain.User;
import com.akif.auth.internal.dto.ProfileUpdateRequest;
import com.akif.auth.internal.mapper.UserMapper;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.auth.internal.service.ProfileServiceImpl;
import com.akif.shared.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileServiceImpl Unit Tests")
class ProfileServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private User testUser;
    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .phone(null)
                .avatarUrl(null)
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();

        testUserDto = createUserDto(1L, "testuser", "test@example.com", "Test", "User", null, null);
    }

    private UserDto createUserDto(Long id, String username, String email, String firstName, String lastName,
            String phone, String avatarUrl) {
        return new UserDto(id, username, email, firstName, lastName, phone, avatarUrl, Set.of(Role.USER), true);
    }

    @Nested
    @DisplayName("Get Profile")
    class GetProfile {

        @Test
        @DisplayName("Should return user profile when user exists")
        void shouldReturnUserProfileWhenUserExists() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(userMapper.toDto(testUser)).thenReturn(testUserDto);

            UserDto result = profileService.getProfile("testuser");

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.username()).isEqualTo("testuser");
            assertThat(result.email()).isEqualTo("test@example.com");
            verify(userRepository).findByUsername("testuser");
            verify(userMapper).toDto(testUser);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.getProfile("nonexistent"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("Update Profile")
    class UpdateProfile {

        @Test
        @DisplayName("Should update profile successfully")
        void shouldUpdateProfileSuccessfully() {
            ProfileUpdateRequest request = new ProfileUpdateRequest("John", "Doe", "+1234567890");
            User updatedUser = User.builder()
                    .id(1L)
                    .username("testuser")
                    .email("test@example.com")
                    .firstName("John")
                    .lastName("Doe")
                    .phone("+1234567890")
                    .build();
            UserDto updatedDto = createUserDto(1L, "testuser", "test@example.com", "John", "Doe", "+1234567890", null);

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(updatedUser);
            when(userMapper.toDto(any(User.class))).thenReturn(updatedDto);

            UserDto result = profileService.updateProfile("testuser", request);

            assertThat(result).isNotNull();
            assertThat(result.firstName()).isEqualTo("John");
            assertThat(result.lastName()).isEqualTo("Doe");
            assertThat(result.phone()).isEqualTo("+1234567890");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should clear phone when empty string provided")
        void shouldClearPhoneWhenEmptyStringProvided() {
            testUser.setPhone("+1234567890");
            ProfileUpdateRequest request = new ProfileUpdateRequest("John", "Doe", "");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userMapper.toDto(any(User.class))).thenReturn(testUserDto);

            profileService.updateProfile("testuser", request);

            verify(userRepository).save(argThat(user -> user.getPhone() == null));
        }

        @Test
        @DisplayName("Should throw exception when user not found on update")
        void shouldThrowExceptionWhenUserNotFoundOnUpdate() {
            ProfileUpdateRequest request = new ProfileUpdateRequest("John", "Doe", null);
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.updateProfile("nonexistent", request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("Avatar Management")
    class AvatarManagement {

        @Test
        @DisplayName("Should update avatar URL successfully")
        void shouldUpdateAvatarUrlSuccessfully() {
            String avatarKey = "avatars/user1_12345.jpg";
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            profileService.updateAvatarUrl("testuser", avatarKey);

            verify(userRepository).save(argThat(user -> avatarKey.equals(user.getAvatarUrl())));
        }

        @Test
        @DisplayName("Should remove avatar successfully")
        void shouldRemoveAvatarSuccessfully() {
            testUser.setAvatarUrl("avatars/user1_old.jpg");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            profileService.removeAvatar("testuser");

            verify(userRepository).save(argThat(user -> user.getAvatarUrl() == null));
        }
    }
}
