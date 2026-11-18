package com.akif.payment.unit.gateway;

import com.akif.payment.internal.service.gateway.IdempotencyKeyGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IdempotencyKeyGenerator")
class IdempotencyKeyGeneratorTest {

    private final IdempotencyKeyGenerator keys = new IdempotencyKeyGenerator();

    @Test
    @DisplayName("is deterministic for the same inputs")
    void deterministic() {
        assertThat(keys.forIntent(5L, 9L, 1)).isEqualTo(keys.forIntent(5L, 9L, 1));
        assertThat(keys.forCapture(5L, 9L, 1)).isEqualTo(keys.forCapture(5L, 9L, 1));
        assertThat(keys.forCancel(5L, 9L, 1)).isEqualTo(keys.forCancel(5L, 9L, 1));
        assertThat(keys.forRefund(5L, 9L, 0)).isEqualTo(keys.forRefund(5L, 9L, 0));
        assertThat(keys.forOffSessionCharge(5L, 9L, 1)).isEqualTo(keys.forOffSessionCharge(5L, 9L, 1));
        assertThat(keys.forCustomer(5L)).isEqualTo(keys.forCustomer(5L));
    }

    @Test
    @DisplayName("embeds the rental id, payment id, operation and version")
    void embedsInputs() {
        assertThat(keys.forCapture(5L, 9L, 2)).isEqualTo("car-rental:rental-5:payment-9:capture:2");
        assertThat(keys.forCustomer(5L)).isEqualTo("car-rental:rental-5:customer");
    }

    @Test
    @DisplayName("differs per operation for the same payment and version")
    void differsPerOperation() {
        assertThat(keys.forIntent(5L, 9L, 1))
                .isNotEqualTo(keys.forCapture(5L, 9L, 1))
                .isNotEqualTo(keys.forCancel(5L, 9L, 1))
                .isNotEqualTo(keys.forOffSessionCharge(5L, 9L, 1));
    }

    @Test
    @DisplayName("differs per attempt so a new attempt is a new Stripe operation")
    void differsPerAttempt() {
        assertThat(keys.forIntent(5L, 9L, 1)).isNotEqualTo(keys.forIntent(5L, 9L, 2));
        assertThat(keys.forRefund(5L, 9L, 0)).isNotEqualTo(keys.forRefund(5L, 9L, 1));
    }

    @Test
    @DisplayName("differs per rental and per payment")
    void differsPerRentalAndPayment() {
        assertThat(keys.forCapture(5L, 9L, 1)).isNotEqualTo(keys.forCapture(6L, 9L, 1));
        assertThat(keys.forCapture(5L, 9L, 1)).isNotEqualTo(keys.forCapture(5L, 10L, 1));
    }

    @Test
    @DisplayName("contains no wall-clock value, so a retry cannot produce a new key")
    void noClockInput() {
        String first = keys.forIntent(5L, 9L, 1);
        String later = keys.forIntent(5L, 9L, 1);

        assertThat(later).isEqualTo(first);
        assertThat(first).doesNotContainPattern("\\d{4}-\\d{2}-\\d{2}");
    }
}
