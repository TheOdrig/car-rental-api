package com.akif.payment.domain;

import com.akif.payment.api.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Payment behaviour")
class PaymentBehaviourTest {

    private Payment payment() {
        return Payment.builder().amount(new BigDecimal("100.00")).status(PaymentStatus.AUTHORIZED).build();
    }

    @Test
    @DisplayName("applyStatus only overwrites the transaction id and failure reason that were provided")
    void applyStatusKeepsMissingValues() {
        Payment payment = payment();
        payment.applyStatus(PaymentStatus.AUTHORIZED, "tx-1", null);

        payment.applyStatus(PaymentStatus.FAILED, null, "card declined");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getTransactionId()).isEqualTo("tx-1");
        assertThat(payment.getFailureReason()).isEqualTo("card declined");
    }

    @Test
    @DisplayName("markCaptured and markFailed record the gateway outcome")
    void gatewayOutcomeIsRecorded() {
        Payment payment = payment();

        payment.markCaptured("tx-9", "approved");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(payment.getTransactionId()).isEqualTo("tx-9");
        assertThat(payment.getGatewayResponse()).isEqualTo("approved");

        payment.markFailed("insufficient funds", "declined");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo("insufficient funds");
        assertThat(payment.getGatewayResponse()).isEqualTo("declined");
    }

    @Test
    @DisplayName("recordRefund accumulates and flips the status to REFUNDED only when fully refunded")
    void recordRefundAccumulates() {
        Payment payment = payment();
        payment.markCaptured("tx-9", "approved");

        payment.recordRefund(new BigDecimal("40.00"));
        assertThat(payment.getRefundedAmount()).isEqualByComparingTo("40.00");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);

        payment.recordRefund(new BigDecimal("60.00"));
        assertThat(payment.getRefundedAmount()).isEqualByComparingTo("100.00");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("markCheckoutCompleted captures the payment with the Stripe payment intent")
    void checkoutCompletedCapturesPayment() {
        Payment payment = payment();

        payment.markCheckoutCompleted("pi_123");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(payment.getStripePaymentIntentId()).isEqualTo("pi_123");
        assertThat(payment.getTransactionId()).isEqualTo("pi_123");
    }
}
