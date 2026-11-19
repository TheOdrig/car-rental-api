package com.akif.notification.internal.service;

import java.time.Clock;
import com.akif.damage.api.*;
import com.akif.notification.internal.exception.EmailSendException;
import com.akif.notification.internal.dto.EmailMessage;
import com.akif.notification.internal.service.email.EmailSender;
import com.akif.notification.internal.service.email.EmailTemplateService;
import com.akif.rental.api.*;
import com.akif.notification.domain.EmailType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LateReturnEmailNotifier {

    private final Clock clock;
    private final EmailTemplateService templateService;
    private final EmailSender emailSender;

    public void sendGracePeriodWarning(GracePeriodWarningEvent event) {
        log.info("Queuing grace period warning email. RentalId: {}",
                event.rentalId());

        String body = templateService.renderGracePeriodWarningEmail(event);
        EmailMessage message = new EmailMessage(
                event.customerEmail(),
                "⚠️ Grace Period Warning - Rental #" + event.rentalId(),
                body,
                EmailType.RETURN_REMINDER,
                event.rentalId());

        try {
            emailSender.send(message);
            log.info("Grace period warning email sent successfully. RentalId: {}, Timestamp: {}",
                    event.rentalId(), Instant.now(clock));
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. RentalId: {}, Error: {}, SMTP Code: {}",
                    event.rentalId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }

    public void sendLateReturnNotification(LateReturnNotificationEvent event) {
        log.info("Queuing late return notification email. RentalId: {}",
                event.rentalId());

        String body = templateService.renderLateReturnNotificationEmail(event);
        EmailMessage message = new EmailMessage(
                event.customerEmail(),
                "🚨 Late Return - Penalty Applied - Rental #" + event.rentalId(),
                body,
                EmailType.RETURN_REMINDER,
                event.rentalId());

        try {
            emailSender.send(message);
            log.info("Late return notification email sent successfully. RentalId: {}, Timestamp: {}",
                    event.rentalId(), Instant.now(clock));
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. RentalId: {}, Error: {}, SMTP Code: {}",
                    event.rentalId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }

    public void sendSeverelyLateNotification(SeverelyLateNotificationEvent event) {
        log.info("Queuing severely late notification email. RentalId: {}",
                event.rentalId());

        String body = templateService.renderSeverelyLateNotificationEmail(event);
        EmailMessage message = new EmailMessage(
                event.customerEmail(),
                "🚨 URGENT: Severely Late Return - Rental #" + event.rentalId(),
                body,
                EmailType.RETURN_REMINDER,
                event.rentalId());

        try {
            emailSender.send(message);
            log.info("Severely late notification email sent successfully. RentalId: {}, Timestamp: {}",
                    event.rentalId(), Instant.now(clock));
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. RentalId: {}, Error: {}, SMTP Code: {}",
                    event.rentalId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }

    public void sendPenaltySummary(PenaltySummaryEvent event) {
        log.info("Queuing penalty summary email. RentalId: {}",
                event.rentalId());

        String body = templateService.renderPenaltySummaryEmail(event);
        EmailMessage message = new EmailMessage(
                event.customerEmail(),
                "📋 Late Return Penalty Summary - Rental #" + event.rentalId(),
                body,
                EmailType.PAYMENT_RECEIPT,
                event.rentalId());

        try {
            emailSender.send(message);
            log.info("Penalty summary email sent successfully. RentalId: {}, Timestamp: {}",
                    event.rentalId(), Instant.now(clock));
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. RentalId: {}, Error: {}, SMTP Code: {}",
                    event.rentalId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }
}
