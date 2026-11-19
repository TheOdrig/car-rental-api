package com.akif.auth.internal.service;

import java.time.Duration;
import com.akif.auth.internal.exception.UserNotFoundException;
import java.time.Clock;

import com.akif.auth.api.PasswordResetRequestedEvent;
import com.akif.auth.api.PasswordResetService;
import com.akif.auth.api.PasswordService;
import com.akif.auth.domain.PasswordResetToken;
import com.akif.auth.domain.User;
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

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PasswordResetServiceImpl implements PasswordResetService {

    private final Clock clock;

    private static final int TOKEN_EXPIRY_HOURS = 1;

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final RefreshTokenService refreshTokenService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.password-reset.base-url:http://localhost:3000}")
    private String baseUrl;

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        log.info("Password reset requested");

        if (!userRepository.existsByEmail(email)) {
            log.info("Password reset requested for an unknown address");
            return;
        }

        tokenRepository.deleteUnusedTokensByEmail(email);

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(TokenHashing.sha256Hex(token))
                .email(email)
                .expiryDate(Instant.now(clock).plus(Duration.ofHours(TOKEN_EXPIRY_HOURS)))
                .used(false)
                .createdAt(Instant.now(clock))
                .build();

        tokenRepository.save(resetToken);

        String resetLink = buildResetLink(token);
        eventPublisher.publishEvent(new PasswordResetRequestedEvent(email, resetLink));

        log.info("Password reset token created");
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        log.info("Password reset attempt with token");

        PasswordResetToken resetToken = tokenRepository.findByTokenAndUsedFalse(TokenHashing.sha256Hex(token))
                .orElseThrow(() -> new InvalidTokenException("Invalid password reset token",
                        HttpStatus.BAD_REQUEST));

        if (resetToken.isExpiredAt(Instant.now(clock))) {
            throw new InvalidTokenException("Password reset token has expired", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + resetToken.getEmail()));

        user.changePassword(passwordService.hashPassword(newPassword));
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user.getId());

        resetToken.markAsUsed();
        tokenRepository.save(resetToken);

        log.info("Password reset completed for user id: {}", user.getId());
    }

    @Override
    public boolean validateToken(String token) {
        return tokenRepository.findByTokenAndUsedFalse(TokenHashing.sha256Hex(token))
                .map(t -> t.isValidAt(Instant.now(clock)))
                .orElse(false);
    }

    private String buildResetLink(String token) {
        return baseUrl + "/reset-password?token=" + token;
    }
}
