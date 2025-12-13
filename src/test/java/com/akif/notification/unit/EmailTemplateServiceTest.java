package com.akif.notification.unit;

import com.akif.notification.internal.service.email.impl.EmailTemplateService;
import com.akif.payment.api.PaymentCapturedEvent;
import com.akif.rental.api.PickupReminderEvent;
import com.akif.rental.api.RentalCancelledEvent;
import com.akif.rental.api.RentalConfirmedEvent;
import com.akif.rental.api.ReturnReminderEvent;
import com.akif.shared.enums.CurrencyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailTemplateService Tests")
class EmailTemplateServiceTest {

    @Mock
    private SpringTemplateEngine templateEngine;

    @InjectMocks
    private EmailTemplateService emailTemplateService;

    @Nested
    @DisplayName("Rental Confirmation Email")
    class RentalConfirmationEmail {

        private RentalConfirmedEvent event;

        @BeforeEach
        void setUp() {
            event = new RentalConfirmedEvent(
                this,
                1L,
                "customer@example.com",
                LocalDateTime.now(),
                "Toyota",
                "Corolla",
                LocalDate.of(2025, 12, 10),
                LocalDate.of(2025, 12, 15),
                new BigDecimal("2500.00"),
                CurrencyType.TRY,
                "Istanbul Airport"
            );
        }

        @Test
        @DisplayName("Should render confirmation email with correct template")
        void shouldRenderConfirmationEmailWithCorrectTemplate() {
            when(templateEngine.process(eq("email/rental-confirmation"), any(Context.class)))
                .thenReturn("<html>Confirmation Email</html>");

            String result = emailTemplateService.renderConfirmationEmail(event);

            assertThat(result).isNotNull();
            verify(templateEngine).process(eq("email/rental-confirmation"), any(Context.class));
        }

        @Test
        @DisplayName("Should include rental ID in template context")
        void shouldIncludeRentalIdInTemplateContext() {
            when(templateEngine.process(eq("email/rental-confirmation"), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    assertThat(context.getVariable("rentalId")).isEqualTo(1L);
                    return "<html>Email</html>";
                });

            emailTemplateService.renderConfirmationEmail(event);

            verify(templateEngine).process(eq("email/rental-confirmation"), any(Context.class));
        }

        @Test
        @DisplayName("Should include car details in template context")
        void shouldIncludeCarDetailsInTemplateContext() {
            when(templateEngine.process(eq("email/rental-confirmation"), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    assertThat(context.getVariable("carBrand")).isEqualTo("Toyota");
                    assertThat(context.getVariable("carModel")).isEqualTo("Corolla");
                    return "<html>Email</html>";
                });

            emailTemplateService.renderConfirmationEmail(event);

            verify(templateEngine).process(eq("email/rental-confirmation"), any(Context.class));
        }

        @Test
        @DisplayName("Should include dates in template context")
        void shouldIncludeDatesInTemplateContext() {
            when(templateEngine.process(eq("email/rental-confirmation"), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    assertThat(context.getVariable("pickupDate")).isNotNull();
                    assertThat(context.getVariable("returnDate")).isNotNull();
                    return "<html>Email</html>";
                });

            emailTemplateService.renderConfirmationEmail(event);

            verify(templateEngine).process(eq("email/rental-confirmation"), any(Context.class));
        }

        @Test
        @DisplayName("Should include total price and currency in template context")
        void shouldIncludeTotalPriceAndCurrencyInTemplateContext() {
            when(templateEngine.process(eq("email/rental-confirmation"), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    assertThat(context.getVariable("totalPrice")).isEqualTo(new BigDecimal("2500.00"));
                    assertThat(context.getVariable("currency")).isEqualTo("TRY");
                    return "<html>Email</html>";
                });

            emailTemplateService.renderConfirmationEmail(event);

            verify(templateEngine).process(eq("email/rental-confirmation"), any(Context.class));
        }

        @Test
        @DisplayName("Should include pickup location in template context")
        void shouldIncludePickupLocationInTemplateContext() {
            when(templateEngine.process(eq("email/rental-confirmation"), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    assertThat(context.getVariable("pickupLocation")).isEqualTo("Istanbul Airport");
                    return "<html>Email</html>";
                });

            emailTemplateService.renderConfirmationEmail(event);

            verify(templateEngine).process(eq("email/rental-confirmation"), any(Context.class));
        }
    }

    @Nested
    @DisplayName("Payment Receipt Email")
    class PaymentReceiptEmail {

        private PaymentCapturedEvent event;

        @BeforeEach
        void setUp() {
            event = new PaymentCapturedEvent(
                this,
                100L,
                1L,
                "customer@example.com",
                new BigDecimal("2500.00"),
                CurrencyType.TRY,
                "TXN-123456",
                LocalDateTime.of(2025, 12, 5, 14, 30)
            );
        }

        @Test
        @DisplayName("Should render payment receipt email with correct template")
        void shouldRenderPaymentReceiptEmailWithCorrectTemplate() {
            when(templateEngine.process(eq("email/payment-receipt"), any(Context.class)))
                .thenReturn("<html>Payment Receipt</html>");

            String result = emailTemplateService.renderPaymentReceiptEmail(event);

            assertThat(result).isNotNull();
            verify(templateEngine).process(eq("email/payment-receipt"), any(Context.class));
        }

        @Test
        @DisplayName("Should include transaction ID in template context")
        void shouldIncludeTransactionIdInTemplateContext() {
            when(templateEngine.process(eq("email/payment-receipt"), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    assertThat(context.getVariable("transactionId")).isEqualTo("TXN-123456");
                    return "<html>Email</html>";
                });

            emailTemplateService.renderPaymentReceiptEmail(event);

            verify(templateEngine).process(eq("email/payment-receipt"), any(Context.class));
        }

        @Test
        @DisplayName("Should include amount and currency in template context")
        void shouldIncludeAmountAndCurrencyInTemplateContext() {
            when(templateEngine.process(eq("email/payment-receipt"), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    assertThat(context.getVariable("amount")).isEqualTo(new BigDecimal("2500.00"));
                    assertThat(context.getVariable("currency")).isEqualTo("TRY");
                    return "<html>Email</html>";
                });

            emailTemplateService.renderPaymentReceiptEmail(event);

            verify(templateEngine).process(eq("email/payment-receipt"), any(Context.class));
        }

        @Test
        @DisplayName("Should include payment date in template context")
        void shouldIncludePaymentDateInTemplateContext() {
            when(templateEngine.process(eq("email/payment-receipt"), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    assertThat(context.getVariable("paymentDate")).isNotNull();
                    return "<html>Email</html>";
                });

            emailTemplateService.renderPaymentReceiptEmail(event);

            verify(templateEngine).process(eq("email/payment-receipt"), any(Context.class));
        }

        @Test
        @DisplayName("Should include rental reference in template context")
        void shouldIncludeRentalReferenceInTemplateContext() {
            when(templateEngine.process(eq("email/payment-receipt"), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    assertThat(context.getVariable("rentalId")).isEqualTo(1L);
                    return "<html>Email</html>";
                });

            emailTemplateService.renderPaymentReceiptEmail(event);

            verify(templateEngine).process(eq("email/payment-receipt"), any(Context.class));
        }
    }

    @Nested
    @DisplayName("Pickup Reminder Email")
    class PickupReminderEmail {

        private PickupReminderEvent event;

        @BeforeEach
        void setUp() {
            event = new PickupReminderEvent(
                this,
                1L,
                "customer@example.com",
                LocalDateTime.now(),
                LocalDate.of(2025, 12, 10),
                "Istanbul Airport",
                "Toyota",
                "Corolla"
            );
        }

        @Test
        @DisplayName("Should render pickup reminder email with correct template")
        void shouldRenderPickupReminderEmailWithCorrectTemplate() {
            when(templateEngine.process(eq("email/pickup-reminder"), any(Context.class)))
                .thenReturn("<html>Pickup Reminder</html>");

            String result = emailTemplateService.renderPickupReminderEmail(event);

            assertThat(result).isNotNull();
            verify(templateEngine).process(eq("email/pickup-reminder"), any(Context.class));
        }

        @Test
        @DisplayName("Should include pickup date in template context")
        void shouldIncludePickupDateInTemplateContext() {
            when(templateEngine.process(eq("email/pickup-reminder"), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    assertThat(context.getVariable("pickupDate")).isNotNull();
                    return "<html>Email</html>";
                });

            emailTemplateService.renderPickupReminderEmail(event);

            verify(templateEngine).process(eq("email/pickup-reminder"), any(Context.class));
        }

        @Test
        @DisplayName("Should include pickup location in template context")
        void shouldIncludePickupLocationInTemplateContext() {
            when(templateEngine.process(eq("email/pickup-reminder"), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    assertThat(context.getVariable("pickupLocation")).isEqualTo("Istanbul Airport");
                    return "<html>Email</html>";
                });

            emailTemplateService.renderPickupReminderEmail(event);

            verify(templateEngine).process(eq("email/pickup-reminder"), any(Context.class));
        }

        @Test
        @DisplayName("Should include car details in template context")
        void shouldIncludeCarDetailsInTemplateContext() {
            when(templateEngine.process(eq("email/pickup-reminder"), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    assertThat(context.getVariable("carBrand")).isEqualTo("Toyota");
                    assertThat(context.getVariable("carModel")).isEqualTo("Corolla");
                    return "<html>Email</html>";
                });

            emailTemplateService.renderPickupReminderEmail(event);

            verify(templateEngine).process(eq("email/pickup-reminder"), any(Context.class));
        }

        @Test
        @DisplayName("Should include rental ID in template context")
        void shouldIncludeRentalIdInTemplateContext() {
            when(templateEngine.process(eq("email/pickup-reminder"), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    assertThat(context.getVariable("rentalId")).isEqualTo(1L);
                    return "<html>Email</html>";
                });

            emailTemplateService.renderPickupReminderEmail(event);

            verify(templateEngine).process(eq("email/pickup-reminder"), any(Context.class));
        }
    }

    @Nested
    @DisplayName("Return Reminder Email")
    class ReturnReminderEmail {

        private ReturnReminderEvent event;

        @BeforeEach
        void setUp() {
            event = new ReturnReminderEvent(
                this,
                1L,
                "customer@example.com",
                LocalDateTime.now(),
                LocalDate.of(2025, 12, 15),
                "Istanbul Airport",
                new BigDecimal("100.00")
            );
        }

        @Test
        @DisplayName("Should render return reminder email with correct template")
        void shouldRenderReturnReminderEmailWithCorrectTemplate() {
            when(templateEngine.process(eq("email/return-reminder"), any(Context.class)))
                .thenReturn("<html>Return Reminder</html>");

            String result = emailTemplateService.renderReturnReminderEmail(event);

            assertThat(result).isNotNull();
            verify(templateEngine).process(eq("email/return-reminder"), any(Context.class));
        }

        @Test
        @DisplayName("Should include return date in template context")
        void shouldIncludeReturnDateInTemplateContext() {
            when(templateEngine.process(eq("email/return-reminder"), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    assertThat(context.getVariable("returnDate")).isNotNull();
                    return "<html>Email</html>";
                });

            emailTemplateService.renderReturnReminderEmail(event);

            verify(templateEngine).process(eq("email/return-reminder"), any(Context.class));
        }

        @Test
        @DisplayName("Should include return location in template context")
        void shouldIncludeReturnLocationInTemplateContext() {
            when(templateEngine.process(eq("email/return-reminder"), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    assertThat(context.getVariable("returnLocation")).isEqualTo("Istanbul Airport");
                    return "<html>Email</html>";
                });

            emailTemplateService.renderReturnReminderEmail(event);

            verify(templateEngine).process(eq("email/return-reminder"), any(Context.class));
        }

        @Test
        @DisplayName("Should include late penalty information in template context")
        void shouldIncludeLatePenaltyInformationInTemplateContext() {
            when(templateEngine.process(eq("email/return-reminder"), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    assertThat(context.getVariable("dailyPenaltyRate")).isEqualTo(new BigDecimal("100.00"));
                    return "<html>Email</html>";
                });

            emailTemplateService.renderReturnReminderEmail(event);

            verify(templateEngine).process(eq("email/return-reminder"), any(Context.class));
        }

        @Test
        @DisplayName("Should include rental ID in template context")
        void shouldIncludeRentalIdInTemplateContext() {
            when(templateEngine.process(eq("email/return-reminder"), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    assertThat(context.getVariable("rentalId")).isEqualTo(1L);
                    return "<html>Email</html>";
                });

            emailTemplateService.renderReturnReminderEmail(event);

            verify(templateEngine).process(eq("email/return-reminder"), any(Context.class));
        }
    }

    @Nested
    @DisplayName("Cancellation Confirmation Email")
    class CancellationConfirmationEmail {

        private RentalCancelledEvent eventWithRefund;
        private RentalCancelledEvent eventWithoutRefund;

        @BeforeEach
        void setUp() {
            eventWithRefund = new RentalCancelledEvent(
                this,
                1L,
                "customer@example.com",
                LocalDateTime.now(),
                LocalDateTime.of(2025, 12, 5, 10, 0),
                "Customer request",
                true,
                new BigDecimal("2500.00"),
                "REFUND-123456"
            );

            eventWithoutRefund = new RentalCancelledEvent(
                this,
                2L,
                "customer@example.com",
                LocalDateTime.now(),
                LocalDateTime.of(2025, 12, 5, 10, 0),
                "Customer request",
                false,
                null,
                null
            );
        }

        @Test
        @DisplayName("Should render cancellation email with correct template")
        void shouldRenderCancellationEmailWithCorrectTemplate() {
            when(templateEngine.process(eq("email/cancellation-confirmation"), any(Context.class)))
                .thenReturn("<html>Cancellation Confirmation</html>");

            String result = emailTemplateService.renderCancellationEmail(eventWithRefund);

            assertThat(result).isNotNull();
            verify(templateEngine).process(eq("email/cancellation-confirmation"), any(Context.class));
        }

        @Test
        @DisplayName("Should include rental ID in template context")
        void shouldIncludeRentalIdInTemplateContext() {
            when(templateEngine.process(eq("email/cancellation-confirmation"), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    assertThat(context.getVariable("rentalId")).isEqualTo(1L);
                    return "<html>Email</html>";
                });

            emailTemplateService.renderCancellationEmail(eventWithRefund);

            verify(templateEngine).process(eq("email/cancellation-confirmation"), any(Context.class));
        }

        @Test
        @DisplayName("Should include cancellation date in template context")
        void shouldIncludeCancellationDateInTemplateContext() {
            when(templateEngine.process(eq("email/cancellation-confirmation"), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    assertThat(context.getVariable("cancellationDate")).isNotNull();
                    return "<html>Email</html>";
                });

            emailTemplateService.renderCancellationEmail(eventWithRefund);

            verify(templateEngine).process(eq("email/cancellation-confirmation"), any(Context.class));
        }

        @Test
        @DisplayName("Should include refund details when refund is processed")
        void shouldIncludeRefundDetailsWhenRefundIsProcessed() {
            when(templateEngine.process(eq("email/cancellation-confirmation"), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    assertThat(context.getVariable("refundProcessed")).isEqualTo(true);
                    assertThat(context.getVariable("refundAmount")).isEqualTo(new BigDecimal("2500.00"));
                    assertThat(context.getVariable("refundTransactionId")).isEqualTo("REFUND-123456");
                    return "<html>Email</html>";
                });

            emailTemplateService.renderCancellationEmail(eventWithRefund);

            verify(templateEngine).process(eq("email/cancellation-confirmation"), any(Context.class));
        }

        @Test
        @DisplayName("Should handle no refund scenario")
        void shouldHandleNoRefundScenario() {
            when(templateEngine.process(eq("email/cancellation-confirmation"), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    assertThat(context.getVariable("refundProcessed")).isEqualTo(false);
                    assertThat(context.getVariable("refundAmount")).isNull();
                    assertThat(context.getVariable("refundTransactionId")).isNull();
                    return "<html>Email</html>";
                });

            emailTemplateService.renderCancellationEmail(eventWithoutRefund);

            verify(templateEngine).process(eq("email/cancellation-confirmation"), any(Context.class));
        }
    }
}
