package com.akif.payment.domain;

import com.akif.payment.api.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Payment state transitions")
class PaymentTransitionsTest {

    private Payment payment(PaymentStatus status) {
        return Payment.builder().amount(new BigDecimal("100.00")).status(status).build();
    }

    @Test
    @DisplayName("authorize only moves PENDING and FAILED payments")
    void authorizeTransitions() {
        Payment pending = payment(PaymentStatus.PENDING);
        Payment captured = payment(PaymentStatus.CAPTURED);

        assertThat(pending.authorize()).isTrue();
        assertThat(pending.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(pending.authorize()).isFalse();
        assertThat(captured.authorize()).isFalse();
        assertThat(captured.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
    }

    @Test
    @DisplayName("a late failure event never downgrades an authorized or captured payment")
    void failureDoesNotDowngrade() {
        Payment authorized = payment(PaymentStatus.AUTHORIZED);
        Payment captured = payment(PaymentStatus.CAPTURED);

        assertThat(authorized.fail("declined")).isFalse();
        assertThat(captured.fail("declined")).isFalse();
        assertThat(authorized.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(captured.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
    }

    @Test
    @DisplayName("a pending payment can fail and later be reopened for another card")
    void failThenReopen() {
        Payment payment = payment(PaymentStatus.PENDING);

        assertThat(payment.fail("declined")).isTrue();
        assertThat(payment.getFailureReason()).isEqualTo("declined");
        assertThat(payment.reopen()).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.reopen()).isFalse();
    }

    @Test
    @DisplayName("capture and cancel are terminal for each other")
    void captureAndCancelAreExclusive() {
        Payment captured = payment(PaymentStatus.AUTHORIZED);
        Payment cancelled = payment(PaymentStatus.AUTHORIZED);

        assertThat(captured.capture()).isTrue();
        assertThat(captured.cancel()).isFalse();
        assertThat(cancelled.cancel()).isTrue();
        assertThat(cancelled.capture()).isFalse();
        assertThat(cancelled.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    @DisplayName("a replayed capture event reports no transition")
    void captureIsIdempotent() {
        Payment payment = payment(PaymentStatus.AUTHORIZED);

        assertThat(payment.capture()).isTrue();
        assertThat(payment.capture()).isFalse();
    }

    @Test
    @DisplayName("applyRefundTotal is absolute and monotonic, so replays and out-of-order events cannot double count")
    void refundTotalIsMonotonic() {
        Payment payment = payment(PaymentStatus.CAPTURED);

        payment.applyRefundTotal(new BigDecimal("40.00"));
        payment.applyRefundTotal(new BigDecimal("40.00"));
        payment.applyRefundTotal(new BigDecimal("25.00"));

        assertThat(payment.getRefundedAmount()).isEqualByComparingTo("40.00");
        assertThat(payment.refundableBalance()).isEqualByComparingTo("60.00");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);

        payment.applyRefundTotal(new BigDecimal("100.00"));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("recordRefundIssued keeps the larger of the webhook total and the local total and counts the refund")
    void refundIssuedAfterWebhook() {
        Payment payment = payment(PaymentStatus.CAPTURED);
        payment.applyRefundTotal(new BigDecimal("30.00"));

        payment.recordRefundIssued(BigDecimal.ZERO, new BigDecimal("30.00"));

        assertThat(payment.getRefundedAmount()).isEqualByComparingTo("30.00");
        assertThat(payment.getRefundCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("restart moves to a new attempt and clears the intent so a fresh idempotency key is used")
    void restartStartsNewAttempt() {
        Payment payment = payment(PaymentStatus.AUTHORIZED);
        payment.attachIntent("pi_old", "key-1");
        payment.cancel();

        payment.restart();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getIntentAttempt()).isEqualTo(2);
        assertThat(payment.getStripePaymentIntentId()).isNull();
    }

    @Test
    @DisplayName("a new payment starts as a PENDING RENTAL payment on attempt 1")
    void defaults() {
        Payment payment = Payment.builder().amount(BigDecimal.TEN).build();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getKind()).isEqualTo(com.akif.payment.domain.enums.PaymentKind.RENTAL);
        assertThat(payment.getIntentAttempt()).isEqualTo(1);
        assertThat(payment.getRefundCount()).isZero();
    }
}
