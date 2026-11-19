package com.akif.shared.security.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimitService")
class RateLimitServiceTest {

    private MutableClock clock;
    private RateLimitService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-03-01T10:00:00Z"));
        RateLimitProperties properties = new RateLimitProperties();
        properties.getLimits().put(RateLimitCategory.LOGIN, new RateLimitProperties.Limit(3, 60));
        properties.getLimits().put(RateLimitCategory.REGISTER, new RateLimitProperties.Limit(1, 600));
        service = new RateLimitService(properties, clock);
    }

    @Test
    @DisplayName("allows requests up to the capacity and then rejects with a retry hint")
    void rejectsAfterCapacity() {
        for (int i = 0; i < 3; i++) {
            assertThat(service.tryConsume(RateLimitCategory.LOGIN, "1.1.1.1").allowed()).isTrue();
        }

        RateLimitService.Decision denied = service.tryConsume(RateLimitCategory.LOGIN, "1.1.1.1");

        assertThat(denied.allowed()).isFalse();
        assertThat(denied.retryAfterSeconds()).isBetween(1L, 60L);
    }

    @Test
    @DisplayName("reserve takes a token up front and rejects once the bucket is empty")
    void reserveConsumesAtomically() {
        for (int i = 0; i < 3; i++) {
            service.reserve(RateLimitCategory.LOGIN, "user|1.1.1.1");
        }

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.reserve(RateLimitCategory.LOGIN, "User|1.1.1.1"))
                .isInstanceOf(com.akif.shared.exception.RateLimitExceededException.class);
        service.reserve(RateLimitCategory.LOGIN, "user|2.2.2.2");
    }

    @Test
    @DisplayName("refund returns a reserved token without exceeding the capacity")
    void refundRestoresReservedToken() {
        for (int i = 0; i < 3; i++) {
            service.reserve(RateLimitCategory.LOGIN, "user|1.1.1.1");
        }
        service.refund(RateLimitCategory.LOGIN, "USER|1.1.1.1");
        service.refund(RateLimitCategory.LOGIN, "unknown|9.9.9.9");

        service.reserve(RateLimitCategory.LOGIN, "user|1.1.1.1");
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.reserve(RateLimitCategory.LOGIN, "user|1.1.1.1"))
                .isInstanceOf(com.akif.shared.exception.RateLimitExceededException.class);

        service.refund(RateLimitCategory.LOGIN, "fresh|1.1.1.1");
        service.reserve(RateLimitCategory.LOGIN, "fresh|1.1.1.1");
        service.refund(RateLimitCategory.LOGIN, "fresh|1.1.1.1");
        service.refund(RateLimitCategory.LOGIN, "fresh|1.1.1.1");
        for (int i = 0; i < 3; i++) {
            service.reserve(RateLimitCategory.LOGIN, "fresh|1.1.1.1");
        }
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.reserve(RateLimitCategory.LOGIN, "fresh|1.1.1.1"))
                .isInstanceOf(com.akif.shared.exception.RateLimitExceededException.class);
    }

    @Test
    @DisplayName("tracks each client address separately")
    void tracksClientsSeparately() {
        for (int i = 0; i < 3; i++) {
            service.tryConsume(RateLimitCategory.LOGIN, "1.1.1.1");
        }

        assertThat(service.tryConsume(RateLimitCategory.LOGIN, "1.1.1.1").allowed()).isFalse();
        assertThat(service.tryConsume(RateLimitCategory.LOGIN, "2.2.2.2").allowed()).isTrue();
    }

    @Test
    @DisplayName("tracks each category separately")
    void tracksCategoriesSeparately() {
        assertThat(service.tryConsume(RateLimitCategory.REGISTER, "1.1.1.1").allowed()).isTrue();
        assertThat(service.tryConsume(RateLimitCategory.REGISTER, "1.1.1.1").allowed()).isFalse();

        assertThat(service.tryConsume(RateLimitCategory.LOGIN, "1.1.1.1").allowed()).isTrue();
    }

    @Test
    @DisplayName("refills as the injected clock advances")
    void refillsWithClock() {
        for (int i = 0; i < 3; i++) {
            service.tryConsume(RateLimitCategory.LOGIN, "1.1.1.1");
        }
        assertThat(service.tryConsume(RateLimitCategory.LOGIN, "1.1.1.1").allowed()).isFalse();

        clock.advance(Duration.ofSeconds(21));

        assertThat(service.tryConsume(RateLimitCategory.LOGIN, "1.1.1.1").allowed()).isTrue();
    }

    @Test
    @DisplayName("clear forgets every bucket")
    void clearResetsState() {
        service.tryConsume(RateLimitCategory.REGISTER, "1.1.1.1");
        assertThat(service.tryConsume(RateLimitCategory.REGISTER, "1.1.1.1").allowed()).isFalse();

        service.clear();

        assertThat(service.tryConsume(RateLimitCategory.REGISTER, "1.1.1.1").allowed()).isTrue();
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
