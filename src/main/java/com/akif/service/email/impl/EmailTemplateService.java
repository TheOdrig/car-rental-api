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
    
    @Override
    public String renderGracePeriodWarningEmail(GracePeriodWarningEvent event) {
        log.debug("Rendering grace period warning email for rental ID: {}", event.getRentalId());
        
        Context context = new Context();
        context.setVariable("rentalId", event.getRentalId());
        context.setVariable("carBrand", event.getCarBrand());
        context.setVariable("carModel", event.getCarModel());
        context.setVariable("licensePlate", event.getLicensePlate());
        context.setVariable("scheduledReturnTime", event.getScheduledReturnTime());
        context.setVariable("remainingGraceMinutes", event.getRemainingGraceMinutes());
        
        return templateEngine.process("email/grace-period-warning", context);
    }
    
    @Override
    public String renderLateReturnNotificationEmail(LateReturnNotificationEvent event) {
        log.debug("Rendering late return notification email for rental ID: {}", event.getRentalId());
        
        Context context = new Context();
        context.setVariable("rentalId", event.getRentalId());
        context.setVariable("carBrand", event.getCarBrand());
        context.setVariable("carModel", event.getCarModel());
        context.setVariable("licensePlate", event.getLicensePlate());
        context.setVariable("scheduledReturnTime", event.getScheduledReturnTime());
        context.setVariable("lateHours", event.getLateHours());
        context.setVariable("currentPenaltyAmount", event.getCurrentPenaltyAmount());
        context.setVariable("currency", event.getCurrency().name());
        
        return templateEngine.process("email/late-return-notification", context);
    }
    
    @Override
    public String renderSeverelyLateNotificationEmail(SeverelyLateNotificationEvent event) {
        log.debug("Rendering severely late notification email for rental ID: {}", event.getRentalId());
        
        Context context = new Context();
        context.setVariable("rentalId", event.getRentalId());
        context.setVariable("carBrand", event.getCarBrand());
        context.setVariable("carModel", event.getCarModel());
        context.setVariable("licensePlate", event.getLicensePlate());
        context.setVariable("scheduledReturnTime", event.getScheduledReturnTime());
        context.setVariable("lateHours", event.getLateHours());
        context.setVariable("lateDays", event.getLateDays());
        context.setVariable("currentPenaltyAmount", event.getCurrentPenaltyAmount());
        context.setVariable("currency", event.getCurrency().name());
        context.setVariable("escalationWarning", event.getEscalationWarning());
        
        return templateEngine.process("email/severely-late-notification", context);
    }
    
    @Override
    public String renderPenaltySummaryEmail(PenaltySummaryEvent event) {
        log.debug("Rendering penalty summary email for rental ID: {}", event.getRentalId());
        
        Context context = new Context();
        context.setVariable("rentalId", event.getRentalId());
        context.setVariable("carBrand", event.getCarBrand());
        context.setVariable("carModel", event.getCarModel());
        context.setVariable("licensePlate", event.getLicensePlate());
        context.setVariable("scheduledReturnTime", event.getScheduledReturnTime());
        context.setVariable("actualReturnTime", event.getActualReturnTime());
        context.setVariable("lateHours", event.getLateHours());
        context.setVariable("lateDays", event.getLateDays());
        context.setVariable("finalPenaltyAmount", event.getFinalPenaltyAmount());
        context.setVariable("currency", event.getCurrency().name());
        context.setVariable("penaltyBreakdown", event.getPenaltyBreakdown());
        context.setVariable("cappedAtMax", event.isCappedAtMax());
        
        return templateEngine.process("email/penalty-summary", context);
    }
}
