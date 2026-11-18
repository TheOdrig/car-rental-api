package com.akif.notification.internal.service;

import java.time.Clock;

import com.akif.damage.api.*;
import com.akif.notification.internal.exception.EmailSendException;
import com.akif.notification.EmailNotificationService;
import com.akif.notification.internal.dto.EmailMessage;
import com.akif.notification.internal.service.email.IEmailSender;
import com.akif.notification.internal.service.email.IEmailTemplateService;
import com.akif.payment.api.PaymentCapturedEvent;
import com.akif.rental.api.*;
import com.akif.notification.domain.EmailType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationServiceImpl implements EmailNotificationService {

    private final Clock clock;

    private final IEmailTemplateService templateService;
    private final IEmailSender emailSender;

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendRentalConfirmation(RentalConfirmedEvent event) {
        log.info("Queuing rental confirmation email. RentalId: {}, Recipient: {}",
                event.rentalId(), event.customerEmail());

        String body = templateService.renderConfirmationEmail(event);
        EmailMessage message = new EmailMessage(
                event.customerEmail(),
                "Rental Confirmation - #" + event.rentalId(),
                body,
                EmailType.RENTAL_CONFIRMATION,
                event.rentalId());

        try {
            emailSender.send(message);
            log.info("Rental confirmation email sent successfully. RentalId: {}, To: {}, Timestamp: {}",
                    event.rentalId(), event.customerEmail(), LocalDateTime.now(clock));
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. RentalId: {}, Error: {}, SMTP Code: {}",
                    event.rentalId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendPaymentReceipt(PaymentCapturedEvent event) {
        log.info("Queuing payment receipt email. PaymentId: {}, RentalId: {}, Recipient: {}",
                event.paymentId(), event.rentalId(), event.customerEmail());

        String body = templateService.renderPaymentReceiptEmail(event);
        EmailMessage message = new EmailMessage(
                event.customerEmail(),
                "Payment Receipt - Transaction #" + event.transactionId(),
                body,
                EmailType.PAYMENT_RECEIPT,
                event.rentalId());

        try {
            emailSender.send(message);
            log.info("Payment receipt email sent successfully. PaymentId: {}, Amount: {}, To: {}, Timestamp: {}",
                    event.paymentId(), event.amount(), event.customerEmail(), LocalDateTime.now(clock));
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. PaymentId: {}, Error: {}, SMTP Code: {}",
                    event.paymentId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendPickupReminder(PickupReminderEvent event) {
        log.info("Queuing pickup reminder email. RentalId: {}, Recipient: {}",
                event.rentalId(), event.customerEmail());

        String body = templateService.renderPickupReminderEmail(event);
        EmailMessage message = new EmailMessage(
                event.customerEmail(),
                "Pickup Reminder - Rental #" + event.rentalId(),
                body,
                EmailType.PICKUP_REMINDER,
                event.rentalId());

        try {
            emailSender.send(message);
            log.info("Pickup reminder email sent successfully. RentalId: {}, To: {}, Timestamp: {}",
                    event.rentalId(), event.customerEmail(), LocalDateTime.now(clock));
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. RentalId: {}, Error: {}, SMTP Code: {}",
                    event.rentalId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendReturnReminder(ReturnReminderEvent event) {
        log.info("Queuing return reminder email. RentalId: {}, Recipient: {}",
                event.rentalId(), event.customerEmail());

        String body = templateService.renderReturnReminderEmail(event);
        EmailMessage message = new EmailMessage(
                event.customerEmail(),
                "Return Reminder - Rental #" + event.rentalId(),
                body,
                EmailType.RETURN_REMINDER,
                event.rentalId());

        try {
            emailSender.send(message);
            log.info("Return reminder email sent successfully. RentalId: {}, To: {}, Timestamp: {}",
                    event.rentalId(), event.customerEmail(), LocalDateTime.now(clock));
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. RentalId: {}, Error: {}, SMTP Code: {}",
                    event.rentalId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendCancellationConfirmation(RentalCancelledEvent event) {
        log.info("Queuing cancellation confirmation email. RentalId: {}, Recipient: {}",
                event.rentalId(), event.customerEmail());

        String body = templateService.renderCancellationEmail(event);
        EmailMessage message = new EmailMessage(
                event.customerEmail(),
                "Cancellation Confirmation - Rental #" + event.rentalId(),
                body,
                EmailType.CANCELLATION_CONFIRMATION,
                event.rentalId());

        try {
            emailSender.send(message);
            log.info("Cancellation confirmation email sent successfully. RentalId: {}, To: {}, Timestamp: {}",
                    event.rentalId(), event.customerEmail(), LocalDateTime.now(clock));
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. RentalId: {}, Error: {}, SMTP Code: {}",
                    event.rentalId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendGracePeriodWarning(GracePeriodWarningEvent event) {
        log.info("Queuing grace period warning email. RentalId: {}, Recipient: {}",
                event.rentalId(), event.customerEmail());

        String body = templateService.renderGracePeriodWarningEmail(event);
        EmailMessage message = new EmailMessage(
                event.customerEmail(),
                "⚠️ Grace Period Warning - Rental #" + event.rentalId(),
                body,
                EmailType.RETURN_REMINDER,
                event.rentalId());

        try {
            emailSender.send(message);
            log.info("Grace period warning email sent successfully. RentalId: {}, To: {}, Timestamp: {}",
                    event.rentalId(), event.customerEmail(), LocalDateTime.now(clock));
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. RentalId: {}, Error: {}, SMTP Code: {}",
                    event.rentalId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendLateReturnNotification(LateReturnNotificationEvent event) {
        log.info("Queuing late return notification email. RentalId: {}, Recipient: {}",
                event.rentalId(), event.customerEmail());

        String body = templateService.renderLateReturnNotificationEmail(event);
        EmailMessage message = new EmailMessage(
                event.customerEmail(),
                "🚨 Late Return - Penalty Applied - Rental #" + event.rentalId(),
                body,
                EmailType.RETURN_REMINDER,
                event.rentalId());

        try {
            emailSender.send(message);
            log.info("Late return notification email sent successfully. RentalId: {}, To: {}, Timestamp: {}",
                    event.rentalId(), event.customerEmail(), LocalDateTime.now(clock));
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. RentalId: {}, Error: {}, SMTP Code: {}",
                    event.rentalId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendSeverelyLateNotification(SeverelyLateNotificationEvent event) {
        log.info("Queuing severely late notification email. RentalId: {}, Recipient: {}",
                event.rentalId(), event.customerEmail());

        String body = templateService.renderSeverelyLateNotificationEmail(event);
        EmailMessage message = new EmailMessage(
                event.customerEmail(),
                "🚨 URGENT: Severely Late Return - Rental #" + event.rentalId(),
                body,
                EmailType.RETURN_REMINDER,
                event.rentalId());

        try {
            emailSender.send(message);
            log.info("Severely late notification email sent successfully. RentalId: {}, To: {}, Timestamp: {}",
                    event.rentalId(), event.customerEmail(), LocalDateTime.now(clock));
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. RentalId: {}, Error: {}, SMTP Code: {}",
                    event.rentalId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendPenaltySummary(PenaltySummaryEvent event) {
        log.info("Queuing penalty summary email. RentalId: {}, Recipient: {}",
                event.rentalId(), event.customerEmail());

        String body = templateService.renderPenaltySummaryEmail(event);
        EmailMessage message = new EmailMessage(
                event.customerEmail(),
                "📋 Late Return Penalty Summary - Rental #" + event.rentalId(),
                body,
                EmailType.PAYMENT_RECEIPT,
                event.rentalId());

        try {
            emailSender.send(message);
            log.info("Penalty summary email sent successfully. RentalId: {}, To: {}, Timestamp: {}",
                    event.rentalId(), event.customerEmail(), LocalDateTime.now(clock));
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. RentalId: {}, Error: {}, SMTP Code: {}",
                    event.rentalId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendDamageReportedNotification(DamageReportedEvent event) {
        String customerEmail = event.customerEmail();
        Long damageId = event.damageReportId();
        Long rentalId = event.rentalId();

        log.info("Queuing damage reported email. DamageId: {}, Recipient: {}", damageId, customerEmail);

        String body = templateService.renderDamageReportedEmail(event);
        EmailMessage message = new EmailMessage(
                customerEmail,
                "⚠️ Damage Report Received - Rental #" + rentalId,
                body,
                EmailType.DAMAGE_REPORTED,
                rentalId);

        try {
            emailSender.send(message);
            log.info("Damage reported email sent successfully. DamageId: {}, To: {}", damageId, customerEmail);
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. DamageId: {}, Error: {}", damageId, e.getMessage());
            throw e;
        }
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendDamageAssessedNotification(DamageAssessedEvent event) {
        String customerEmail = event.customerEmail();
        Long damageId = event.damageReportId();
        Long rentalId = event.rentalId();

        log.info("Queuing damage assessed email. DamageId: {}, Recipient: {}", damageId, customerEmail);

        String body = templateService.renderDamageAssessedEmail(event);
        EmailMessage message = new EmailMessage(
                customerEmail,
                "📋 Damage Assessment Complete - Rental #" + rentalId,
                body,
                EmailType.DAMAGE_ASSESSED,
                rentalId);

        try {
            emailSender.send(message);
            log.info("Damage assessed email sent successfully. DamageId: {}, To: {}", damageId, customerEmail);
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. DamageId: {}, Error: {}", damageId, e.getMessage());
            throw e;
        }
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendDamageChargedNotification(DamageChargedEvent event) {
        String customerEmail = event.customerEmail();
        Long damageId = event.damageReportId();
        Long rentalId = event.rentalId();

        log.info("Queuing damage charged email. DamageId: {}, Recipient: {}", damageId, customerEmail);

        String body = templateService.renderDamageChargedEmail(event);
        EmailMessage message = new EmailMessage(
                customerEmail,
                "✅ Damage Charge Processed - Rental #" + rentalId,
                body,
                EmailType.DAMAGE_CHARGED,
                rentalId);

        try {
            emailSender.send(message);
            log.info("Damage charged email sent successfully. DamageId: {}, To: {}", damageId, customerEmail);
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. DamageId: {}, Error: {}", damageId, e.getMessage());
            throw e;
        }
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendDamageDisputedNotification(DamageDisputedEvent event) {
        String customerEmail = event.customerEmail();
        Long damageId = event.damageReportId();
        Long rentalId = event.rentalId();

        log.info("Queuing damage disputed email. DamageId: {}, Recipient: {}", damageId, customerEmail);

        String body = templateService.renderDamageDisputedEmail(event);
        EmailMessage message = new EmailMessage(
                customerEmail,
                "📝 Dispute Received - Rental #" + rentalId,
                body,
                EmailType.DAMAGE_DISPUTED,
                rentalId);

        try {
            emailSender.send(message);
            log.info("Damage disputed email sent successfully. DamageId: {}, To: {}", damageId, customerEmail);
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. DamageId: {}, Error: {}", damageId, e.getMessage());
            throw e;
        }
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendDamageResolvedNotification(DamageResolvedEvent event) {
        String customerEmail = event.customerEmail();
        Long damageId = event.damageReportId();
        Long rentalId = event.rentalId();

        log.info("Queuing damage resolved email. DamageId: {}, Recipient: {}", damageId, customerEmail);

        String body = templateService.renderDamageResolvedEmail(event);
        EmailMessage message = new EmailMessage(
                customerEmail,
                "✅ Dispute Resolved - Rental #" + rentalId,
                body,
                EmailType.DAMAGE_RESOLVED,
                rentalId);

        try {
            emailSender.send(message);
            log.info("Damage resolved email sent successfully. DamageId: {}, To: {}", damageId, customerEmail);
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. DamageId: {}, Error: {}", damageId, e.getMessage());
            throw e;
        }
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendDamageChargeFailedNotification(DamageChargeFailedEvent event) {
        String customerEmail = event.customerEmail();
        Long damageId = event.damageReportId();
        Long rentalId = event.rentalId();

        log.info("Queuing damage charge failed email. DamageId: {}, Recipient: {}", damageId, customerEmail);

        String body = templateService.renderDamageChargeFailedEmail(event);
        EmailMessage message = new EmailMessage(
                customerEmail,
                "🚨 Payment Failed - Damage Charge - Rental #" + rentalId,
                body,
                EmailType.DAMAGE_CHARGE_FAILED,
                rentalId);

        try {
            emailSender.send(message);
            log.info("Damage charge failed email sent successfully. DamageId: {}, To: {}", damageId, customerEmail);
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. DamageId: {}, Error: {}", damageId, e.getMessage());
            throw e;
        }
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendPasswordResetEmail(String email, String resetLink) {
        log.info("Queuing password reset email. Recipient: {}", email);

        String body = templateService.renderPasswordResetEmail(email, resetLink);
        EmailMessage message = new EmailMessage(
                email,
                "🔐 Password Reset Request",
                body,
                EmailType.PASSWORD_RESET,
                null);

        try {
            emailSender.send(message);
            log.info("Password reset email sent successfully. To: {}", email);
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. Email: {}, Error: {}", email, e.getMessage());
            throw e;
        }
    }

}
