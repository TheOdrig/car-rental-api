package com.akif.auth.internal.service;

import com.akif.auth.api.AvatarStorageService;
import com.akif.auth.internal.config.AvatarProperties;
import com.akif.shared.exception.FileUploadException;
import com.akif.shared.infrastructure.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
@RequiredArgsConstructor
@Slf4j
public class AvatarStorageServiceImpl implements AvatarStorageService {

    private static final String AVATARS_DIRECTORY = "avatars";

    private final FileUploadService fileUploadService;
    private final AvatarProperties properties;

    @Override
    public String storeAvatar(Long userId, MultipartFile file) {
        log.info("Storing avatar for user: {}", userId);

        validateFile(file);

        String avatarKey = fileUploadService.uploadFile(file, AVATARS_DIRECTORY);

        log.info("Avatar stored successfully for user: {}", userId);
        return avatarKey;
    }

    @Override
    public void deleteAvatar(String avatarKey) {
        if (avatarKey == null || avatarKey.isBlank()) {
            log.debug("No avatar key provided, skipping deletion");
            return;
        }
        if (isExternal(avatarKey)) {
            log.debug("Avatar is hosted by an OAuth provider, nothing to delete");
            return;
        }

        log.info("Deleting stored avatar");
        fileUploadService.deleteFile(avatarKey);
        log.info("Stored avatar deleted");
    }

    @Override
    public String generateAvatarUrl(String avatarKey, int expirationMinutes) {
        if (avatarKey == null || avatarKey.isBlank()) {
            return null;
        }

        if (isExternal(avatarKey)) {
            return avatarKey;
        }

        int expiration = expirationMinutes > 0 ? expirationMinutes : properties.defaultUrlExpirationMinutes();
        return fileUploadService.generateSecureUrl(avatarKey, expiration);
    }

    private static boolean isExternal(String avatarKey) {
        return avatarKey.startsWith("https://");
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("Avatar file is required");
        }

        if (!fileUploadService.validateFileType(file, properties.allowedTypes())) {
            throw new FileUploadException("Invalid file type. Allowed types: JPEG, PNG, GIF, WebP");
        }

        if (!fileUploadService.validateFileSize(file, properties.maxFileSizeBytes())) {
            throw new FileUploadException("File size exceeds maximum limit of 5MB");
        }
    }
}
