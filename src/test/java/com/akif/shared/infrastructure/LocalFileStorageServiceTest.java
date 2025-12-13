package com.akif.shared.infrastructure;

import com.akif.shared.infrastructure.LocalFileStorageService;
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
                    "test image content".getBytes()
            );

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
                    "test content".getBytes()
            );

            String result = fileStorageService.uploadFile(file, "damage-photos");

            assertThat(result).doesNotContain("original-name");
            assertThat(result).endsWith(".png");
        }

        @Test
        @DisplayName("Should handle file without extension")
        void shouldHandleFileWithoutExtension() {
            MultipartFile file = new MockMultipartFile(
                    "file",
                    "no-extension",
                    "application/octet-stream",
                    "test content".getBytes()
            );

            String result = fileStorageService.uploadFile(file, "test");

            assertThat(result).isNotNull();
            String fileName = Path.of(result).getFileName().toString();
            assertThat(fileName).doesNotContain(".");
        }

        @Test
        @DisplayName("Should create directory if not exists")
        void shouldCreateDirectoryIfNotExists() {
            MultipartFile file = new MockMultipartFile(
                    "file",
                    "test.jpg",
                    "image/jpeg",
                    "test".getBytes()
            );

            String result = fileStorageService.uploadFile(file, "new-directory/sub-directory");

            assertThat(result).isNotNull();
            assertThat(result).contains("new-directory");

            Path createdDir = tempDir.resolve("new-directory/sub-directory");
            assertThat(Files.exists(createdDir)).isTrue();
            assertThat(Files.isDirectory(createdDir)).isTrue();
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
                    "test".getBytes()
            );
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
                    "test".getBytes()
            );
            List<String> allowedTypes = List.of("image/jpeg", "image/png");

            boolean result = fileStorageService.validateFileType(file, allowedTypes);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for null content type")
        void shouldReturnFalseForNullContentType() {
            MultipartFile file = new MockMultipartFile(
                    "file",
                    "test.unknown",
                    null,
                    "test".getBytes()
            );
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
                    new byte[1024]
            );

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
                    new byte[1024]
            );

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
                    new byte[10 * 1024]
            );

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
            assertThat(result).startsWith("file:///");
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
