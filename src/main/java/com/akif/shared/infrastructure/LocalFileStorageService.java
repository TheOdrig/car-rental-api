package com.akif.shared.infrastructure;

import com.akif.shared.exception.FileUploadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@Profile({"dev", "test"})
@Slf4j
public class LocalFileStorageService implements FileUploadService {

    private final String baseUploadDir;

    public LocalFileStorageService(@Value("${file.upload.base-dir:uploads}") String baseUploadDir) {
        this.baseUploadDir = baseUploadDir;
    }

    @Override
    public String uploadFile(MultipartFile file, String directory) {
        try {
            UploadContentInspector.InspectedUpload inspected = UploadContentInspector.inspect(file)
                    .orElseThrow(() -> new FileUploadException("File content is not a supported image"));
            Path root = Paths.get(baseUploadDir).toAbsolutePath().normalize();
            Path uploadPath = root.resolve(directory).normalize();
            requireInside(root, uploadPath);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = UUID.randomUUID() + inspected.extension();

            Path filePath = uploadPath.resolve(fileName).normalize();
            requireInside(root, filePath);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            log.debug("File uploaded to local storage: {}", filePath);
            return Paths.get(baseUploadDir).resolve(directory).resolve(fileName).normalize().toString().replace("\\", "/");
        } catch (IOException e) {
            log.error("Failed to write an uploaded file to local storage", e);
            throw FileUploadException.storageFailure(e);
        }
    }

    @Override
    public void deleteFile(String filePath) {
        try {
            Path root = Paths.get(baseUploadDir).toAbsolutePath().normalize();
            Path path = Paths.get(filePath).toAbsolutePath().normalize();
            requireInside(root, path);
            if (Files.exists(path)) {
                Files.delete(path);
                log.debug("File deleted from local storage: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete a file from local storage", e);
            throw FileUploadException.storageFailure(e);
        }
    }

    @Override
    public String generateSecureUrl(String filePath, int expirationMinutes) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }

        Path path = Paths.get(filePath);
        String fileName = path.getFileName().toString();
        String directory = path.getParent() != null ? path.getParent().getFileName().toString() : "uploads";

        return "http://localhost:8082/api/v1/files/" + directory + "/" + fileName;
    }

    @Override
    public boolean validateFileType(MultipartFile file, List<String> allowedTypes) {
        return UploadContentInspector.matchesAllowedTypes(file, allowedTypes);
    }

    private static void requireInside(Path root, Path candidate) {
        if (!candidate.startsWith(root)) {
            throw new FileUploadException("Invalid file path");
        }
    }

    @Override
    public boolean validateFileSize(MultipartFile file, long maxSizeBytes) {
        return file.getSize() <= maxSizeBytes;
    }
}
