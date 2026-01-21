package com.akif.auth.unit;

import com.akif.auth.internal.service.AvatarStorageServiceImpl;
import com.akif.shared.exception.FileUploadException;
import com.akif.shared.infrastructure.FileUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AvatarStorageServiceImpl Unit Tests")
class AvatarStorageServiceImplTest {

    @Mock
    private FileUploadService fileUploadService;

    @InjectMocks
    private AvatarStorageServiceImpl avatarStorageService;

    private MockMultipartFile validImage;
    private MockMultipartFile invalidImage;
    private MockMultipartFile largeImage;

    @BeforeEach
    void setUp() {
        validImage = new MockMultipartFile("avatar", "avatar.jpg", "image/jpeg", new byte[1024]);
        invalidImage = new MockMultipartFile("avatar", "document.pdf", "application/pdf", new byte[1024]);
        largeImage = new MockMultipartFile("avatar", "large.jpg", "image/jpeg", new byte[10 * 1024 * 1024]);
    }

    @Nested
    @DisplayName("Store Avatar")
    class StoreAvatar {

        @Test
        @DisplayName("Should store avatar successfully")
        void shouldStoreAvatarSuccessfully() {
            when(fileUploadService.validateFileType(eq(validImage), anyList())).thenReturn(true);
            when(fileUploadService.validateFileSize(eq(validImage), anyLong())).thenReturn(true);
            when(fileUploadService.uploadFile(eq(validImage), eq("avatars"))).thenReturn("avatars/user1_123.jpg");

            String result = avatarStorageService.storeAvatar("testuser", validImage);

            assertThat(result).isEqualTo("avatars/user1_123.jpg");
            verify(fileUploadService).uploadFile(eq(validImage), eq("avatars"));
        }

        @Test
        @DisplayName("Should throw exception for invalid file type")
        void shouldThrowExceptionForInvalidFileType() {
            when(fileUploadService.validateFileType(eq(invalidImage), anyList())).thenReturn(false);

            assertThatThrownBy(() -> avatarStorageService.storeAvatar("testuser", invalidImage))
                    .isInstanceOf(FileUploadException.class)
                    .hasMessageContaining("Invalid file type");

            verify(fileUploadService, never()).uploadFile(any(), any());
        }

        @Test
        @DisplayName("Should throw exception for oversized file")
        void shouldThrowExceptionForOversizedFile() {
            when(fileUploadService.validateFileType(eq(largeImage), anyList())).thenReturn(true);
            when(fileUploadService.validateFileSize(eq(largeImage), anyLong())).thenReturn(false);

            assertThatThrownBy(() -> avatarStorageService.storeAvatar("testuser", largeImage))
                    .isInstanceOf(FileUploadException.class)
                    .hasMessageContaining("File size exceeds");

            verify(fileUploadService, never()).uploadFile(any(), any());
        }

        @Test
        @DisplayName("Should throw exception for null file")
        void shouldThrowExceptionForNullFile() {
            assertThatThrownBy(() -> avatarStorageService.storeAvatar("testuser", null))
                    .isInstanceOf(FileUploadException.class)
                    .hasMessageContaining("required");
        }
    }

    @Nested
    @DisplayName("Delete Avatar")
    class DeleteAvatar {

        @Test
        @DisplayName("Should delete avatar successfully")
        void shouldDeleteAvatarSuccessfully() {
            String avatarKey = "avatars/user1_123.jpg";
            doNothing().when(fileUploadService).deleteFile(avatarKey);

            avatarStorageService.deleteAvatar(avatarKey);

            verify(fileUploadService).deleteFile(avatarKey);
        }

        @Test
        @DisplayName("Should skip deletion when avatar key is null")
        void shouldSkipDeletionWhenAvatarKeyIsNull() {
            avatarStorageService.deleteAvatar(null);

            verify(fileUploadService, never()).deleteFile(any());
        }

        @Test
        @DisplayName("Should skip deletion when avatar key is blank")
        void shouldSkipDeletionWhenAvatarKeyIsBlank() {
            avatarStorageService.deleteAvatar("   ");

            verify(fileUploadService, never()).deleteFile(any());
        }
    }

    @Nested
    @DisplayName("Generate Avatar URL")
    class GenerateAvatarUrl {

        @Test
        @DisplayName("Should generate secure URL for avatar")
        void shouldGenerateSecureUrlForAvatar() {
            String avatarKey = "avatars/user1_123.jpg";
            when(fileUploadService.generateSecureUrl(avatarKey, 60))
                    .thenReturn("https://cdn.example.com/avatars/user1_123.jpg?token=xyz");

            String result = avatarStorageService.generateAvatarUrl(avatarKey, 60);

            assertThat(result).isEqualTo("https://cdn.example.com/avatars/user1_123.jpg?token=xyz");
        }

        @Test
        @DisplayName("Should use default expiration when negative value provided")
        void shouldUseDefaultExpirationWhenNegativeValueProvided() {
            String avatarKey = "avatars/user1_123.jpg";
            when(fileUploadService.generateSecureUrl(avatarKey, 60))
                    .thenReturn("https://cdn.example.com/avatars/user1_123.jpg?token=xyz");

            String result = avatarStorageService.generateAvatarUrl(avatarKey, -1);

            assertThat(result).isEqualTo("https://cdn.example.com/avatars/user1_123.jpg?token=xyz");
            verify(fileUploadService).generateSecureUrl(avatarKey, 60);
        }

        @Test
        @DisplayName("Should return null when avatar key is null")
        void shouldReturnNullWhenAvatarKeyIsNull() {
            String result = avatarStorageService.generateAvatarUrl(null, 60);

            assertThat(result).isNull();
            verify(fileUploadService, never()).generateSecureUrl(any(), anyInt());
        }

        @Test
        @DisplayName("Should return null when avatar key is blank")
        void shouldReturnNullWhenAvatarKeyIsBlank() {
            String result = avatarStorageService.generateAvatarUrl("   ", 60);

            assertThat(result).isNull();
            verify(fileUploadService, never()).generateSecureUrl(any(), anyInt());
        }
    }
}
