package com.akif.service.webhook;

import com.akif.config.StripeConfig;
import com.akif.shared.enums.PaymentStatus;
import com.akif.shared.enums.WebhookEventStatus;
import com.akif.model.Payment;
import com.akif.model.Rental;
import com.akif.model.WebhookEvent;
import com.akif.repository.PaymentRepository;
import com.akif.repository.WebhookEventRepository;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeWebhookHandler Tests")
class StripeWebhookHandlerTest {

    @Mock
    private StripeConfig stripeConfig;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @InjectMocks
    private StripeWebhookHandler webhookHandler;

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
        testPayment.setStatus(PaymentStatus.PENDING);
        testPayment.setStripeSessionId("cs_test_123");
    }

    @Nested
    @DisplayName("Duplicate Event Detection Tests")
    class DuplicateEventDetectionTests {

        @Test
        @DisplayName("Should detect already processed event")
        void shouldDetectAlreadyProcessedEvent() {
            String eventId = "evt_test_123";
            WebhookEvent existingEvent = WebhookEvent.builder()
                    .eventId(eventId)
                    .status(WebhookEventStatus.PROCESSED)
                    .build();

            when(webhookEventRepository.findByEventId(eventId))
                    .thenReturn(Optional.of(existingEvent));

            boolean result = webhookHandler.isEventAlreadyProcessed(eventId);

            assertThat(result).isTrue();
            verify(webhookEventRepository).save(any(WebhookEvent.class));
        }

        @Test
        @DisplayName("Should detect event currently processing")
        void shouldDetectEventCurrentlyProcessing() {
            String eventId = "evt_test_123";
            WebhookEvent existingEvent = WebhookEvent.builder()
                    .eventId(eventId)
                    .status(WebhookEventStatus.PROCESSING)
                    .build();

            when(webhookEventRepository.findByEventId(eventId))
                    .thenReturn(Optional.of(existingEvent));

            boolean result = webhookHandler.isEventAlreadyProcessed(eventId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should not detect new event as duplicate")
        void shouldNotDetectNewEventAsDuplicate() {
            String eventId = "evt_test_123";

            when(webhookEventRepository.findByEventId(eventId))
                    .thenReturn(Optional.empty());

            boolean result = webhookHandler.isEventAlreadyProcessed(eventId);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should allow retry of failed event")
        void shouldAllowRetryOfFailedEvent() {
            String eventId = "evt_test_123";
            WebhookEvent existingEvent = WebhookEvent.builder()
                    .eventId(eventId)
                    .status(WebhookEventStatus.FAILED)
                    .build();

            when(webhookEventRepository.findByEventId(eventId))
                    .thenReturn(Optional.of(existingEvent));

            boolean result = webhookHandler.isEventAlreadyProcessed(eventId);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Checkout Session Completed Tests")
    class CheckoutSessionCompletedTests {

        @Test
        @DisplayName("Should update payment status to CAPTURED")
        void shouldUpdatePaymentStatusToCaptured() {
            Session session = mock(Session.class);
            when(session.getId()).thenReturn("cs_test_123");
            when(session.getPaymentIntent()).thenReturn("pi_test_456");

            List<Payment> payments = new ArrayList<>();
            payments.add(testPayment);
            when(paymentRepository.findAll()).thenReturn(payments);

            webhookHandler.processCheckoutSessionCompleted(session);

            ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(paymentCaptor.capture());

            Payment savedPayment = paymentCaptor.getValue();
            assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
            assertThat(savedPayment.getStripePaymentIntentId()).isEqualTo("pi_test_456");
            assertThat(savedPayment.getTransactionId()).isEqualTo("pi_test_456");
        }

        @Test
        @DisplayName("Should throw exception when payment not found")
        void shouldThrowExceptionWhenPaymentNotFound() {
            Session session = mock(Session.class);
            when(session.getId()).thenReturn("cs_test_nonexistent");

            when(paymentRepository.findAll()).thenReturn(new ArrayList<>());

            assertThatThrownBy(() -> webhookHandler.processCheckoutSessionCompleted(session))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Payment not found");
        }
    }

    @Nested
    @DisplayName("Checkout Session Expired Tests")
    class CheckoutSessionExpiredTests {

        @Test
        @DisplayName("Should update payment status to FAILED")
        void shouldUpdatePaymentStatusToFailed() {
            Session session = mock(Session.class);
            when(session.getId()).thenReturn("cs_test_123");

            List<Payment> payments = new ArrayList<>();
            payments.add(testPayment);
            when(paymentRepository.findAll()).thenReturn(payments);

            webhookHandler.processCheckoutSessionExpired(session);

            ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(paymentCaptor.capture());

            Payment savedPayment = paymentCaptor.getValue();
            assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(savedPayment.getFailureReason()).isEqualTo("Checkout session expired");
        }

        @Test
        @DisplayName("Should throw exception when payment not found")
        void shouldThrowExceptionWhenPaymentNotFound() {
            Session session = mock(Session.class);
            when(session.getId()).thenReturn("cs_test_nonexistent");

            when(paymentRepository.findAll()).thenReturn(new ArrayList<>());

            assertThatThrownBy(() -> webhookHandler.processCheckoutSessionExpired(session))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Payment not found");
        }
    }

    @Nested
    @DisplayName("Payment Intent Failed Tests")
    class PaymentIntentFailedTests {

        @Test
        @DisplayName("Should update payment status to FAILED without error details")
        void shouldUpdatePaymentStatusToFailedWithoutErrorDetails() {
            testPayment.setStripePaymentIntentId("pi_test_456");

            PaymentIntent paymentIntent = mock(PaymentIntent.class);
            when(paymentIntent.getId()).thenReturn("pi_test_456");
            when(paymentIntent.getLastPaymentError()).thenReturn(null);

            List<Payment> payments = new ArrayList<>();
            payments.add(testPayment);
            when(paymentRepository.findAll()).thenReturn(payments);

            webhookHandler.processPaymentIntentFailed(paymentIntent);

            ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(paymentCaptor.capture());

            Payment savedPayment = paymentCaptor.getValue();
            assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(savedPayment.getFailureReason()).isEqualTo("Payment failed");
        }

        @Test
        @DisplayName("Should throw exception when payment not found")
        void shouldThrowExceptionWhenPaymentNotFound() {
            PaymentIntent paymentIntent = mock(PaymentIntent.class);
            when(paymentIntent.getId()).thenReturn("pi_test_nonexistent");

            when(paymentRepository.findAll()).thenReturn(new ArrayList<>());

            assertThatThrownBy(() -> webhookHandler.processPaymentIntentFailed(paymentIntent))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Payment not found");
        }
    }
}
