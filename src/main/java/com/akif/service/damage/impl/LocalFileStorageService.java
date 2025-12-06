package com.akif.service.damage.impl;

import com.akif.exception.FileUploadException;
import com.akif.service.damage.IFileUploadService;
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
@Profile("!prod")
@Slf4j
public class LocalFileStorageService implements IFileUploadService {

    private final String baseUploadDir;

    public LocalFileStorageService(@Value("${file.upload.base-dir:uploads}") String baseUploadDir) {
        this.baseUploadDir = baseUploadDir;
    }

    @Override
    public String uploadFile(MultipartFile file, String directory) {
        try {
            Path uploadPath = Paths.get(baseUploadDir, directory);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String fileName = UUID.randomUUID() + extension;
            
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            log.debug("File uploaded to local storage: {}", filePath);
            return filePath.toString().replace("\\", "/");
        } catch (IOException e) {
            throw new FileUploadException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                log.debug("File deleted from local storage: {}", filePath);
            }
        } catch (IOException e) {
            throw new FileUploadException("Failed to delete file: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateSecureUrl(String filePath, int expirationMinutes) {
        return "file:///" + Paths.get(filePath).toAbsolutePath().toString().replace("\\", "/");
    }

    @Override
    public boolean validateFileType(MultipartFile file, List<String> allowedTypes) {
        String contentType = file.getContentType();
        return contentType != null && allowedTypes.contains(contentType);
    }

    @Override
    public boolean validateFileSize(MultipartFile file, long maxSizeBytes) {
        return file.getSize() <= maxSizeBytes;
    }
}
