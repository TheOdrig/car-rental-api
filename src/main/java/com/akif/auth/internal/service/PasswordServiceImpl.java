package com.akif.auth.internal.service;

import com.akif.auth.api.PasswordService;
import com.akif.auth.domain.User;
import com.akif.auth.internal.exceptipn.InvalidPasswordException;
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

    @Override
    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        log.info("Changing password for user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        if (!verifyPassword(currentPassword, user.getPassword())) {
            log.warn("Invalid current password for user: {}", username);
            throw new InvalidPasswordException("Current password is incorrect");
        }

        if (currentPassword.equals(newPassword)) {
            throw new InvalidPasswordException("New password must be different from current password");
        }

        user.setPassword(hashPassword(newPassword));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", username);
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
