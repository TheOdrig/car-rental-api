package com.akif.payment.unit.gateway;

import com.akif.payment.internal.config.InstallationIdentity;
import com.akif.payment.internal.service.gateway.IdempotencyKeyGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IdempotencyKeyGenerator")
class IdempotencyKeyGeneratorTest {

    private static final String INSTALLATION = "0b1c2d3e-4f50-4a6b-8c7d-9e0f1a2b3c4d";
    private static final String OTHER_INSTALLATION = "ffffffff-4f50-4a6b-8c7d-9e0f1a2b3c4d";

    private final IdempotencyKeyGenerator keys =
            new IdempotencyKeyGenerator(new InstallationIdentity(() -> INSTALLATION));

    @Test
    @DisplayName("is deterministic for the same inputs")
    void deterministic() {
        assertThat(keys.forIntent(5L, 9L, 1)).isEqualTo(keys.forIntent(5L, 9L, 1));
        assertThat(keys.forCapture(5L, 9L, 1)).isEqualTo(keys.forCapture(5L, 9L, 1));
        assertThat(keys.forCancel(5L, 9L, 1)).isEqualTo(keys.forCancel(5L, 9L, 1));
        assertThat(keys.forRefundOperation("rental-5:cancel-refund", 0)).isEqualTo(keys.forRefundOperation("rental-5:cancel-refund", 0));
        assertThat(keys.forPenaltyCharge(5L, 9L, 1)).isEqualTo(keys.forPenaltyCharge(5L, 9L, 1));
        assertThat(keys.forDamageCharge(7L, 3L, 1)).isEqualTo(keys.forDamageCharge(7L, 3L, 1));
        assertThat(keys.forCustomer(5L)).isEqualTo(keys.forCustomer(5L));
    }

    @Test
    @DisplayName("embeds the rental id, payment id, operation and version")
    void embedsInputs() {
        assertThat(keys.forCapture(5L, 9L, 2)).isEqualTo("car-rental:" + INSTALLATION + ":rental-5:payment-9:capture:2");
        assertThat(keys.forCustomer(5L)).isEqualTo("car-rental:" + INSTALLATION + ":rental-5:customer");
    }

    @Test
    @DisplayName("differs per operation for the same payment and version")
    void differsPerOperation() {
        assertThat(keys.forIntent(5L, 9L, 1))
                .isNotEqualTo(keys.forCapture(5L, 9L, 1))
                .isNotEqualTo(keys.forCancel(5L, 9L, 1))
                .isNotEqualTo(keys.forPenaltyCharge(5L, 9L, 1));
    }

    @Test
    @DisplayName("differs per attempt so a new attempt is a new Stripe operation")
    void differsPerAttempt() {
        assertThat(keys.forIntent(5L, 9L, 1)).isNotEqualTo(keys.forIntent(5L, 9L, 2));
        assertThat(keys.forPenaltyCharge(5L, 9L, 1)).isNotEqualTo(keys.forPenaltyCharge(5L, 9L, 2));
        assertThat(keys.forRefundOperation("waiver-3:refund", 0)).isNotEqualTo(keys.forRefundOperation("waiver-3:refund", 1));
    }

    @Test
    @DisplayName("charge and refund keys follow the business identity, not the payment row")
    void businessIdentity() {
        assertThat(keys.forPenaltyCharge(5L, 9L, 1)).isEqualTo("car-rental:" + INSTALLATION + ":rental-5:payment-9:penalty:1");
        assertThat(keys.forDamageCharge(7L, 3L, 2)).isEqualTo("car-rental:" + INSTALLATION + ":damage-7:payment-3:charge:2");
        assertThat(keys.forRefundOperation("rental-5:cancel-refund", 0)).isEqualTo("car-rental:" + INSTALLATION + ":rental-5:cancel-refund");
        assertThat(keys.forRefundOperation("rental-5:cancel-refund", 1)).isEqualTo("car-rental:" + INSTALLATION + ":rental-5:cancel-refund:retry-1");
    }

    @Test
    @DisplayName("a second penalty payment of the same rental gets its own key")
    void penaltyKeyIncludesThePaymentId() {
        assertThat(keys.forPenaltyCharge(5L, 9L, 1)).isNotEqualTo(keys.forPenaltyCharge(5L, 10L, 1));
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

    @Test
    @DisplayName("every key starts with the installation identity so another database cannot collide")
    void everyKeyCarriesTheInstallation() {
        String prefix = "car-rental:" + INSTALLATION + ":";

        assertThat(keys.forCustomer(5L)).startsWith(prefix);
        assertThat(keys.forIntent(5L, 9L, 1)).startsWith(prefix);
        assertThat(keys.forCapture(5L, 9L, 1)).startsWith(prefix);
        assertThat(keys.forCancel(5L, 9L, 1)).startsWith(prefix);
        assertThat(keys.forOrphanCancel("pi_1")).startsWith(prefix);
        assertThat(keys.forPenaltyCharge(5L, 9L, 1)).startsWith(prefix);
        assertThat(keys.forDamageCharge(7L, 3L, 1)).startsWith(prefix);
        assertThat(keys.forRefundOperation("rental-5:cancel-refund", 0)).startsWith(prefix);
    }

    @Test
    @DisplayName("the same ids under another installation produce different keys")
    void differsPerInstallation() {
        IdempotencyKeyGenerator other = new IdempotencyKeyGenerator(new InstallationIdentity(() -> OTHER_INSTALLATION));

        assertThat(other.forIntent(5L, 9L, 1)).isNotEqualTo(keys.forIntent(5L, 9L, 1));
        assertThat(other.forCustomer(5L)).isNotEqualTo(keys.forCustomer(5L));
        assertThat(other.forRefundOperation("rental-5:cancel-refund", 0))
                .isNotEqualTo(keys.forRefundOperation("rental-5:cancel-refund", 0));
    }

    @Test
    @DisplayName("keys stay stable across retries within one installation and read the identity once")
    void stableWithinInstallation() {
        int[] reads = {0};
        IdempotencyKeyGenerator counting = new IdempotencyKeyGenerator(new InstallationIdentity(() -> {
            reads[0]++;
            return INSTALLATION;
        }));

        String first = counting.forIntent(5L, 9L, 1);
        String retry = counting.forIntent(5L, 9L, 1);

        assertThat(retry).isEqualTo(first);
        assertThat(reads[0]).isEqualTo(1);
    }
}
