package com.akif.payment.unit.gateway.impl;

import com.akif.payment.api.PaymentResult;
import com.akif.payment.internal.service.gateway.impl.StubPaymentGateway;
import com.akif.shared.enums.CurrencyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StubPaymentGateway Tests")
class StubPaymentGatewayTest {

    private StubPaymentGateway stubPaymentGateway;

    @BeforeEach
    void setUp() {
        stubPaymentGateway = new StubPaymentGateway();
    }

    @Nested
    @DisplayName("Authorize Tests")
    class AuthorizeTests {

        @Test
        @DisplayName("Should successfully authorize payment")
        void shouldSuccessfullyAuthorizePayment() {
            BigDecimal amount = new BigDecimal("1000.00");
            CurrencyType currency = CurrencyType.TRY;
            String customerId = "123";

            PaymentResult result = stubPaymentGateway.authorize(amount, currency, customerId);

            assertThat(result.success()).isTrue();
            assertThat(result.transactionId()).isNotNull();
            assertThat(result.transactionId()).startsWith("STUB-");
            assertThat(result.message()).contains("authorized");
        }

        @Test
        @DisplayName("Should generate unique transaction IDs")
        void shouldGenerateUniqueTransactionIds() {
            PaymentResult result1 = stubPaymentGateway.authorize(
                    new BigDecimal("100"), CurrencyType.USD, "1");
            PaymentResult result2 = stubPaymentGateway.authorize(
                    new BigDecimal("200"), CurrencyType.EUR, "2");

            assertThat(result1.transactionId()).isNotEqualTo(result2.transactionId());
        }

        @Test
        @DisplayName("Should handle different currencies")
        void shouldHandleDifferentCurrencies() {
            PaymentResult resultTry = stubPaymentGateway.authorize(
                    new BigDecimal("1000"), CurrencyType.TRY, "1");
            PaymentResult resultUsd = stubPaymentGateway.authorize(
                    new BigDecimal("100"), CurrencyType.USD, "2");

            assertThat(resultTry.success()).isTrue();
            assertThat(resultUsd.success()).isTrue();
        }

        @Test
        @DisplayName("Should handle null customer ID")
        void shouldHandleNullCustomerId() {
            PaymentResult result = stubPaymentGateway.authorize(
                    new BigDecimal("100"), CurrencyType.USD, null);

            assertThat(result.success()).isTrue();
        }
    }

    @Nested
    @DisplayName("Capture Tests")
    class CaptureTests {

        @Test
        @DisplayName("Should successfully capture payment")
        void shouldSuccessfullyCapturePayment() {
            String transactionId = "STUB-ABC123DEF456";
            BigDecimal amount = new BigDecimal("1000.00");

            PaymentResult result = stubPaymentGateway.capture(transactionId, amount);

            assertThat(result.success()).isTrue();
            assertThat(result.transactionId()).isEqualTo(transactionId);
            assertThat(result.message()).contains("captured");
        }

        @Test
        @DisplayName("Should handle partial capture")
        void shouldHandlePartialCapture() {
            String transactionId = "STUB-ABC123DEF456";
            BigDecimal authorizedAmount = new BigDecimal("1000.00");
            BigDecimal captureAmount = new BigDecimal("500.00");

            assertThat(captureAmount).isLessThan(authorizedAmount);

            PaymentResult result = stubPaymentGateway.capture(transactionId, captureAmount);

            assertThat(result.success()).isTrue();
            assertThat(result.transactionId()).isEqualTo(transactionId);
            assertThat(result.message()).contains("captured");
        }
    }

    @Nested
    @DisplayName("Refund Tests")
    class RefundTests {

        @Test
        @DisplayName("Should successfully refund payment")
        void shouldSuccessfullyRefundPayment() {
            String transactionId = "STUB-ABC123DEF456";
            BigDecimal amount = new BigDecimal("1000.00");

            PaymentResult result = stubPaymentGateway.refund(transactionId, amount);

            assertThat(result.success()).isTrue();
            assertThat(result.transactionId()).isNotNull();
            assertThat(result.transactionId()).startsWith("STUB-");
            assertThat(result.message()).contains("refunded");
        }

        @Test
        @DisplayName("Should handle partial refund")
        void shouldHandlePartialRefund() {
            String transactionId = "STUB-ABC123DEF456";
            BigDecimal capturedAmount = new BigDecimal("1000.00");
            BigDecimal refundAmount = new BigDecimal("500.00");

            assertThat(refundAmount).isLessThan(capturedAmount);

            PaymentResult result = stubPaymentGateway.refund(transactionId, refundAmount);

            assertThat(result.success()).isTrue();
            assertThat(result.transactionId()).isNotNull();
            assertThat(result.transactionId()).startsWith("STUB-");
            assertThat(result.message()).contains("refunded");
        }
    }

}
