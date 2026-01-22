package com.akif.notification.internal.service;

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
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationServiceImpl implements EmailNotificationService {

    private final IEmailTemplateService templateService;
    private final IEmailSender emailSender;

    @Override
    @Retryable(retryFor = EmailSendException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void sendRentalConfirmation(RentalConfirmedEvent event) {
        log.info("Queuing rental confirmation email. RentalId: {}, Recipient: {}",
                event.getRentalId(), event.getCustomerEmail());

        String body = templateService.renderConfirmationEmail(event);
        EmailMessage message = new EmailMessage(
                event.getCustomerEmail(),
                "Rental Confirmation - #" + event.getRentalId(),
                body,
                EmailType.RENTAL_CONFIRMATION,
                event.getRentalId());

        try {
            emailSender.send(message);
            log.info("Rental confirmation email sent successfully. RentalId: {}, To: {}, Timestamp: {}",
                    event.getRentalId(), event.getCustomerEmail(), LocalDateTime.now());
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. RentalId: {}, Error: {}, SMTP Code: {}",
                    event.getRentalId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }

    @Override
    @Retryable(retryFor = EmailSendException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void sendPaymentReceipt(PaymentCapturedEvent event) {
        log.info("Queuing payment receipt email. PaymentId: {}, RentalId: {}, Recipient: {}",
                event.getPaymentId(), event.getRentalId(), event.getCustomerEmail());

        String body = templateService.renderPaymentReceiptEmail(event);
        EmailMessage message = new EmailMessage(
                event.getCustomerEmail(),
                "Payment Receipt - Transaction #" + event.getTransactionId(),
                body,
                EmailType.PAYMENT_RECEIPT,
                event.getRentalId());

        try {
            emailSender.send(message);
            log.info("Payment receipt email sent successfully. PaymentId: {}, Amount: {}, To: {}, Timestamp: {}",
                    event.getPaymentId(), event.getAmount(), event.getCustomerEmail(), LocalDateTime.now());
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. PaymentId: {}, Error: {}, SMTP Code: {}",
                    event.getPaymentId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }

    @Override
    @Retryable(retryFor = EmailSendException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void sendPickupReminder(PickupReminderEvent event) {
        log.info("Queuing pickup reminder email. RentalId: {}, Recipient: {}",
                event.getRentalId(), event.getCustomerEmail());

        String body = templateService.renderPickupReminderEmail(event);
        EmailMessage message = new EmailMessage(
                event.getCustomerEmail(),
                "Pickup Reminder - Rental #" + event.getRentalId(),
                body,
                EmailType.PICKUP_REMINDER,
                event.getRentalId());

        try {
            emailSender.send(message);
            log.info("Pickup reminder email sent successfully. RentalId: {}, To: {}, Timestamp: {}",
                    event.getRentalId(), event.getCustomerEmail(), LocalDateTime.now());
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. RentalId: {}, Error: {}, SMTP Code: {}",
                    event.getRentalId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }

    @Override
    @Retryable(retryFor = EmailSendException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void sendReturnReminder(ReturnReminderEvent event) {
        log.info("Queuing return reminder email. RentalId: {}, Recipient: {}",
                event.getRentalId(), event.getCustomerEmail());

        String body = templateService.renderReturnReminderEmail(event);
        EmailMessage message = new EmailMessage(
                event.getCustomerEmail(),
                "Return Reminder - Rental #" + event.getRentalId(),
                body,
                EmailType.RETURN_REMINDER,
                event.getRentalId());

        try {
            emailSender.send(message);
            log.info("Return reminder email sent successfully. RentalId: {}, To: {}, Timestamp: {}",
                    event.getRentalId(), event.getCustomerEmail(), LocalDateTime.now());
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. RentalId: {}, Error: {}, SMTP Code: {}",
                    event.getRentalId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }

    @Override
    @Retryable(retryFor = EmailSendException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void sendCancellationConfirmation(RentalCancelledEvent event) {
        log.info("Queuing cancellation confirmation email. RentalId: {}, Recipient: {}",
                event.getRentalId(), event.getCustomerEmail());

        String body = templateService.renderCancellationEmail(event);
        EmailMessage message = new EmailMessage(
                event.getCustomerEmail(),
                "Cancellation Confirmation - Rental #" + event.getRentalId(),
                body,
                EmailType.CANCELLATION_CONFIRMATION,
                event.getRentalId());

        try {
            emailSender.send(message);
            log.info("Cancellation confirmation email sent successfully. RentalId: {}, To: {}, Timestamp: {}",
                    event.getRentalId(), event.getCustomerEmail(), LocalDateTime.now());
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. RentalId: {}, Error: {}, SMTP Code: {}",
                    event.getRentalId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }

    @SuppressWarnings("unused")
    @Recover
    public void recoverFromRentalConfirmationFailure(EmailSendException e, RentalConfirmedEvent event) {
        log.error(
                "Rental confirmation email failed after all retries. RentalId: {}, Recipient: {}, Error: {}, SMTP Code: {}",
                event.getRentalId(), event.getCustomerEmail(), e.getMessage(), e.getSmtpErrorCode());
    }

    @SuppressWarnings("unused")
    @Recover
    public void recoverFromPaymentReceiptFailure(EmailSendException e, PaymentCapturedEvent event) {
        log.error(
                "Payment receipt email failed after all retries. PaymentId: {}, RentalId: {}, Recipient: {}, Error: {}, SMTP Code: {}",
                event.getPaymentId(), event.getRentalId(), event.getCustomerEmail(), e.getMessage(),
                e.getSmtpErrorCode());
    }

    @SuppressWarnings("unused")
    @Recover
    public void recoverFromPickupReminderFailure(EmailSendException e, PickupReminderEvent event) {
        log.error(
                "Pickup reminder email failed after all retries. RentalId: {}, Recipient: {}, Error: {}, SMTP Code: {}",
                event.getRentalId(), event.getCustomerEmail(), e.getMessage(), e.getSmtpErrorCode());
    }

    @SuppressWarnings("unused")
    @Recover
    public void recoverFromReturnReminderFailure(EmailSendException e, ReturnReminderEvent event) {
        log.error(
                "Return reminder email failed after all retries. RentalId: {}, Recipient: {}, Error: {}, SMTP Code: {}",
                event.getRentalId(), event.getCustomerEmail(), e.getMessage(), e.getSmtpErrorCode());
    }

    @SuppressWarnings("unused")
    @Recover
    public void recoverFromCancellationFailure(EmailSendException e, RentalCancelledEvent event) {
        log.error(
                "Cancellation confirmation email failed after all retries. RentalId: {}, Recipient: {}, Error: {}, SMTP Code: {}",
                event.getRentalId(), event.getCustomerEmail(), e.getMessage(), e.getSmtpErrorCode());
    }

    @Override
    @Retryable(retryFor = EmailSendException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void sendGracePeriodWarning(GracePeriodWarningEvent event) {
        log.info("Queuing grace period warning email. RentalId: {}, Recipient: {}",
                event.getRentalId(), event.getCustomerEmail());

        String body = templateService.renderGracePeriodWarningEmail(event);
        EmailMessage message = new EmailMessage(
                event.getCustomerEmail(),
                "⚠️ Grace Period Warning - Rental #" + event.getRentalId(),
                body,
                EmailType.RETURN_REMINDER,
                event.getRentalId());

        try {
            emailSender.send(message);
            log.info("Grace period warning email sent successfully. RentalId: {}, To: {}, Timestamp: {}",
                    event.getRentalId(), event.getCustomerEmail(), LocalDateTime.now());
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. RentalId: {}, Error: {}, SMTP Code: {}",
                    event.getRentalId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }

    @Override
    @Retryable(retryFor = EmailSendException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void sendLateReturnNotification(LateReturnNotificationEvent event) {
        log.info("Queuing late return notification email. RentalId: {}, Recipient: {}",
                event.getRentalId(), event.getCustomerEmail());

        String body = templateService.renderLateReturnNotificationEmail(event);
        EmailMessage message = new EmailMessage(
                event.getCustomerEmail(),
                "🚨 Late Return - Penalty Applied - Rental #" + event.getRentalId(),
                body,
                EmailType.RETURN_REMINDER,
                event.getRentalId());

        try {
            emailSender.send(message);
            log.info("Late return notification email sent successfully. RentalId: {}, To: {}, Timestamp: {}",
                    event.getRentalId(), event.getCustomerEmail(), LocalDateTime.now());
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. RentalId: {}, Error: {}, SMTP Code: {}",
                    event.getRentalId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }

    @Override
    @Retryable(retryFor = EmailSendException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void sendSeverelyLateNotification(SeverelyLateNotificationEvent event) {
        log.info("Queuing severely late notification email. RentalId: {}, Recipient: {}",
                event.getRentalId(), event.getCustomerEmail());

        String body = templateService.renderSeverelyLateNotificationEmail(event);
        EmailMessage message = new EmailMessage(
                event.getCustomerEmail(),
                "🚨 URGENT: Severely Late Return - Rental #" + event.getRentalId(),
                body,
                EmailType.RETURN_REMINDER,
                event.getRentalId());

        try {
            emailSender.send(message);
            log.info("Severely late notification email sent successfully. RentalId: {}, To: {}, Timestamp: {}",
                    event.getRentalId(), event.getCustomerEmail(), LocalDateTime.now());
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. RentalId: {}, Error: {}, SMTP Code: {}",
                    event.getRentalId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }

    @Override
    @Retryable(retryFor = EmailSendException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void sendPenaltySummary(PenaltySummaryEvent event) {
        log.info("Queuing penalty summary email. RentalId: {}, Recipient: {}",
                event.getRentalId(), event.getCustomerEmail());

        String body = templateService.renderPenaltySummaryEmail(event);
        EmailMessage message = new EmailMessage(
                event.getCustomerEmail(),
                "📋 Late Return Penalty Summary - Rental #" + event.getRentalId(),
                body,
                EmailType.PAYMENT_RECEIPT,
                event.getRentalId());

        try {
            emailSender.send(message);
            log.info("Penalty summary email sent successfully. RentalId: {}, To: {}, Timestamp: {}",
                    event.getRentalId(), event.getCustomerEmail(), LocalDateTime.now());
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. RentalId: {}, Error: {}, SMTP Code: {}",
                    event.getRentalId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }

    @SuppressWarnings("unused")
    @Recover
    public void recoverFromGracePeriodWarningFailure(EmailSendException e, GracePeriodWarningEvent event) {
        log.error(
                "Grace period warning email failed after all retries. RentalId: {}, Recipient: {}, Error: {}, SMTP Code: {}",
                event.getRentalId(), event.getCustomerEmail(), e.getMessage(), e.getSmtpErrorCode());
    }

    @SuppressWarnings("unused")
    @Recover
    public void recoverFromLateReturnNotificationFailure(EmailSendException e, LateReturnNotificationEvent event) {
        log.error(
                "Late return notification email failed after all retries. RentalId: {}, Recipient: {}, Error: {}, SMTP Code: {}",
                event.getRentalId(), event.getCustomerEmail(), e.getMessage(), e.getSmtpErrorCode());
    }

    @SuppressWarnings("unused")
    @Recover
    public void recoverFromSeverelyLateNotificationFailure(EmailSendException e, SeverelyLateNotificationEvent event) {
        log.error(
                "Severely late notification email failed after all retries. RentalId: {}, Recipient: {}, Error: {}, SMTP Code: {}",
                event.getRentalId(), event.getCustomerEmail(), e.getMessage(), e.getSmtpErrorCode());
    }

    @SuppressWarnings("unused")
    @Recover
    public void recoverFromPenaltySummaryFailure(EmailSendException e, PenaltySummaryEvent event) {
        log.error(
                "Penalty summary email failed after all retries. RentalId: {}, Recipient: {}, Error: {}, SMTP Code: {}",
                event.getRentalId(), event.getCustomerEmail(), e.getMessage(), e.getSmtpErrorCode());
    }

    @Override
    @Retryable(retryFor = EmailSendException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void sendDamageReportedNotification(DamageReportedEvent event) {
        String customerEmail = event.getCustomerEmail();
        Long damageId = event.getDamageReportId();
        Long rentalId = event.getRentalId();

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
    @Retryable(retryFor = EmailSendException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void sendDamageAssessedNotification(DamageAssessedEvent event) {
        String customerEmail = event.getCustomerEmail();
        Long damageId = event.getDamageReportId();
        Long rentalId = event.getRentalId();

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
    @Retryable(retryFor = EmailSendException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void sendDamageChargedNotification(DamageChargedEvent event) {
        String customerEmail = event.getCustomerEmail();
        Long damageId = event.getDamageReportId();
        Long rentalId = event.getRentalId();

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
    @Retryable(retryFor = EmailSendException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void sendDamageDisputedNotification(DamageDisputedEvent event) {
        String customerEmail = event.getCustomerEmail();
        Long damageId = event.getDamageReportId();
        Long rentalId = event.getRentalId();

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
    @Retryable(retryFor = EmailSendException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void sendDamageResolvedNotification(DamageResolvedEvent event) {
        String customerEmail = event.getCustomerEmail();
        Long damageId = event.getDamageReportId();
        Long rentalId = event.getRentalId();

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

    @SuppressWarnings("unused")
    @Recover
    public void recoverFromDamageReportedFailure(EmailSendException e, DamageReportedEvent event) {
        log.error("Damage reported email failed after all retries. DamageId: {}, Error: {}",
                event.getDamageReportId(), e.getMessage());
    }

    @SuppressWarnings("unused")
    @Recover
    public void recoverFromDamageAssessedFailure(EmailSendException e, DamageAssessedEvent event) {
        log.error("Damage assessed email failed after all retries. DamageId: {}, Error: {}",
                event.getDamageReportId(), e.getMessage());
    }

    @SuppressWarnings("unused")
    @Recover
    public void recoverFromDamageChargedFailure(EmailSendException e, DamageChargedEvent event) {
        log.error("Damage charged email failed after all retries. DamageId: {}, Error: {}",
                event.getDamageReportId(), e.getMessage());
    }

    @SuppressWarnings("unused")
    @Recover
    public void recoverFromDamageDisputedFailure(EmailSendException e, DamageDisputedEvent event) {
        log.error("Damage disputed email failed after all retries. DamageId: {}, Error: {}",
                event.getDamageReportId(), e.getMessage());
    }

    @SuppressWarnings("unused")
    @Recover
    public void recoverFromDamageResolvedFailure(EmailSendException e, DamageResolvedEvent event) {
        log.error("Damage resolved email failed after all retries. DamageId: {}, Error: {}",
                event.getDamageReportId(), e.getMessage());
    }

    @Override
    @Retryable(retryFor = EmailSendException.class, maxAttempts = 4, backoff = @Backoff(delay = 1000, multiplier = 2))
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

    @SuppressWarnings("unused")
    @Recover
    public void recoverFromPasswordResetFailure(EmailSendException e, String email, String resetLink) {
        log.error("Password reset email failed after all retries. Email: {}, Error: {}",
                email, e.getMessage());
    }
}
