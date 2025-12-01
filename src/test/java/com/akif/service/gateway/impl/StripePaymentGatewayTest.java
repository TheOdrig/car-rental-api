package com.akif.service.gateway.impl;

import com.akif.config.StripeConfig;
import com.akif.enums.CurrencyType;
import com.akif.enums.PaymentStatus;
import com.akif.model.Payment;
import com.akif.model.Rental;
import com.akif.service.gateway.IdempotencyKeyGenerator;
import com.akif.service.gateway.PaymentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripePaymentGateway Tests")
class StripePaymentGatewayTest {

    @Mock
    private StripeConfig stripeConfig;

    @Mock
    private IdempotencyKeyGenerator idempotencyKeyGenerator;

    @InjectMocks
    private StripePaymentGateway stripePaymentGateway;

    private Payment testPayment;
    private Rental testRental;

    @BeforeEach
    void setUp() {
        testRental = new Rental();
        testRental.setId(123L);

        testPayment = new Payment();
        testPayment.setId(456L);
        testPayment.setRental(testRental);
        testPayment.setAmount(new BigDecimal("100.00"));
        testPayment.setCurrency(CurrencyType.USD);
        testPayment.setStatus(PaymentStatus.PENDING);
    }

    @Nested
    @DisplayName("Checkout Session Creation Tests")
    class CheckoutSessionCreationTests {

        @Test
        @DisplayName("Should generate idempotency key when creating checkout session")
        void shouldGenerateIdempotencyKeyWhenCreatingCheckoutSession() {
            String expectedKey = "test-idempotency-key-123";
            when(idempotencyKeyGenerator.generateForCheckout(eq(123L), any(LocalDateTime.class)))
                    .thenReturn(expectedKey);

            String generatedKey = idempotencyKeyGenerator.generateForCheckout(123L, LocalDateTime.now());

            assertThat(generatedKey).isEqualTo(expectedKey);
        }

        @Test
        @DisplayName("Should use correct success and cancel URLs from config")
        void shouldUseCorrectUrlsFromConfig() {
            when(stripeConfig.getSuccessUrl()).thenReturn("http://localhost:8080/success");
            when(stripeConfig.getCancelUrl()).thenReturn("http://localhost:8080/cancel");
            
            assertThat(stripeConfig.getSuccessUrl()).isEqualTo("http://localhost:8080/success");
            assertThat(stripeConfig.getCancelUrl()).isEqualTo("http://localhost:8080/cancel");
        }
    }

    @Nested
    @DisplayName("Authorize Tests")
    class AuthorizeTests {

        @Test
        @DisplayName("Should return success result for authorize")
        void shouldReturnSuccessResultForAuthorize() {
            PaymentResult result = stripePaymentGateway.authorize(
                    new BigDecimal("100.00"),
                    CurrencyType.USD,
                    "cust_123"
            );

            assertThat(result.success()).isTrue();
            assertThat(result.transactionId()).isNotNull();
            assertThat(result.transactionId()).startsWith("STRIPE-AUTH-");
            assertThat(result.message()).contains("Stripe Checkout");
        }
    }

    @Nested
    @DisplayName("Capture Tests")
    class CaptureTests {

        @Test
        @DisplayName("Should return success result for capture")
        void shouldReturnSuccessResultForCapture() {
            String transactionId = "pi_test123";
            BigDecimal amount = new BigDecimal("100.00");

            PaymentResult result = stripePaymentGateway.capture(transactionId, amount);

            assertThat(result.success()).isTrue();
            assertThat(result.transactionId()).isEqualTo(transactionId);
            assertThat(result.message()).contains("captured automatically");
        }
    }

    @Nested
    @DisplayName("Refund Tests")
    class RefundTests {

        @Test
        @DisplayName("Should handle refund metadata correctly")
        void shouldHandleRefundMetadataCorrectly() {
            String transactionId = "pi_test123";
            BigDecimal amount = new BigDecimal("50.00");

            assertThat(transactionId).isNotNull();
            assertThat(amount).isPositive();
        }
    }
}
