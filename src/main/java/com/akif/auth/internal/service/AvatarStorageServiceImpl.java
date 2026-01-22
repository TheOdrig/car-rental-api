package com.akif.auth.internal.service;

import com.akif.auth.api.AvatarStorageService;
import com.akif.shared.exception.FileUploadException;
import com.akif.shared.infrastructure.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvatarStorageServiceImpl implements AvatarStorageService {

    private static final String AVATARS_DIRECTORY = "avatars";
    private static final List<String> ALLOWED_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp");
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
    private static final int DEFAULT_URL_EXPIRATION_MINUTES = 60;

    private final FileUploadService fileUploadService;

    @Override
    public String storeAvatar(String username, MultipartFile file) {
        log.info("Storing avatar for user: {}", username);

        validateFile(file);

        String avatarKey = fileUploadService.uploadFile(file, AVATARS_DIRECTORY);

        log.info("Avatar stored successfully for user: {} with key: {}", username, avatarKey);
        return avatarKey;
    }

    @Override
    public void deleteAvatar(String avatarKey) {
        if (avatarKey == null || avatarKey.isBlank()) {
            log.debug("No avatar key provided, skipping deletion");
            return;
        }

        log.info("Deleting avatar with key: {}", avatarKey);
        fileUploadService.deleteFile(avatarKey);
        log.info("Avatar deleted successfully: {}", avatarKey);
    }

    @Override
    public String generateAvatarUrl(String avatarKey, int expirationMinutes) {
        if (avatarKey == null || avatarKey.isBlank()) {
            return null;
        }

        int expiration = expirationMinutes > 0 ? expirationMinutes : DEFAULT_URL_EXPIRATION_MINUTES;
        return fileUploadService.generateSecureUrl(avatarKey, expiration);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("Avatar file is required");
        }

        if (!fileUploadService.validateFileType(file, ALLOWED_TYPES)) {
            throw new FileUploadException("Invalid file type. Allowed types: JPEG, PNG, GIF, WebP");
        }

        if (!fileUploadService.validateFileSize(file, MAX_FILE_SIZE_BYTES)) {
            throw new FileUploadException("File size exceeds maximum limit of 5MB");
        }
    }
}
