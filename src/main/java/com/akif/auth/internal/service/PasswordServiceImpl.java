package com.akif.auth.internal.service;

import com.akif.auth.internal.exception.UserNotFoundException;
import com.akif.auth.api.PasswordService;
import com.akif.auth.domain.User;
import com.akif.auth.internal.exception.InvalidPasswordException;
import com.akif.auth.internal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PasswordServiceImpl implements PasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        log.info("Changing password for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (!verifyPassword(currentPassword, user.getPassword())) {
            log.warn("Invalid current password for user: {}", userId);
            throw new InvalidPasswordException("Current password is incorrect");
        }

        if (currentPassword.equals(newPassword)) {
            throw new InvalidPasswordException("New password must be different from current password");
        }

        user.changePassword(hashPassword(newPassword));
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user.getId());

        log.info("Password changed successfully for user: {}", userId);
    }

    @Override
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Override
    public String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
