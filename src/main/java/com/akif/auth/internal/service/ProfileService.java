package com.akif.auth.internal.service;

import com.akif.auth.api.UserDto;
import com.akif.auth.internal.dto.ProfileUpdateRequest;

public interface ProfileService {

    UserDto getProfile(String username);

    UserDto updateProfile(String username, ProfileUpdateRequest request);

    void updateAvatarUrl(String username, String avatarKey);

    void removeAvatar(String username);
}
