package com.akif.notification.internal.service;

import com.akif.damage.api.*;
import com.akif.notification.EmailNotificationService;
import com.akif.notification.internal.exception.EmailSendException;
import com.akif.payment.api.PaymentCapturedEvent;
import com.akif.rental.api.*;
import lombok.RequiredArgsConstructor;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailNotificationServiceImpl implements EmailNotificationService {

    private final RentalEmailNotifier rentalEmails;
    private final LateReturnEmailNotifier lateReturnEmails;
    private final DamageEmailNotifier damageEmails;
    private final PaymentEmailNotifier paymentEmails;
    private final AccountEmailNotifier accountEmails;

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendRentalConfirmation(RentalConfirmedEvent event) {
        rentalEmails.sendRentalConfirmation(event);
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendPaymentReceipt(PaymentCapturedEvent event) {
        paymentEmails.sendPaymentReceipt(event);
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendPickupReminder(PickupReminderEvent event) {
        rentalEmails.sendPickupReminder(event);
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendReturnReminder(ReturnReminderEvent event) {
        rentalEmails.sendReturnReminder(event);
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendCancellationConfirmation(RentalCancelledEvent event) {
        rentalEmails.sendCancellationConfirmation(event);
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendGracePeriodWarning(GracePeriodWarningEvent event) {
        lateReturnEmails.sendGracePeriodWarning(event);
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendLateReturnNotification(LateReturnNotificationEvent event) {
        lateReturnEmails.sendLateReturnNotification(event);
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendSeverelyLateNotification(SeverelyLateNotificationEvent event) {
        lateReturnEmails.sendSeverelyLateNotification(event);
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendPenaltySummary(PenaltySummaryEvent event) {
        lateReturnEmails.sendPenaltySummary(event);
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendDamageReportedNotification(DamageReportedEvent event) {
        damageEmails.sendDamageReportedNotification(event);
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendDamageAssessedNotification(DamageAssessedEvent event) {
        damageEmails.sendDamageAssessedNotification(event);
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendDamageChargedNotification(DamageChargedEvent event) {
        damageEmails.sendDamageChargedNotification(event);
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendDamageDisputedNotification(DamageDisputedEvent event) {
        damageEmails.sendDamageDisputedNotification(event);
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendDamageResolvedNotification(DamageResolvedEvent event) {
        damageEmails.sendDamageResolvedNotification(event);
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendDamageChargeFailedNotification(DamageChargeFailedEvent event) {
        damageEmails.sendDamageChargeFailedNotification(event);
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendPaymentRetryRequest(PaymentRetryRequestedEvent event) {
        paymentEmails.sendPaymentRetryRequest(event);
    }

    @Override
    @Retryable(includes = EmailSendException.class, maxRetries = 3, delay = 1000, multiplier = 2)
    public void sendPasswordResetEmail(String email, String resetLink) {
        accountEmails.sendPasswordResetEmail(email, resetLink);
    }
}
