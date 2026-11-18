package com.akif.auth.internal.service;

import com.akif.auth.domain.RefreshToken;
import com.akif.auth.internal.repository.RefreshTokenRepository;
import com.akif.shared.exception.InvalidTokenException;
import com.akif.shared.exception.TokenExpiredException;
import com.akif.shared.security.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@Slf4j
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository repository;
    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final Duration reuseGracePeriod;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository, JwtProperties jwtProperties, Clock clock,
                               @Value("${jwt.refresh-reuse-grace-seconds:10}") long reuseGraceSeconds) {
        this.repository = repository;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        this.reuseGracePeriod = Duration.ofSeconds(Math.max(0, reuseGraceSeconds));
    }

    public record Rotation(Long userId, String refreshToken) {
    }

    private record Issued(RefreshToken entity, String rawToken) {
    }

    @Transactional
    public String issue(Long userId) {
        return persistNew(userId, UUID.randomUUID(), LocalDateTime.now(clock)).rawToken();
    }

    @Transactional(noRollbackFor = {InvalidTokenException.class, TokenExpiredException.class})
    public Rotation rotate(String rawToken) {
        LocalDateTime now = LocalDateTime.now(clock);
        RefreshToken current = repository.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (current.isRevoked()) {
            if (current.wasRotatedWithin(reuseGracePeriod, now)) {
                log.info("Refresh token for user {} was already rotated moments ago; rejecting without revoking family",
                        current.getUserId());
                throw new InvalidTokenException("Invalid refresh token");
            }
            int revoked = repository.revokeFamily(current.getFamilyId(), now);
            log.warn("Refresh token reuse detected for user {}; revoked {} active token(s) in family {}",
                    current.getUserId(), revoked, current.getFamilyId());
            throw new InvalidTokenException("Invalid refresh token");
        }

        if (current.isExpiredAt(now)) {
            current.revoke(now);
            throw new TokenExpiredException("Refresh token has expired");
        }

        Issued successor = persistNew(current.getUserId(), current.getFamilyId(), now);
        current.rotateTo(successor.entity(), now);
        return new Rotation(current.getUserId(), successor.rawToken());
    }

    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHashForUpdate(hash(rawToken))
                .ifPresent(token -> repository.revokeFamily(token.getFamilyId(), LocalDateTime.now(clock)));
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        repository.revokeAllForUser(userId, LocalDateTime.now(clock));
    }

    @Transactional
    public int purgeExpired(Duration retention) {
        return repository.deleteExpiredBefore(LocalDateTime.now(clock).minus(retention));
    }

    private Issued persistNew(Long userId, UUID familyId, LocalDateTime now) {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime expiresAt = now.plus(Duration.ofMillis(jwtProperties.refreshTokenExpiration()));
        RefreshToken saved = repository.saveAndFlush(
                RefreshToken.issue(userId, familyId, hash(rawToken), now, expiresAt));
        return new Issued(saved, rawToken);
    }

    static String hash(String rawToken) {
        return TokenHashing.sha256Hex(rawToken);
    }
}
