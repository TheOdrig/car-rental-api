package com.akif.notification.internal.service.email.impl;

import com.akif.damage.api.*;
import com.akif.notification.internal.service.email.IEmailTemplateService;
import com.akif.payment.api.PaymentCapturedEvent;
import com.akif.rental.api.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateService implements IEmailTemplateService {

    private final SpringTemplateEngine templateEngine;

    @Value("${app.password-reset.base-url:http://localhost:3000}")
    private String baseUrl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("MMMM dd, yyyy 'at' HH:mm");

    @Override
    public String renderConfirmationEmail(RentalConfirmedEvent event) {
        log.debug("Rendering confirmation email for rental ID: {}", event.rentalId());

        Context context = new Context();
        context.setVariable("rentalId", event.rentalId());
        context.setVariable("carBrand", event.carBrand());
        context.setVariable("carModel", event.carModel());
        context.setVariable("pickupDate", event.pickupDate().format(DATE_FORMATTER));
        context.setVariable("returnDate", event.returnDate().format(DATE_FORMATTER));
        context.setVariable("totalPrice", event.totalPrice());
        context.setVariable("currency", event.currency().name());
        context.setVariable("pickupLocation", event.pickupLocation());

        return templateEngine.process("email/rental/rental-confirmation", context);
    }

    @Override
    public String renderPaymentReceiptEmail(PaymentCapturedEvent event) {
        log.debug("Rendering payment receipt email for payment ID: {}", event.paymentId());

        Context context = new Context();
        context.setVariable("paymentId", event.paymentId());
        context.setVariable("rentalId", event.rentalId());
        context.setVariable("transactionId", event.transactionId());
        context.setVariable("amount", event.amount());
        context.setVariable("currency", event.currency().name());
        context.setVariable("paymentDate", event.paymentDate().format(DATE_TIME_FORMATTER));

        return templateEngine.process("email/payment/payment-receipt", context);
    }

    @Override
    public String renderPickupReminderEmail(PickupReminderEvent event) {
        log.debug("Rendering pickup reminder email for rental ID: {}", event.rentalId());

        Context context = new Context();
        context.setVariable("rentalId", event.rentalId());
        context.setVariable("pickupDate", event.pickupDate().format(DATE_FORMATTER));
        context.setVariable("pickupLocation", event.pickupLocation());
        context.setVariable("carBrand", event.carBrand());
        context.setVariable("carModel", event.carModel());
        context.setVariable("timeWindow", "9:00 AM - 6:00 PM");

        return templateEngine.process("email/rental/pickup-reminder", context);
    }

    @Override
    public String renderReturnReminderEmail(ReturnReminderEvent event) {
        log.debug("Rendering return reminder email for rental ID: {}", event.rentalId());

        Context context = new Context();
        context.setVariable("rentalId", event.rentalId());
        context.setVariable("returnDate", event.returnDate().format(DATE_FORMATTER));
        context.setVariable("returnLocation", event.returnLocation());
        context.setVariable("dailyPenaltyRate", event.dailyPenaltyRate());

        return templateEngine.process("email/rental/return-reminder", context);
    }

    @Override
    public String renderCancellationEmail(RentalCancelledEvent event) {
        log.debug("Rendering cancellation email for rental ID: {}", event.rentalId());

        Context context = new Context();
        context.setVariable("rentalId", event.rentalId());
        context.setVariable("cancellationDate", event.cancellationDate().format(DATE_TIME_FORMATTER));
        context.setVariable("cancellationReason", event.cancellationReason());
        context.setVariable("refundProcessed", event.refundProcessed());
        context.setVariable("refundAmount", event.refundAmount());
        context.setVariable("refundTransactionId", event.refundTransactionId());
        context.setVariable("refundTimeline", "3-5 business days");

        return templateEngine.process("email/rental/cancellation-confirmation", context);
    }

    @Override
    public String renderGracePeriodWarningEmail(GracePeriodWarningEvent event) {
        log.debug("Rendering grace period warning email for rental ID: {}", event.rentalId());

        Context context = new Context();
        context.setVariable("rentalId", event.rentalId());
        context.setVariable("carBrand", event.carBrand());
        context.setVariable("carModel", event.carModel());
        context.setVariable("licensePlate", event.licensePlate());
        context.setVariable("scheduledReturnTime", event.scheduledReturnTime());
        context.setVariable("remainingGraceMinutes", event.remainingGraceMinutes());

        return templateEngine.process("email/penalty/grace-period-warning", context);
    }

    @Override
    public String renderLateReturnNotificationEmail(LateReturnNotificationEvent event) {
        log.debug("Rendering late return notification email for rental ID: {}", event.rentalId());

        Context context = new Context();
        context.setVariable("rentalId", event.rentalId());
        context.setVariable("carBrand", event.carBrand());
        context.setVariable("carModel", event.carModel());
        context.setVariable("licensePlate", event.licensePlate());
        context.setVariable("scheduledReturnTime", event.scheduledReturnTime());
        context.setVariable("lateHours", event.lateHours());
        context.setVariable("currentPenaltyAmount", event.currentPenaltyAmount());
        context.setVariable("currency", event.currency().name());

        return templateEngine.process("email/penalty/late-return-notification", context);
    }

    @Override
    public String renderSeverelyLateNotificationEmail(SeverelyLateNotificationEvent event) {
        log.debug("Rendering severely late notification email for rental ID: {}", event.rentalId());

        Context context = new Context();
        context.setVariable("rentalId", event.rentalId());
        context.setVariable("carBrand", event.carBrand());
        context.setVariable("carModel", event.carModel());
        context.setVariable("licensePlate", event.licensePlate());
        context.setVariable("scheduledReturnTime", event.scheduledReturnTime());
        context.setVariable("lateHours", event.lateHours());
        context.setVariable("lateDays", event.lateDays());
        context.setVariable("currentPenaltyAmount", event.currentPenaltyAmount());
        context.setVariable("currency", event.currency().name());
        context.setVariable("escalationWarning", event.escalationWarning());

        return templateEngine.process("email/penalty/severely-late-notification", context);
    }

    @Override
    public String renderPenaltySummaryEmail(PenaltySummaryEvent event) {
        log.debug("Rendering penalty summary email for rental ID: {}", event.rentalId());

        Context context = new Context();
        context.setVariable("rentalId", event.rentalId());
        context.setVariable("carBrand", event.carBrand());
        context.setVariable("carModel", event.carModel());
        context.setVariable("licensePlate", event.licensePlate());
        context.setVariable("scheduledReturnTime", event.scheduledReturnTime());
        context.setVariable("actualReturnTime", event.actualReturnTime());
        context.setVariable("lateHours", event.lateHours());
        context.setVariable("lateDays", event.lateDays());
        context.setVariable("finalPenaltyAmount", event.finalPenaltyAmount());
        context.setVariable("currency", event.currency().name());
        context.setVariable("penaltyBreakdown", event.penaltyBreakdown());
        context.setVariable("cappedAtMax", event.cappedAtMax());

        return templateEngine.process("email/penalty/penalty-summary", context);
    }

    @Override
    public String renderDamageReportedEmail(DamageReportedEvent event) {
        log.debug("Rendering damage reported email for damage ID: {}", event.damageReportId());

        Context context = new Context();
        context.setVariable("rentalId", event.rentalId());
        context.setVariable("carLicensePlate", event.carLicensePlate());
        context.setVariable("reportDate", event.reportedAt().format(DATE_TIME_FORMATTER));
        context.setVariable("description", event.description());
        context.setVariable("severity", event.severity().name());

        return templateEngine.process("email/damage/damage-reported", context);
    }

    @Override
    public String renderDamageAssessedEmail(DamageAssessedEvent event) {
        log.debug("Rendering damage assessed email for damage ID: {}", event.damageReportId());

        Context context = new Context();
        context.setVariable("rentalId", event.rentalId());
        context.setVariable("carLicensePlate", event.carLicensePlate());
        context.setVariable("severity", event.severity().name());
        context.setVariable("repairCost", event.repairCostEstimate());
        context.setVariable("customerLiability", event.customerLiability());
        context.setVariable("currency", "TRY");
        context.setVariable("assessedAt", event.assessedAt().format(DATE_TIME_FORMATTER));

        return templateEngine.process("email/damage/damage-assessed", context);
    }

    @Override
    public String renderDamageChargedEmail(DamageChargedEvent event) {
        log.debug("Rendering damage charged email for damage ID: {}", event.damageReportId());

        Context context = new Context();
        context.setVariable("rentalId", event.rentalId());
        context.setVariable("transactionId", event.transactionId());
        context.setVariable("chargedAmount", event.chargeAmount());
        context.setVariable("currency", "TRY");
        context.setVariable("paymentDate", event.chargedAt().format(DATE_TIME_FORMATTER));

        return templateEngine.process("email/damage/damage-charged", context);
    }

    @Override
    public String renderDamageDisputedEmail(DamageDisputedEvent event) {
        log.debug("Rendering damage disputed email for damage ID: {}", event.damageReportId());

        Context context = new Context();
        context.setVariable("rentalId", event.rentalId());
        context.setVariable("currency", "TRY");
        context.setVariable("disputeReason", event.disputeReason());
        context.setVariable("disputeDate", event.disputedAt().format(DATE_TIME_FORMATTER));

        return templateEngine.process("email/damage/damage-disputed", context);
    }

    @Override
    public String renderDamageResolvedEmail(DamageResolvedEvent event) {
        log.debug("Rendering damage resolved email for damage ID: {}", event.damageReportId());

        Context context = new Context();
        context.setVariable("rentalId", event.rentalId());
        context.setVariable("adjustedCharge", event.adjustedLiability());
        context.setVariable("refundAmount", event.refundAmount());
        context.setVariable("currency", "TRY");
        context.setVariable("resolutionNotes", event.resolutionNotes());
        context.setVariable("resolvedAt", event.resolvedAt().format(DATE_TIME_FORMATTER));

        return templateEngine.process("email/damage/damage-resolved", context);
    }

    @Override
    public String renderDamageChargeFailedEmail(DamageChargeFailedEvent event) {
        log.debug("Rendering damage charge failed email for damage ID: {}", event.damageReportId());

        Context context = new Context();
        context.setVariable("rentalId", event.rentalId());
        context.setVariable("amount", event.amount());
        context.setVariable("currency", "TRY");
        context.setVariable("failureReason", event.failureReason());
        context.setVariable("failedAt", event.failedAt().format(DATE_TIME_FORMATTER));

        return templateEngine.process("email/damage/damage-charge-failed", context);
    }

    @Override
    public String renderPaymentRetryEmail(PaymentRetryRequestedEvent event) {
        log.debug("Rendering payment retry email for rental ID: {}", event.rentalId());

        Context context = new Context();
        context.setVariable("rentalId", event.rentalId());
        context.setVariable("carBrand", event.carBrand());
        context.setVariable("carModel", event.carModel());
        context.setVariable("payBy", event.payBy().format(DATE_FORMATTER));
        context.setVariable("amount", event.amount());
        context.setVariable("currency", event.currency().name());
        context.setVariable("payUrl", baseUrl + "/rentals/" + event.rentalId() + "/pay");

        return templateEngine.process("email/payment/payment-retry", context);
    }

    @Override
    public String renderPasswordResetEmail(String email, String resetLink) {
        log.debug("Rendering password reset email for: {}", email);

        Context context = new Context();
        context.setVariable("email", email);
        context.setVariable("resetLink", resetLink);
        context.setVariable("expirationHours", 1);

        return templateEngine.process("email/auth/password-reset", context);
    }
}

