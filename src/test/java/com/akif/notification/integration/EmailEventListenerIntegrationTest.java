package com.akif.notification.integration;

import com.akif.notification.domain.EmailType;
import com.akif.notification.internal.dto.EmailMessage;
import com.akif.notification.internal.service.email.IEmailSender;
import com.akif.payment.api.PaymentCapturedEvent;
import com.akif.rental.api.PickupReminderEvent;
import com.akif.rental.api.RentalCancelledEvent;
import com.akif.rental.api.RentalConfirmedEvent;
import com.akif.rental.api.ReturnReminderEvent;
import com.akif.shared.enums.CurrencyType;
import com.akif.starter.CarGalleryProjectApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("EmailEventListener Integration Tests")
class EmailEventListenerIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @MockitoSpyBean
    private IEmailSender emailSender;

    @Nested
    @DisplayName("RentalConfirmedEvent Tests")
    class RentalConfirmedEventTests {

        @Test
        @DisplayName("Should send rental confirmation email when RentalConfirmedEvent is published")
        void shouldSendRentalConfirmationEmail() {
            RentalConfirmedEvent event = new RentalConfirmedEvent(
                    this,
                    1L,
                    "customer@example.com",
                    LocalDateTime.now(),
                    "Toyota",
                    "Corolla",
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(5),
                    new BigDecimal("2000.00"),
                    CurrencyType.TRY,
                    "Istanbul Airport"
            );

            eventPublisher.publishEvent(event);

            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                ArgumentCaptor<EmailMessage> captor =
                    ArgumentCaptor.forClass(EmailMessage.class);
                verify(emailSender, times(1)).send(captor.capture());

                EmailMessage sentEmail = captor.getValue();
                assertThat(sentEmail.to()).isEqualTo("customer@example.com");
                assertThat(sentEmail.subject()).contains("Rental Confirmation");
                assertThat(sentEmail.subject()).contains("#1");
                assertThat(sentEmail.type()).isEqualTo(EmailType.RENTAL_CONFIRMATION);
                assertThat(sentEmail.referenceId()).isEqualTo(1L);
                assertThat(sentEmail.body()).contains("Toyota");
                assertThat(sentEmail.body()).contains("Corolla");
                assertThat(sentEmail.body()).contains("2000.00");
                assertThat(sentEmail.body()).contains("Istanbul Airport");
            });
        }
    }

    @Nested
    @DisplayName("PaymentCapturedEvent Tests")
    class PaymentCapturedEventTests {

        @Test
        @DisplayName("Should send payment receipt email when PaymentCapturedEvent is published")
        void shouldSendPaymentReceiptEmail() {
            PaymentCapturedEvent event = new PaymentCapturedEvent(
                    this,
                    100L,
                    1L,
                    "customer@example.com",
                    new BigDecimal("2000.00"),
                    CurrencyType.TRY,
                    "txn_123456",
                    LocalDateTime.now()
            );

            eventPublisher.publishEvent(event);

            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                ArgumentCaptor<EmailMessage> captor =
                    ArgumentCaptor.forClass(EmailMessage.class);
                verify(emailSender, atLeastOnce()).send(captor.capture());

                EmailMessage sentEmail = captor.getAllValues().stream()
                    .filter(email -> email.type() == EmailType.PAYMENT_RECEIPT)
                    .findFirst()
                    .orElseThrow();

                assertThat(sentEmail.to()).isEqualTo("customer@example.com");
                assertThat(sentEmail.subject()).contains("Payment Receipt");
                assertThat(sentEmail.type()).isEqualTo(EmailType.PAYMENT_RECEIPT);
                assertThat(sentEmail.referenceId()).isEqualTo(1L);
                assertThat(sentEmail.body()).contains("txn_123456");
                assertThat(sentEmail.body()).contains("2000.00");
            });
        }
    }

    @Nested
    @DisplayName("PickupReminderEvent Tests")
    class PickupReminderEventTests {

        @Test
        @DisplayName("Should send pickup reminder email when PickupReminderEvent is published")
        void shouldSendPickupReminderEmail() {
            PickupReminderEvent event = new PickupReminderEvent(
                    this,
                    1L,
                    "customer@example.com",
                    LocalDateTime.now(),
                    LocalDate.now().plusDays(1),
                    "Istanbul Airport",
                    "Toyota",
                    "Corolla"
            );

            eventPublisher.publishEvent(event);

            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                ArgumentCaptor<EmailMessage> captor =
                    ArgumentCaptor.forClass(EmailMessage.class);
                verify(emailSender, atLeastOnce()).send(captor.capture());

                EmailMessage sentEmail = captor.getAllValues().stream()
                    .filter(email -> email.type() == EmailType.PICKUP_REMINDER)
                    .findFirst()
                    .orElseThrow();

                assertThat(sentEmail.to()).isEqualTo("customer@example.com");
                assertThat(sentEmail.subject()).contains("Pickup Reminder");
                assertThat(sentEmail.type()).isEqualTo(EmailType.PICKUP_REMINDER);
                assertThat(sentEmail.referenceId()).isEqualTo(1L);
                assertThat(sentEmail.body()).contains("Toyota");
                assertThat(sentEmail.body()).contains("Corolla");
                assertThat(sentEmail.body()).contains("Istanbul Airport");
            });
        }
    }

    @Nested
    @DisplayName("ReturnReminderEvent Tests")
    class ReturnReminderEventTests {

        @Test
        @DisplayName("Should send return reminder email when ReturnReminderEvent is published")
        void shouldSendReturnReminderEmail() {
            ReturnReminderEvent event = new ReturnReminderEvent(
                    this,
                    1L,
                    "customer@example.com",
                    LocalDateTime.now(),
                    LocalDate.now(),
                    "Istanbul Airport",
                    new BigDecimal("500.00")
            );

            eventPublisher.publishEvent(event);

            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                ArgumentCaptor<EmailMessage> captor =
                    ArgumentCaptor.forClass(EmailMessage.class);
                verify(emailSender, atLeastOnce()).send(captor.capture());

                EmailMessage sentEmail = captor.getAllValues().stream()
                    .filter(email -> email.type() == EmailType.RETURN_REMINDER)
                    .findFirst()
                    .orElseThrow();

                assertThat(sentEmail.to()).isEqualTo("customer@example.com");
                assertThat(sentEmail.subject()).contains("Return Reminder");
                assertThat(sentEmail.type()).isEqualTo(EmailType.RETURN_REMINDER);
                assertThat(sentEmail.referenceId()).isEqualTo(1L);
                assertThat(sentEmail.body()).contains("Istanbul Airport");
                assertThat(sentEmail.body()).contains("500.00");
            });
        }
    }

    @Nested
    @DisplayName("RentalCancelledEvent Tests")
    class RentalCancelledEventTests {

        @Test
        @DisplayName("Should send cancellation confirmation email when RentalCancelledEvent is published")
        void shouldSendCancellationConfirmationEmail() {
            RentalCancelledEvent event = new RentalCancelledEvent(
                    this,
                    1L,
                    "customer@example.com",
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    "Customer request",
                    true,
                    new BigDecimal("2000.00"),
                    "refund_123456"
            );

            eventPublisher.publishEvent(event);

            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                ArgumentCaptor<EmailMessage> captor =
                    ArgumentCaptor.forClass(EmailMessage.class);
                verify(emailSender, atLeastOnce()).send(captor.capture());

                EmailMessage sentEmail = captor.getAllValues().stream()
                    .filter(email -> email.type() == EmailType.CANCELLATION_CONFIRMATION)
                    .findFirst()
                    .orElseThrow();

                assertThat(sentEmail.to()).isEqualTo("customer@example.com");
                assertThat(sentEmail.subject()).contains("Cancellation Confirmation");
                assertThat(sentEmail.type()).isEqualTo(EmailType.CANCELLATION_CONFIRMATION);
                assertThat(sentEmail.referenceId()).isEqualTo(1L);
                assertThat(sentEmail.body()).contains("refund_123456");
                assertThat(sentEmail.body()).contains("2000.00");
            });
        }

        @Test
        @DisplayName("Should send cancellation email without refund details when no refund processed")
        void shouldSendCancellationEmailWithoutRefund() {
            RentalCancelledEvent event = new RentalCancelledEvent(
                    this,
                    2L,
                    "customer@example.com",
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    "Customer request",
                    false,
                    null,
                    null
            );

            eventPublisher.publishEvent(event);

            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                ArgumentCaptor<EmailMessage> captor =
                    ArgumentCaptor.forClass(EmailMessage.class);
                verify(emailSender, atLeastOnce()).send(captor.capture());

                EmailMessage sentEmail = captor.getAllValues().stream()
                    .filter(email -> email.type() == EmailType.CANCELLATION_CONFIRMATION)
                    .filter(email -> email.referenceId().equals(2L))
                    .findFirst()
                    .orElseThrow();

                assertThat(sentEmail.to()).isEqualTo("customer@example.com");
                assertThat(sentEmail.subject()).contains("Cancellation Confirmation");
                assertThat(sentEmail.type()).isEqualTo(EmailType.CANCELLATION_CONFIRMATION);
                assertThat(sentEmail.referenceId()).isEqualTo(2L);
            });
        }
    }
}
