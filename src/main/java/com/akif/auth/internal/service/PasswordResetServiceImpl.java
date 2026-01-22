package com.akif.auth.internal.service;

import com.akif.auth.api.PasswordResetRequestedEvent;
import com.akif.auth.api.PasswordResetService;
import com.akif.auth.api.PasswordService;
import com.akif.auth.domain.PasswordResetToken;
import com.akif.auth.internal.repository.PasswordResetTokenRepository;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.shared.exception.InvalidTokenException;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final int TOKEN_EXPIRY_HOURS = 1;

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.password-reset.base-url:http://localhost:3000}")
    private String baseUrl;

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        log.info("Password reset requested for email: {}", email);

        if (!userRepository.existsByEmail(email)) {
            log.warn("Password reset requested for non-existent email: {}", email);
            return;
        }

        tokenRepository.deleteUnusedTokensByEmail(email);

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .email(email)
                .expiryDate(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();

        tokenRepository.save(resetToken);

        String resetLink = buildResetLink(token);
        eventPublisher.publishEvent(new PasswordResetRequestedEvent(email, resetLink));

        log.info("Password reset token created for email: {}", email);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        log.info("Password reset attempt with token");

        PasswordResetToken resetToken = tokenRepository.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired password reset token",
                        HttpStatus.BAD_REQUEST));

        if (resetToken.isExpired()) {
            throw new InvalidTokenException("Password reset token has expired", HttpStatus.BAD_REQUEST);
        }

        var user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + resetToken.getEmail()));

        user.setPassword(passwordService.hashPassword(newPassword));
        userRepository.save(user);

        resetToken.markAsUsed();
        tokenRepository.save(resetToken);

        log.info("Password reset successfully for email: {}", resetToken.getEmail());
    }

    @Override
    public boolean validateToken(String token) {
        return tokenRepository.findByTokenAndUsedFalse(token)
                .map(PasswordResetToken::isValid)
                .orElse(false);
    }

    private String buildResetLink(String token) {
        return baseUrl + "/reset-password?token=" + token;
    }
}
