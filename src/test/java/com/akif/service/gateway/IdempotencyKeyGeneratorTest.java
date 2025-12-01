package com.akif.service.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IdempotencyKeyGenerator Tests")
class IdempotencyKeyGeneratorTest {

    private IdempotencyKeyGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new IdempotencyKeyGenerator();
    }

    @Nested
    @DisplayName("Checkout Idempotency Key Tests")
    class CheckoutIdempotencyKeyTests {

        @Test
        @DisplayName("Should generate same key for same inputs")
        void shouldGenerateSameKeyForSameInputs() {
            Long rentalId = 123L;
            LocalDateTime timestamp = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

            String key1 = generator.generateForCheckout(rentalId, timestamp);
            String key2 = generator.generateForCheckout(rentalId, timestamp);

            assertThat(key1).isEqualTo(key2);
        }

        @Test
        @DisplayName("Should generate different keys for different rental IDs")
        void shouldGenerateDifferentKeysForDifferentRentalIds() {
            LocalDateTime timestamp = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

            String key1 = generator.generateForCheckout(123L, timestamp);
            String key2 = generator.generateForCheckout(456L, timestamp);

            assertThat(key1).isNotEqualTo(key2);
        }

        @Test
        @DisplayName("Should generate different keys for different timestamps")
        void shouldGenerateDifferentKeysForDifferentTimestamps() {
            Long rentalId = 123L;

            String key1 = generator.generateForCheckout(rentalId, LocalDateTime.of(2024, 1, 15, 10, 30, 0));
            String key2 = generator.generateForCheckout(rentalId, LocalDateTime.of(2024, 1, 15, 10, 30, 1));

            assertThat(key1).isNotEqualTo(key2);
        }

        @Test
        @DisplayName("Should generate non-null and non-empty key")
        void shouldGenerateNonNullAndNonEmptyKey() {
            String key = generator.generateForCheckout(123L, LocalDateTime.now());

            assertThat(key).isNotNull();
            assertThat(key).isNotEmpty();
        }

        @Test
        @DisplayName("Should generate hex string of expected length")
        void shouldGenerateHexStringOfExpectedLength() {
            String key = generator.generateForCheckout(123L, LocalDateTime.now());

            assertThat(key).hasSize(64); // SHA-256 produces 64 hex characters
            assertThat(key).matches("^[0-9a-f]+$"); // Only hex characters
        }
    }

    @Nested
    @DisplayName("Refund Idempotency Key Tests")
    class RefundIdempotencyKeyTests {

        @Test
        @DisplayName("Should generate same key for same inputs")
        void shouldGenerateSameKeyForSameInputs() {
            Long paymentId = 789L;
            BigDecimal amount = new BigDecimal("100.50");

            String key1 = generator.generateForRefund(paymentId, amount);
            String key2 = generator.generateForRefund(paymentId, amount);

            assertThat(key1).isEqualTo(key2);
        }

        @Test
        @DisplayName("Should generate different keys for different payment IDs")
        void shouldGenerateDifferentKeysForDifferentPaymentIds() {
            BigDecimal amount = new BigDecimal("100.50");

            String key1 = generator.generateForRefund(789L, amount);
            String key2 = generator.generateForRefund(999L, amount);

            assertThat(key1).isNotEqualTo(key2);
        }

        @Test
        @DisplayName("Should generate different keys for different amounts")
        void shouldGenerateDifferentKeysForDifferentAmounts() {
            Long paymentId = 789L;

            String key1 = generator.generateForRefund(paymentId, new BigDecimal("100.50"));
            String key2 = generator.generateForRefund(paymentId, new BigDecimal("200.75"));

            assertThat(key1).isNotEqualTo(key2);
        }

        @Test
        @DisplayName("Should generate non-null and non-empty key")
        void shouldGenerateNonNullAndNonEmptyKey() {
            String key = generator.generateForRefund(789L, new BigDecimal("100.50"));

            assertThat(key).isNotNull();
            assertThat(key).isNotEmpty();
        }

        @Test
        @DisplayName("Should generate hex string of expected length")
        void shouldGenerateHexStringOfExpectedLength() {
            String key = generator.generateForRefund(789L, new BigDecimal("100.50"));

            assertThat(key).hasSize(64);
            assertThat(key).matches("^[0-9a-f]+$");
        }
    }

    @Nested
    @DisplayName("Cross-Type Key Tests")
    class CrossTypeKeyTests {

        @Test
        @DisplayName("Checkout and refund keys should be different even with similar inputs")
        void checkoutAndRefundKeysShouldBeDifferent() {
            Long id = 123L;
            LocalDateTime timestamp = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
            BigDecimal amount = new BigDecimal("123");

            String checkoutKey = generator.generateForCheckout(id, timestamp);
            String refundKey = generator.generateForRefund(id, amount);

            assertThat(checkoutKey).isNotEqualTo(refundKey);
        }
    }
}
