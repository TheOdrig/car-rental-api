package com.akif.service.email.impl;

import com.akif.event.*;
import com.akif.service.email.IEmailTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateService implements IEmailTemplateService {
    
    private final SpringTemplateEngine templateEngine;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm");

    @Override
    public String renderConfirmationEmail(RentalConfirmedEvent event) {
        log.debug("Rendering confirmation email for rental ID: {}", event.getRentalId());
        
        Context context = new Context();
        context.setVariable("rentalId", event.getRentalId());
        context.setVariable("carBrand", event.getCarBrand());
        context.setVariable("carModel", event.getCarModel());
        context.setVariable("pickupDate", event.getPickupDate().format(DATE_FORMATTER));
        context.setVariable("returnDate", event.getReturnDate().format(DATE_FORMATTER));
        context.setVariable("totalPrice", event.getTotalPrice());
        context.setVariable("currency", event.getCurrency().name());
        context.setVariable("pickupLocation", event.getPickupLocation());
        
        return templateEngine.process("email/rental-confirmation", context);
    }

    @Override
    public String renderPaymentReceiptEmail(PaymentCapturedEvent event) {
        log.debug("Rendering payment receipt email for payment ID: {}", event.getPaymentId());
        
        Context context = new Context();
        context.setVariable("paymentId", event.getPaymentId());
        context.setVariable("rentalId", event.getRentalId());
        context.setVariable("transactionId", event.getTransactionId());
        context.setVariable("amount", event.getAmount());
        context.setVariable("currency", event.getCurrency().name());
        context.setVariable("paymentDate", event.getPaymentDate().format(DATE_TIME_FORMATTER));
        
        return templateEngine.process("email/payment-receipt", context);
    }

    @Override
    public String renderPickupReminderEmail(PickupReminderEvent event) {
        log.debug("Rendering pickup reminder email for rental ID: {}", event.getRentalId());
        
        Context context = new Context();
        context.setVariable("rentalId", event.getRentalId());
        context.setVariable("pickupDate", event.getPickupDate().format(DATE_FORMATTER));
        context.setVariable("pickupLocation", event.getPickupLocation());
        context.setVariable("carBrand", event.getCarBrand());
        context.setVariable("carModel", event.getCarModel());
        context.setVariable("timeWindow", "9:00 AM - 6:00 PM");
        
        return templateEngine.process("email/pickup-reminder", context);
    }

    @Override
    public String renderReturnReminderEmail(ReturnReminderEvent event) {
        log.debug("Rendering return reminder email for rental ID: {}", event.getRentalId());
        
        Context context = new Context();
        context.setVariable("rentalId", event.getRentalId());
        context.setVariable("returnDate", event.getReturnDate().format(DATE_FORMATTER));
        context.setVariable("returnLocation", event.getReturnLocation());
        context.setVariable("dailyPenaltyRate", event.getDailyPenaltyRate());
        
        return templateEngine.process("email/return-reminder", context);
    }

    @Override
    public String renderCancellationEmail(RentalCancelledEvent event) {
        log.debug("Rendering cancellation email for rental ID: {}", event.getRentalId());
        
        Context context = new Context();
        context.setVariable("rentalId", event.getRentalId());
        context.setVariable("cancellationDate", event.getCancellationDate().format(DATE_TIME_FORMATTER));
        context.setVariable("cancellationReason", event.getCancellationReason());
        context.setVariable("refundProcessed", event.isRefundProcessed());
        context.setVariable("refundAmount", event.getRefundAmount());
        context.setVariable("refundTransactionId", event.getRefundTransactionId());
        context.setVariable("refundTimeline", "3-5 business days");
        
        return templateEngine.process("email/cancellation-confirmation", context);
    }
}
