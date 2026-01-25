package com.akif.notification.internal.listener;

import com.akif.auth.api.PasswordResetRequestedEvent;
import com.akif.damage.api.*;
import com.akif.notification.EmailNotificationService;
import com.akif.payment.api.PaymentCapturedEvent;
import com.akif.rental.api.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailEventListener {

    private final EmailNotificationService emailNotificationService;

    @Async("emailTaskExecutor")
    @EventListener
    public void handleRentalConfirmed(RentalConfirmedEvent event) {
        log.debug("Received RentalConfirmedEvent. RentalId: {}, CustomerEmail: {}",
                event.getRentalId(), event.getCustomerEmail());
        emailNotificationService.sendRentalConfirmation(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handlePaymentCaptured(PaymentCapturedEvent event) {
        log.debug("Received PaymentCapturedEvent. PaymentId: {}, RentalId: {}, CustomerEmail: {}",
                event.getPaymentId(), event.getRentalId(), event.getCustomerEmail());
        emailNotificationService.sendPaymentReceipt(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handlePickupReminder(PickupReminderEvent event) {
        log.debug("Received PickupReminderEvent. RentalId: {}, CustomerEmail: {}, PickupDate: {}",
                event.getRentalId(), event.getCustomerEmail(), event.getPickupDate());
        emailNotificationService.sendPickupReminder(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handleReturnReminder(ReturnReminderEvent event) {
        log.debug("Received ReturnReminderEvent. RentalId: {}, CustomerEmail: {}, ReturnDate: {}",
                event.getRentalId(), event.getCustomerEmail(), event.getReturnDate());
        emailNotificationService.sendReturnReminder(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handleRentalCancelled(RentalCancelledEvent event) {
        log.debug("Received RentalCancelledEvent. RentalId: {}, CustomerEmail: {}, RefundProcessed: {}",
                event.getRentalId(), event.getCustomerEmail(), event.isRefundProcessed());
        emailNotificationService.sendCancellationConfirmation(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handleGracePeriodWarning(GracePeriodWarningEvent event) {
        log.debug("Received GracePeriodWarningEvent. RentalId: {}, CustomerEmail: {}, RemainingMinutes: {}",
                event.getRentalId(), event.getCustomerEmail(), event.getRemainingGraceMinutes());
        emailNotificationService.sendGracePeriodWarning(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handleLateReturnNotification(LateReturnNotificationEvent event) {
        log.debug("Received LateReturnNotificationEvent. RentalId: {}, CustomerEmail: {}, LateHours: {}, Penalty: {}",
                event.getRentalId(), event.getCustomerEmail(), event.getLateHours(), event.getCurrentPenaltyAmount());
        emailNotificationService.sendLateReturnNotification(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handleSeverelyLateNotification(SeverelyLateNotificationEvent event) {
        log.debug("Received SeverelyLateNotificationEvent. RentalId: {}, CustomerEmail: {}, LateDays: {}, Penalty: {}",
                event.getRentalId(), event.getCustomerEmail(), event.getLateDays(), event.getCurrentPenaltyAmount());
        emailNotificationService.sendSeverelyLateNotification(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handlePenaltySummary(PenaltySummaryEvent event) {
        log.debug("Received PenaltySummaryEvent. RentalId: {}, CustomerEmail: {}, FinalPenalty: {}",
                event.getRentalId(), event.getCustomerEmail(), event.getFinalPenaltyAmount());
        emailNotificationService.sendPenaltySummary(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handleDamageReported(DamageReportedEvent event) {
        log.debug("Received DamageReportedEvent. DamageId: {}, RentalId: {}",
                event.getDamageReportId(), event.getRentalId());
        emailNotificationService.sendDamageReportedNotification(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handleDamageAssessed(DamageAssessedEvent event) {
        log.debug("Received DamageAssessedEvent. DamageId: {}, Severity: {}, Liability: {}",
                event.getDamageReportId(), event.getSeverity(), event.getCustomerLiability());
        emailNotificationService.sendDamageAssessedNotification(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handleDamageCharged(DamageChargedEvent event) {
        log.debug("Received DamageChargedEvent. DamageId: {}, TransactionId: {}",
                event.getDamageReportId(), event.getTransactionId());
        emailNotificationService.sendDamageChargedNotification(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handleDamageDisputed(DamageDisputedEvent event) {
        log.debug("Received DamageDisputedEvent. DamageId: {}, Reason: {}",
                event.getDamageReportId(), event.getDisputeReason());
        emailNotificationService.sendDamageDisputedNotification(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handleDamageResolved(DamageResolvedEvent event) {
        log.debug("Received DamageResolvedEvent. DamageId: {}, RefundAmount: {}",
                event.getDamageReportId(), event.getRefundAmount());
        emailNotificationService.sendDamageResolvedNotification(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handleDamageChargeFailed(DamageChargeFailedEvent event) {
        log.debug("Received DamageChargeFailedEvent. DamageId: {}, Reason: {}",
                event.getDamageReportId(), event.getFailureReason());
        emailNotificationService.sendDamageChargeFailedNotification(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handlePasswordResetRequested(PasswordResetRequestedEvent event) {
        log.debug("Received PasswordResetRequestedEvent. Email: {}", event.email());
        emailNotificationService.sendPasswordResetEmail(event.email(), event.resetLink());
    }
}
