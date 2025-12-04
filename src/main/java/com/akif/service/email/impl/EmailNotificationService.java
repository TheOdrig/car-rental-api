package com.akif.service.email.impl;

import com.akif.dto.email.EmailMessage;
import com.akif.enums.EmailType;
import com.akif.event.*;
import com.akif.exception.EmailSendException;
import com.akif.service.email.IEmailNotificationService;
import com.akif.service.email.IEmailSender;
import com.akif.service.email.IEmailTemplateService;
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
public class EmailNotificationService implements IEmailNotificationService {
    
    private final IEmailTemplateService templateService;
    private final IEmailSender emailSender;
    
    @Override
    @Retryable(
        retryFor = EmailSendException.class,
        maxAttempts = 4,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void sendRentalConfirmation(RentalConfirmedEvent event) {
        log.info("Queuing rental confirmation email. RentalId: {}, Recipient: {}", 
            event.getRentalId(), event.getCustomerEmail());
        
        String body = templateService.renderConfirmationEmail(event);
        EmailMessage message = new EmailMessage(
            event.getCustomerEmail(),
            "Rental Confirmation - #" + event.getRentalId(),
            body,
            EmailType.RENTAL_CONFIRMATION,
            event.getRentalId()
        );
        
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
    @Retryable(
        retryFor = EmailSendException.class,
        maxAttempts = 4,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void sendPaymentReceipt(PaymentCapturedEvent event) {
        log.info("Queuing payment receipt email. PaymentId: {}, RentalId: {}, Recipient: {}", 
            event.getPaymentId(), event.getRentalId(), event.getCustomerEmail());
        
        String body = templateService.renderPaymentReceiptEmail(event);
        EmailMessage message = new EmailMessage(
            event.getCustomerEmail(),
            "Payment Receipt - Transaction #" + event.getTransactionId(),
            body,
            EmailType.PAYMENT_RECEIPT,
            event.getRentalId()
        );
        
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
    @Retryable(
        retryFor = EmailSendException.class,
        maxAttempts = 4,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void sendPickupReminder(PickupReminderEvent event) {
        log.info("Queuing pickup reminder email. RentalId: {}, Recipient: {}", 
            event.getRentalId(), event.getCustomerEmail());
        
        String body = templateService.renderPickupReminderEmail(event);
        EmailMessage message = new EmailMessage(
            event.getCustomerEmail(),
            "Pickup Reminder - Rental #" + event.getRentalId(),
            body,
            EmailType.PICKUP_REMINDER,
            event.getRentalId()
        );
        
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
    @Retryable(
        retryFor = EmailSendException.class,
        maxAttempts = 4,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void sendReturnReminder(ReturnReminderEvent event) {
        log.info("Queuing return reminder email. RentalId: {}, Recipient: {}", 
            event.getRentalId(), event.getCustomerEmail());
        
        String body = templateService.renderReturnReminderEmail(event);
        EmailMessage message = new EmailMessage(
            event.getCustomerEmail(),
            "Return Reminder - Rental #" + event.getRentalId(),
            body,
            EmailType.RETURN_REMINDER,
            event.getRentalId()
        );
        
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
    @Retryable(
        retryFor = EmailSendException.class,
        maxAttempts = 4,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void sendCancellationConfirmation(RentalCancelledEvent event) {
        log.info("Queuing cancellation confirmation email. RentalId: {}, Recipient: {}", 
            event.getRentalId(), event.getCustomerEmail());
        
        String body = templateService.renderCancellationEmail(event);
        EmailMessage message = new EmailMessage(
            event.getCustomerEmail(),
            "Cancellation Confirmation - Rental #" + event.getRentalId(),
            body,
            EmailType.CANCELLATION_CONFIRMATION,
            event.getRentalId()
        );
        
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

    
    @Recover
    public void recoverFromRentalConfirmationFailure(EmailSendException e, RentalConfirmedEvent event) {
        log.error("Rental confirmation email failed after all retries. RentalId: {}, Recipient: {}, Error: {}, SMTP Code: {}", 
            event.getRentalId(), event.getCustomerEmail(), e.getMessage(), e.getSmtpErrorCode());
    }
    
    @Recover
    public void recoverFromPaymentReceiptFailure(EmailSendException e, PaymentCapturedEvent event) {
        log.error("Payment receipt email failed after all retries. PaymentId: {}, RentalId: {}, Recipient: {}, Error: {}, SMTP Code: {}", 
            event.getPaymentId(), event.getRentalId(), event.getCustomerEmail(), e.getMessage(), e.getSmtpErrorCode());
    }
    
    @Recover
    public void recoverFromPickupReminderFailure(EmailSendException e, PickupReminderEvent event) {
        log.error("Pickup reminder email failed after all retries. RentalId: {}, Recipient: {}, Error: {}, SMTP Code: {}", 
            event.getRentalId(), event.getCustomerEmail(), e.getMessage(), e.getSmtpErrorCode());
    }
    
    @Recover
    public void recoverFromReturnReminderFailure(EmailSendException e, ReturnReminderEvent event) {
        log.error("Return reminder email failed after all retries. RentalId: {}, Recipient: {}, Error: {}, SMTP Code: {}", 
            event.getRentalId(), event.getCustomerEmail(), e.getMessage(), e.getSmtpErrorCode());
    }
    
    @Recover
    public void recoverFromCancellationFailure(EmailSendException e, RentalCancelledEvent event) {
        log.error("Cancellation confirmation email failed after all retries. RentalId: {}, Recipient: {}, Error: {}, SMTP Code: {}", 
            event.getRentalId(), event.getCustomerEmail(), e.getMessage(), e.getSmtpErrorCode());
    }
    
    @Override
    @Retryable(
        retryFor = EmailSendException.class,
        maxAttempts = 4,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void sendGracePeriodWarning(GracePeriodWarningEvent event) {
        log.info("Queuing grace period warning email. RentalId: {}, Recipient: {}", 
            event.getRentalId(), event.getCustomerEmail());
        
        String body = templateService.renderGracePeriodWarningEmail(event);
        EmailMessage message = new EmailMessage(
            event.getCustomerEmail(),
            "⚠️ Grace Period Warning - Rental #" + event.getRentalId(),
            body,
            EmailType.RETURN_REMINDER,
            event.getRentalId()
        );
        
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
    @Retryable(
        retryFor = EmailSendException.class,
        maxAttempts = 4,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void sendLateReturnNotification(LateReturnNotificationEvent event) {
        log.info("Queuing late return notification email. RentalId: {}, Recipient: {}", 
            event.getRentalId(), event.getCustomerEmail());
        
        String body = templateService.renderLateReturnNotificationEmail(event);
        EmailMessage message = new EmailMessage(
            event.getCustomerEmail(),
            "🚨 Late Return - Penalty Applied - Rental #" + event.getRentalId(),
            body,
            EmailType.RETURN_REMINDER,
            event.getRentalId()
        );
        
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
    @Retryable(
        retryFor = EmailSendException.class,
        maxAttempts = 4,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void sendSeverelyLateNotification(SeverelyLateNotificationEvent event) {
        log.info("Queuing severely late notification email. RentalId: {}, Recipient: {}", 
            event.getRentalId(), event.getCustomerEmail());
        
        String body = templateService.renderSeverelyLateNotificationEmail(event);
        EmailMessage message = new EmailMessage(
            event.getCustomerEmail(),
            "🚨 URGENT: Severely Late Return - Rental #" + event.getRentalId(),
            body,
            EmailType.RETURN_REMINDER,
            event.getRentalId()
        );
        
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
    @Retryable(
        retryFor = EmailSendException.class,
        maxAttempts = 4,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void sendPenaltySummary(PenaltySummaryEvent event) {
        log.info("Queuing penalty summary email. RentalId: {}, Recipient: {}", 
            event.getRentalId(), event.getCustomerEmail());
        
        String body = templateService.renderPenaltySummaryEmail(event);
        EmailMessage message = new EmailMessage(
            event.getCustomerEmail(),
            "📋 Late Return Penalty Summary - Rental #" + event.getRentalId(),
            body,
            EmailType.PAYMENT_RECEIPT,
            event.getRentalId()
        );
        
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
    
    @Recover
    public void recoverFromGracePeriodWarningFailure(EmailSendException e, GracePeriodWarningEvent event) {
        log.error("Grace period warning email failed after all retries. RentalId: {}, Recipient: {}, Error: {}, SMTP Code: {}", 
            event.getRentalId(), event.getCustomerEmail(), e.getMessage(), e.getSmtpErrorCode());
    }
    
    @Recover
    public void recoverFromLateReturnNotificationFailure(EmailSendException e, LateReturnNotificationEvent event) {
        log.error("Late return notification email failed after all retries. RentalId: {}, Recipient: {}, Error: {}, SMTP Code: {}", 
            event.getRentalId(), event.getCustomerEmail(), e.getMessage(), e.getSmtpErrorCode());
    }
    
    @Recover
    public void recoverFromSeverelyLateNotificationFailure(EmailSendException e, SeverelyLateNotificationEvent event) {
        log.error("Severely late notification email failed after all retries. RentalId: {}, Recipient: {}, Error: {}, SMTP Code: {}", 
            event.getRentalId(), event.getCustomerEmail(), e.getMessage(), e.getSmtpErrorCode());
    }
    
    @Recover
    public void recoverFromPenaltySummaryFailure(EmailSendException e, PenaltySummaryEvent event) {
        log.error("Penalty summary email failed after all retries. RentalId: {}, Recipient: {}, Error: {}, SMTP Code: {}", 
            event.getRentalId(), event.getCustomerEmail(), e.getMessage(), e.getSmtpErrorCode());
    }
}
