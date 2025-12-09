package com.akif.service.email;

import com.akif.dto.email.EmailMessage;
import com.akif.shared.enums.CurrencyType;
import com.akif.shared.enums.EmailType;
import com.akif.event.*;
import com.akif.exception.EmailSendException;
import com.akif.service.email.impl.EmailNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailNotificationService Tests")
class EmailNotificationServiceTest {

    @Mock
    private IEmailTemplateService templateService;

    @Mock
    private IEmailSender emailSender;

    @InjectMocks
    private EmailNotificationService emailNotificationService;

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
        @DisplayName("Should successfully send rental confirmation email")
        void shouldSuccessfullySendRentalConfirmationEmail() throws EmailSendException {
            when(templateService.renderConfirmationEmail(event))
                .thenReturn("<html>Confirmation Email</html>");

            emailNotificationService.sendRentalConfirmation(event);

            verify(templateService).renderConfirmationEmail(event);
            verify(emailSender).send(any(EmailMessage.class));
        }

        @Test
        @DisplayName("Should send email with correct subject")
        void shouldSendEmailWithCorrectSubject() throws EmailSendException {
            when(templateService.renderConfirmationEmail(event))
                .thenReturn("<html>Confirmation Email</html>");

            emailNotificationService.sendRentalConfirmation(event);

            verify(emailSender).send(argThat(message ->
                message.subject().equals("Rental Confirmation - #1")
            ));
        }

        @Test
        @DisplayName("Should send email with correct type")
        void shouldSendEmailWithCorrectType() throws EmailSendException {
            when(templateService.renderConfirmationEmail(event))
                .thenReturn("<html>Confirmation Email</html>");

            emailNotificationService.sendRentalConfirmation(event);

            verify(emailSender).send(argThat(message ->
                message.type() == EmailType.RENTAL_CONFIRMATION
            ));
        }

        @Test
        @DisplayName("Should throw EmailSendException on failure")
        void shouldThrowEmailSendExceptionOnFailure() throws EmailSendException {
            when(templateService.renderConfirmationEmail(event))
                .thenReturn("<html>Confirmation Email</html>");
            
            doThrow(new EmailSendException("customer@example.com", EmailType.RENTAL_CONFIRMATION, "550", "SMTP error", null))
                .when(emailSender).send(any(EmailMessage.class));

            assertThatThrownBy(() -> emailNotificationService.sendRentalConfirmation(event))
                .isInstanceOf(EmailSendException.class)
                .hasMessageContaining("SMTP error");

            verify(emailSender).send(any(EmailMessage.class));
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
        @DisplayName("Should successfully send payment receipt email")
        void shouldSuccessfullySendPaymentReceiptEmail() throws EmailSendException {
            when(templateService.renderPaymentReceiptEmail(event))
                .thenReturn("<html>Payment Receipt</html>");

            emailNotificationService.sendPaymentReceipt(event);

            verify(templateService).renderPaymentReceiptEmail(event);
            verify(emailSender).send(any(EmailMessage.class));
        }

        @Test
        @DisplayName("Should send email with correct subject")
        void shouldSendEmailWithCorrectSubject() throws EmailSendException {
            when(templateService.renderPaymentReceiptEmail(event))
                .thenReturn("<html>Payment Receipt</html>");

            emailNotificationService.sendPaymentReceipt(event);

            verify(emailSender).send(argThat(message ->
                message.subject().equals("Payment Receipt - Transaction #TXN-123456")
            ));
        }

        @Test
        @DisplayName("Should send email with correct type")
        void shouldSendEmailWithCorrectType() throws EmailSendException {
            when(templateService.renderPaymentReceiptEmail(event))
                .thenReturn("<html>Payment Receipt</html>");

            emailNotificationService.sendPaymentReceipt(event);

            verify(emailSender).send(argThat(message ->
                message.type() == EmailType.PAYMENT_RECEIPT
            ));
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
        @DisplayName("Should successfully send pickup reminder email")
        void shouldSuccessfullySendPickupReminderEmail() throws EmailSendException {
            when(templateService.renderPickupReminderEmail(event))
                .thenReturn("<html>Pickup Reminder</html>");

            emailNotificationService.sendPickupReminder(event);

            verify(templateService).renderPickupReminderEmail(event);
            verify(emailSender).send(any(EmailMessage.class));
        }

        @Test
        @DisplayName("Should send email with correct subject")
        void shouldSendEmailWithCorrectSubject() throws EmailSendException {
            when(templateService.renderPickupReminderEmail(event))
                .thenReturn("<html>Pickup Reminder</html>");

            emailNotificationService.sendPickupReminder(event);

            verify(emailSender).send(argThat(message ->
                message.subject().equals("Pickup Reminder - Rental #1")
            ));
        }

        @Test
        @DisplayName("Should send email with correct type")
        void shouldSendEmailWithCorrectType() throws EmailSendException {
            when(templateService.renderPickupReminderEmail(event))
                .thenReturn("<html>Pickup Reminder</html>");

            emailNotificationService.sendPickupReminder(event);

            verify(emailSender).send(argThat(message ->
                message.type() == EmailType.PICKUP_REMINDER
            ));
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
        @DisplayName("Should successfully send return reminder email")
        void shouldSuccessfullySendReturnReminderEmail() throws EmailSendException {
            when(templateService.renderReturnReminderEmail(event))
                .thenReturn("<html>Return Reminder</html>");

            emailNotificationService.sendReturnReminder(event);

            verify(templateService).renderReturnReminderEmail(event);
            verify(emailSender).send(any(EmailMessage.class));
        }

        @Test
        @DisplayName("Should send email with correct subject")
        void shouldSendEmailWithCorrectSubject() throws EmailSendException {
            when(templateService.renderReturnReminderEmail(event))
                .thenReturn("<html>Return Reminder</html>");

            emailNotificationService.sendReturnReminder(event);

            verify(emailSender).send(argThat(message ->
                message.subject().equals("Return Reminder - Rental #1")
            ));
        }

        @Test
        @DisplayName("Should send email with correct type")
        void shouldSendEmailWithCorrectType() throws EmailSendException {
            when(templateService.renderReturnReminderEmail(event))
                .thenReturn("<html>Return Reminder</html>");

            emailNotificationService.sendReturnReminder(event);

            verify(emailSender).send(argThat(message ->
                message.type() == EmailType.RETURN_REMINDER
            ));
        }
    }

    @Nested
    @DisplayName("Cancellation Confirmation Email")
    class CancellationConfirmationEmail {

        private RentalCancelledEvent event;

        @BeforeEach
        void setUp() {
            event = new RentalCancelledEvent(
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
        }

        @Test
        @DisplayName("Should successfully send cancellation confirmation email")
        void shouldSuccessfullySendCancellationConfirmationEmail() throws EmailSendException {
            when(templateService.renderCancellationEmail(event))
                .thenReturn("<html>Cancellation Confirmation</html>");

            emailNotificationService.sendCancellationConfirmation(event);

            verify(templateService).renderCancellationEmail(event);
            verify(emailSender).send(any(EmailMessage.class));
        }

        @Test
        @DisplayName("Should send email with correct subject")
        void shouldSendEmailWithCorrectSubject() throws EmailSendException {
            when(templateService.renderCancellationEmail(event))
                .thenReturn("<html>Cancellation Confirmation</html>");

            emailNotificationService.sendCancellationConfirmation(event);

            verify(emailSender).send(argThat(message ->
                message.subject().equals("Cancellation Confirmation - Rental #1")
            ));
        }

        @Test
        @DisplayName("Should send email with correct type")
        void shouldSendEmailWithCorrectType() throws EmailSendException {
            when(templateService.renderCancellationEmail(event))
                .thenReturn("<html>Cancellation Confirmation</html>");

            emailNotificationService.sendCancellationConfirmation(event);

            verify(emailSender).send(argThat(message ->
                message.type() == EmailType.CANCELLATION_CONFIRMATION
            ));
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

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
        @DisplayName("Should propagate EmailSendException when email fails")
        void shouldPropagateEmailSendExceptionWhenEmailFails() throws EmailSendException {
            when(templateService.renderConfirmationEmail(event))
                .thenReturn("<html>Confirmation Email</html>");
            
            EmailSendException exception = new EmailSendException(
                "customer@example.com",
                EmailType.RENTAL_CONFIRMATION,
                "550",
                "SMTP error",
                null
            );
            
            doThrow(exception).when(emailSender).send(any(EmailMessage.class));

            assertThatThrownBy(() -> emailNotificationService.sendRentalConfirmation(event))
                .isInstanceOf(EmailSendException.class)
                .hasMessageContaining("SMTP error");

            verify(emailSender).send(any(EmailMessage.class));
        }

        @Test
        @DisplayName("Should include correct error details in exception")
        void shouldIncludeCorrectErrorDetailsInException() throws EmailSendException {
            when(templateService.renderConfirmationEmail(event))
                .thenReturn("<html>Confirmation Email</html>");
            
            doThrow(new EmailSendException("customer@example.com", EmailType.RENTAL_CONFIRMATION, "421", "Temporary error", null))
                .when(emailSender).send(any(EmailMessage.class));

            assertThatThrownBy(() -> emailNotificationService.sendRentalConfirmation(event))
                .isInstanceOf(EmailSendException.class)
                .satisfies(ex -> {
                    EmailSendException emailEx = (EmailSendException) ex;
                    assertThat(emailEx.getRecipient()).isEqualTo("customer@example.com");
                    assertThat(emailEx.getEmailType()).isEqualTo(EmailType.RENTAL_CONFIRMATION);
                    assertThat(emailEx.getSmtpErrorCode()).isEqualTo("421");
                });

            verify(emailSender).send(any(EmailMessage.class));
        }
    }
}
