package com.akif.auth.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PasswordResetToken expiry")
class PasswordResetTokenTest {

    private static final LocalDateTime EXPIRY = LocalDateTime.of(2026, 5, 1, 12, 0);

    private PasswordResetToken token(boolean used) {
        return PasswordResetToken.builder().token("t").email("a@b.c").expiryDate(EXPIRY).used(used).build();
    }

    @Test
    @DisplayName("token is valid up to and including its expiry instant")
    void validUntilExpiry() {
        assertThat(token(false).isValidAt(EXPIRY)).isTrue();
        assertThat(token(false).isExpiredAt(EXPIRY)).isFalse();
    }

    @Test
    @DisplayName("token expires right after its expiry instant")
    void expiresAfterExpiry() {
        assertThat(token(false).isExpiredAt(EXPIRY.plusNanos(1))).isTrue();
        assertThat(token(false).isValidAt(EXPIRY.plusSeconds(1))).isFalse();
    }

    @Test
    @DisplayName("a used token is never valid")
    void usedTokenIsInvalid() {
        assertThat(token(true).isValidAt(EXPIRY.minusHours(1))).isFalse();
    }
}
