package com.akif.shared.infrastructure;

import com.akif.shared.exception.FileUploadException;
import com.akif.testsupport.ImageFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LocalFileStorageService Unit Tests")
class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new LocalFileStorageService(tempDir.toString());
    }

    @Nested
    @DisplayName("Upload File Operations")
    class UploadFileOperations {

        @Test
        @DisplayName("Should upload file successfully with valid file")
        void shouldUploadFileSuccessfullyWithValidFile() {
            MultipartFile file = new MockMultipartFile(
                    "file",
                    "test-image.jpg",
                    "image/jpeg",
                    ImageFixtures.jpeg());

            String result = fileStorageService.uploadFile(file, "test-directory");

            assertThat(result).isNotNull();
            assertThat(result).contains("test-directory");
            assertThat(result).endsWith(".jpg");

            Path uploadedFile = Path.of(result.replace("/", java.io.File.separator));
            assertThat(Files.exists(uploadedFile)).isTrue();
        }

        @Test
        @DisplayName("Should generate unique filename with UUID")
        void shouldGenerateUniqueFilenameWithUUID() {
            MultipartFile file = new MockMultipartFile(
                    "file",
                    "original-name.png",
                    "image/png",
                    ImageFixtures.png());

            String result = fileStorageService.uploadFile(file, "damage-photos");

            assertThat(result).doesNotContain("original-name");
            assertThat(result).endsWith(".png");
        }

        @Test
        @DisplayName("Should take the extension from the file content, not from the client file name")
        void shouldTakeExtensionFromContent() {
            MultipartFile file = new MockMultipartFile(
                    "file",
                    "no-extension.png/../../escape",
                    "image/png",
                    ImageFixtures.png());

            String result = fileStorageService.uploadFile(file, "test");

            assertThat(Path.of(result).getFileName().toString()).endsWith(".png").doesNotContain("escape");
            assertThat(Files.exists(Path.of(result))).isTrue();
        }

        @Test
        @DisplayName("Should refuse content that is not a supported image")
        void shouldRefuseNonImageContent() {
            MultipartFile file = new MockMultipartFile(
                    "file",
                    "payload.png",
                    "image/png",
                    "<script>alert(1)</script>".getBytes());

            assertThatThrownBy(() -> fileStorageService.uploadFile(file, "test"))
                    .isInstanceOf(FileUploadException.class);
        }

        @Test
        @DisplayName("Should refuse a directory that escapes the upload root")
        void shouldRefuseDirectoryTraversal() {
            MultipartFile file = new MockMultipartFile("file", "a.png", "image/png", ImageFixtures.png());

            assertThatThrownBy(() -> fileStorageService.uploadFile(file, "../outside"))
                    .isInstanceOf(FileUploadException.class);
        }

        @Test
        @DisplayName("Should refuse to delete a path outside the upload root")
        void shouldRefuseDeleteOutsideRoot() {
            assertThatThrownBy(() -> fileStorageService.deleteFile(tempDir.getParent().resolve("other.txt").toString()))
                    .isInstanceOf(FileUploadException.class);
        }

        @Test
        @DisplayName("Should create directory if not exists")
        void shouldCreateDirectoryIfNotExists() {
            MultipartFile file = new MockMultipartFile(
                    "file",
                    "test.jpg",
                    "image/jpeg",
                    ImageFixtures.jpeg());

            String result = fileStorageService.uploadFile(file, "new-directory/sub-directory");

            assertThat(result).isNotNull();
            assertThat(result).contains("new-directory");

            Path createdDir = tempDir.resolve("new-directory/sub-directory");
            assertThat(Files.exists(createdDir)).isTrue();
            assertThat(Files.isDirectory(createdDir)).isTrue();
        }
    }

    @Nested
    @DisplayName("Serving URLs")
    class ServingUrls {

        @Test
        @DisplayName("Should return a relative path so the client can proxy it")
        void shouldReturnARelativePath() {
            assertThat(fileStorageService.generateSecureUrl("uploads/damage-photos/abc.png", 60))
                    .isEqualTo("/api/v1/files/damage-photos/abc.png");
            assertThat(fileStorageService.generateSecureUrl("uploads/avatars/abc.png", 60))
                    .isEqualTo("/api/v1/files/avatars/abc.png");
            assertThat(fileStorageService.generateSecureUrl(" ", 60)).isNull();
        }
    }

    @Nested
    @DisplayName("Validate File Type Operations")
    class ValidateFileTypeOperations {

        @Test
        @DisplayName("Should return true for allowed file type")
        void shouldReturnTrueForAllowedFileType() {
            MultipartFile file = new MockMultipartFile(
                    "file",
                    "test.jpg",
                    "image/jpeg",
                    ImageFixtures.jpeg());
            List<String> allowedTypes = List.of("image/jpeg", "image/png", "image/gif");

            boolean result = fileStorageService.validateFileType(file, allowedTypes);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for disallowed file type")
        void shouldReturnFalseForDisallowedFileType() {
            MultipartFile file = new MockMultipartFile(
                    "file",
                    "test.exe",
                    "application/x-msdownload",
                    "test".getBytes());
            List<String> allowedTypes = List.of("image/jpeg", "image/png");

            boolean result = fileStorageService.validateFileType(file, allowedTypes);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when the content does not match the declared type")
        void shouldReturnFalseForSpoofedContent() {
            List<String> allowedTypes = List.of("image/jpeg", "image/png");

            assertThat(fileStorageService.validateFileType(
                    new MockMultipartFile("file", "a.png", "image/png", "not an image".getBytes()), allowedTypes))
                    .isFalse();
            assertThat(fileStorageService.validateFileType(
                    new MockMultipartFile("file", "a.jpg", "image/jpeg", ImageFixtures.png()), allowedTypes))
                    .isFalse();
        }

        @Test
        @DisplayName("Should return false for an image above the pixel limit")
        void shouldReturnFalseForOversizedImage() {
            MultipartFile file = new MockMultipartFile("file", "big.png", "image/png",
                    ImageFixtures.png(9000, 9000));

            assertThat(fileStorageService.validateFileType(file, List.of("image/png"))).isFalse();
        }

        @Test
        @DisplayName("Should return false for null content type")
        void shouldReturnFalseForNullContentType() {
            MultipartFile file = new MockMultipartFile(
                    "file",
                    "test.unknown",
                    null,
                    "test".getBytes());
            List<String> allowedTypes = List.of("image/jpeg");

            boolean result = fileStorageService.validateFileType(file, allowedTypes);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Validate File Size Operations")
    class ValidateFileSizeOperations {

        @Test
        @DisplayName("Should return true when file size is within limit")
        void shouldReturnTrueWhenFileSizeIsWithinLimit() {
            MultipartFile file = new MockMultipartFile(
                    "file",
                    "test.jpg",
                    "image/jpeg",
                    new byte[1024]);

            boolean result = fileStorageService.validateFileSize(file, 5 * 1024);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true when file size equals limit")
        void shouldReturnTrueWhenFileSizeEqualsLimit() {
            MultipartFile file = new MockMultipartFile(
                    "file",
                    "test.jpg",
                    "image/jpeg",
                    new byte[1024]);

            boolean result = fileStorageService.validateFileSize(file, 1024);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when file size exceeds limit")
        void shouldReturnFalseWhenFileSizeExceedsLimit() {
            MultipartFile file = new MockMultipartFile(
                    "file",
                    "test.jpg",
                    "image/jpeg",
                    new byte[10 * 1024]);

            boolean result = fileStorageService.validateFileSize(file, 5 * 1024);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Generate Secure URL Operations")
    class GenerateSecureUrlOperations {

        @Test
        @DisplayName("Should generate file URL for local storage")
        void shouldGenerateFileUrlForLocalStorage() {
            String filePath = tempDir.resolve("damage-photos/test-file.jpg").toString();

            String result = fileStorageService.generateSecureUrl(filePath, 60);

            assertThat(result).isNotNull();
            assertThat(result).isEqualTo("/api/v1/files/damage-photos/test-file.jpg");
            assertThat(result).contains("test-file.jpg");
        }
    }

    @Nested
    @DisplayName("Delete File Operations")
    class DeleteFileOperations {

        @Test
        @DisplayName("Should delete existing file successfully")
        void shouldDeleteExistingFileSuccessfully() throws IOException {
            Path testFile = tempDir.resolve("test-to-delete.jpg");
            Files.write(testFile, "test content".getBytes());
            assertThat(Files.exists(testFile)).isTrue();

            fileStorageService.deleteFile(testFile.toString());

            assertThat(Files.exists(testFile)).isFalse();
        }

        @Test
        @DisplayName("Should not throw when deleting non-existent file")
        void shouldNotThrowWhenDeletingNonExistentFile() {
            String nonExistentPath = tempDir.resolve("non/existent/path/file.jpg").toString();

            fileStorageService.deleteFile(nonExistentPath);
        }
    }
}
