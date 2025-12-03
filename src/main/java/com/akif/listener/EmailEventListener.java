package com.akif.listener;

import com.akif.event.*;
import com.akif.service.email.IEmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailEventListener {
    
    private final IEmailNotificationService emailNotificationService;

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
}
