package com.akif.auth.api;

import org.springframework.web.multipart.MultipartFile;

public interface AvatarStorageService {

    String storeAvatar(String username, MultipartFile file);

    void deleteAvatar(String avatarKey);

    String generateAvatarUrl(String avatarKey, int expirationMinutes);
}
