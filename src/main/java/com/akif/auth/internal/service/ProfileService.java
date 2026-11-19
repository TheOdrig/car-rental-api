package com.akif.auth.internal.service;

import com.akif.auth.api.UserDto;
import com.akif.auth.internal.dto.ProfileUpdateRequest;

public interface ProfileService {

    UserDto getProfile(Long userId);

    UserDto updateProfile(Long userId, ProfileUpdateRequest request);

    void updateAvatarUrl(Long userId, String avatarKey);

    void removeAvatar(Long userId);
}
