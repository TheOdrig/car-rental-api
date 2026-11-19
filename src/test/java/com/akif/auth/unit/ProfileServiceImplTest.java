package com.akif.auth.unit;

import com.akif.auth.api.AvatarStorageService;
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

    @Mock
    private AvatarStorageService avatarStorageService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

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
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userMapper.toDto(testUser)).thenReturn(testUserDto);

            UserDto result = profileService.getProfile(1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.username()).isEqualTo("testuser");
            assertThat(result.email()).isEqualTo("test@example.com");
            verify(userRepository).findById(1L);
            verify(userMapper).toDto(testUser);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.getProfile(404L))
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

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(updatedUser);
            when(userMapper.toDto(any(User.class))).thenReturn(updatedDto);

            UserDto result = profileService.updateProfile(1L, request);

            assertThat(result).isNotNull();
            assertThat(result.firstName()).isEqualTo("John");
            assertThat(result.lastName()).isEqualTo("Doe");
            assertThat(result.phone()).isEqualTo("+1234567890");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should clear phone when empty string provided")
        void shouldClearPhoneWhenEmptyStringProvided() {
            testUser.updateProfile(testUser.getFirstName(), testUser.getLastName(), "+1234567890");
            ProfileUpdateRequest request = new ProfileUpdateRequest("John", "Doe", "");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userMapper.toDto(any(User.class))).thenReturn(testUserDto);

            profileService.updateProfile(1L, request);

            verify(userRepository).save(argThat(user -> user.getPhone() == null));
        }

        @Test
        @DisplayName("Should throw exception when user not found on update")
        void shouldThrowExceptionWhenUserNotFoundOnUpdate() {
            ProfileUpdateRequest request = new ProfileUpdateRequest("John", "Doe", null);
            when(userRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.updateProfile(404L, request))
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
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            profileService.updateAvatarUrl(1L, avatarKey);

            verify(userRepository).save(argThat(user -> avatarKey.equals(user.getAvatarUrl())));
        }

        @Test
        @DisplayName("Should remove avatar successfully")
        void shouldRemoveAvatarSuccessfully() {
            testUser.changeAvatar("avatars/user1_old.jpg");
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            profileService.removeAvatar(1L);

            verify(userRepository).save(argThat(user -> user.getAvatarUrl() == null));
        }

        @Test
        @DisplayName("Should delete the stored object by its key, not by a presigned link")
        void shouldDeleteByStoredKey() {
            testUser.changeAvatar("avatars/user1_old.jpg");
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userMapper.toDto(testUser)).thenReturn(testUserDto);
            when(avatarStorageService.generateAvatarUrl(anyString(), anyInt()))
                    .thenReturn("https://cdn.example.com/avatars/user1_old.jpg?X-Amz-Signature=abc");
            profileService.getProfile(1L);

            profileService.removeAvatar(1L);

            verify(avatarStorageService).deleteAvatar("avatars/user1_old.jpg");
            verify(avatarStorageService, never()).deleteAvatar(contains("https://"));
        }

        @Test
        @DisplayName("Should not call storage when there is no avatar to remove")
        void shouldNotDeleteWhenNoAvatar() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            profileService.removeAvatar(1L);

            verify(avatarStorageService, never()).deleteAvatar(any());
        }

        @Test
        @DisplayName("Should delete the previous object when a new avatar replaces it")
        void shouldDeletePreviousObjectOnReupload() {
            testUser.changeAvatar("avatars/user1_old.jpg");
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            profileService.updateAvatarUrl(1L, "avatars/user1_new.jpg");

            verify(avatarStorageService).deleteAvatar("avatars/user1_old.jpg");
            verify(avatarStorageService, never()).deleteAvatar("avatars/user1_new.jpg");
        }

        @Test
        @DisplayName("Should keep the object when the same key is stored again")
        void shouldKeepObjectWhenKeyUnchanged() {
            testUser.changeAvatar("avatars/user1_same.jpg");
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            profileService.updateAvatarUrl(1L, "avatars/user1_same.jpg");

            verify(avatarStorageService, never()).deleteAvatar(any());
        }

        @Test
        @DisplayName("Should not fail the request when the old object cannot be deleted")
        void shouldSurviveStorageFailure() {
            testUser.changeAvatar("avatars/user1_old.jpg");
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            doThrow(new RuntimeException("storage down")).when(avatarStorageService).deleteAvatar(any());

            profileService.removeAvatar(1L);

            assertThat(testUser.getAvatarUrl()).isNull();
        }
    }
}
