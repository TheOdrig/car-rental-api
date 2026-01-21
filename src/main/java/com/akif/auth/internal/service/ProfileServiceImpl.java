package com.akif.auth.internal.service;

import com.akif.auth.api.ProfileService;
import com.akif.auth.api.UserDto;
import com.akif.auth.domain.User;
import com.akif.auth.internal.dto.ProfileUpdateRequest;
import com.akif.auth.internal.mapper.UserMapper;
import com.akif.auth.internal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserDto getProfile(String username) {
        log.debug("Getting profile for user: {}", username);
        User user = findUserByUsername(username);
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserDto updateProfile(String username, ProfileUpdateRequest request) {
        log.info("Updating profile for user: {}", username);

        User user = findUserByUsername(username);

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());

        if (request.phone() != null && !request.phone().isBlank()) {
            user.setPhone(request.phone());
        } else {
            user.setPhone(null);
        }

        User savedUser = userRepository.save(user);
        log.info("Profile updated successfully for user: {}", username);

        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    public void updateAvatarUrl(String username, String avatarKey) {
        log.info("Updating avatar for user: {}", username);

        User user = findUserByUsername(username);
        user.setAvatarUrl(avatarKey);
        userRepository.save(user);

        log.info("Avatar updated successfully for user: {}", username);
    }

    @Override
    @Transactional
    public void removeAvatar(String username) {
        log.info("Removing avatar for user: {}", username);

        User user = findUserByUsername(username);
        user.setAvatarUrl(null);
        userRepository.save(user);

        log.info("Avatar removed successfully for user: {}", username);
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
    }
}
