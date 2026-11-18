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
                event.rentalId(), event.customerEmail());
        emailNotificationService.sendRentalConfirmation(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handlePaymentCaptured(PaymentCapturedEvent event) {
        log.debug("Received PaymentCapturedEvent. PaymentId: {}, RentalId: {}, CustomerEmail: {}",
                event.paymentId(), event.rentalId(), event.customerEmail());
        emailNotificationService.sendPaymentReceipt(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handlePickupReminder(PickupReminderEvent event) {
        log.debug("Received PickupReminderEvent. RentalId: {}, CustomerEmail: {}, PickupDate: {}",
                event.rentalId(), event.customerEmail(), event.pickupDate());
        emailNotificationService.sendPickupReminder(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handleReturnReminder(ReturnReminderEvent event) {
        log.debug("Received ReturnReminderEvent. RentalId: {}, CustomerEmail: {}, ReturnDate: {}",
                event.rentalId(), event.customerEmail(), event.returnDate());
        emailNotificationService.sendReturnReminder(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handleRentalCancelled(RentalCancelledEvent event) {
        log.debug("Received RentalCancelledEvent. RentalId: {}, CustomerEmail: {}, RefundProcessed: {}",
                event.rentalId(), event.customerEmail(), event.refundProcessed());
        emailNotificationService.sendCancellationConfirmation(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handleGracePeriodWarning(GracePeriodWarningEvent event) {
        log.debug("Received GracePeriodWarningEvent. RentalId: {}, CustomerEmail: {}, RemainingMinutes: {}",
                event.rentalId(), event.customerEmail(), event.remainingGraceMinutes());
        emailNotificationService.sendGracePeriodWarning(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handleLateReturnNotification(LateReturnNotificationEvent event) {
        log.debug("Received LateReturnNotificationEvent. RentalId: {}, CustomerEmail: {}, LateHours: {}, Penalty: {}",
                event.rentalId(), event.customerEmail(), event.lateHours(), event.currentPenaltyAmount());
        emailNotificationService.sendLateReturnNotification(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handleSeverelyLateNotification(SeverelyLateNotificationEvent event) {
        log.debug("Received SeverelyLateNotificationEvent. RentalId: {}, CustomerEmail: {}, LateDays: {}, Penalty: {}",
                event.rentalId(), event.customerEmail(), event.lateDays(), event.currentPenaltyAmount());
        emailNotificationService.sendSeverelyLateNotification(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handlePenaltySummary(PenaltySummaryEvent event) {
        log.debug("Received PenaltySummaryEvent. RentalId: {}, CustomerEmail: {}, FinalPenalty: {}",
                event.rentalId(), event.customerEmail(), event.finalPenaltyAmount());
        emailNotificationService.sendPenaltySummary(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handleDamageReported(DamageReportedEvent event) {
        log.debug("Received DamageReportedEvent. DamageId: {}, RentalId: {}",
                event.damageReportId(), event.rentalId());
        emailNotificationService.sendDamageReportedNotification(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handleDamageAssessed(DamageAssessedEvent event) {
        log.debug("Received DamageAssessedEvent. DamageId: {}, Severity: {}, Liability: {}",
                event.damageReportId(), event.severity(), event.customerLiability());
        emailNotificationService.sendDamageAssessedNotification(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handleDamageCharged(DamageChargedEvent event) {
        log.debug("Received DamageChargedEvent. DamageId: {}, TransactionId: {}",
                event.damageReportId(), event.transactionId());
        emailNotificationService.sendDamageChargedNotification(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handleDamageDisputed(DamageDisputedEvent event) {
        log.debug("Received DamageDisputedEvent. DamageId: {}, Reason: {}",
                event.damageReportId(), event.disputeReason());
        emailNotificationService.sendDamageDisputedNotification(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handleDamageResolved(DamageResolvedEvent event) {
        log.debug("Received DamageResolvedEvent. DamageId: {}, RefundAmount: {}",
                event.damageReportId(), event.refundAmount());
        emailNotificationService.sendDamageResolvedNotification(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handleDamageChargeFailed(DamageChargeFailedEvent event) {
        log.debug("Received DamageChargeFailedEvent. DamageId: {}, Reason: {}",
                event.damageReportId(), event.failureReason());
        emailNotificationService.sendDamageChargeFailedNotification(event);
    }

    @Async("emailTaskExecutor")
    @EventListener
    public void handlePasswordResetRequested(PasswordResetRequestedEvent event) {
        log.debug("Received PasswordResetRequestedEvent. Email: {}", event.email());
        emailNotificationService.sendPasswordResetEmail(event.email(), event.resetLink());
    }
}
