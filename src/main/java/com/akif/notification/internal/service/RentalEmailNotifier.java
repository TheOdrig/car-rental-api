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
public class RentalEmailNotifier {

    private final Clock clock;
    private final EmailTemplateService templateService;
    private final EmailSender emailSender;

    public void sendRentalConfirmation(RentalConfirmedEvent event) {
        log.info("Queuing rental confirmation email. RentalId: {}",
                event.rentalId());

        String body = templateService.renderConfirmationEmail(event);
        EmailMessage message = new EmailMessage(
                event.customerEmail(),
                "Rental Confirmation - #" + event.rentalId(),
                body,
                EmailType.RENTAL_CONFIRMATION,
                event.rentalId());

        try {
            emailSender.send(message);
            log.info("Rental confirmation email sent successfully. RentalId: {}, Timestamp: {}",
                    event.rentalId(), Instant.now(clock));
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. RentalId: {}, Error: {}, SMTP Code: {}",
                    event.rentalId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }

    public void sendPickupReminder(PickupReminderEvent event) {
        log.info("Queuing pickup reminder email. RentalId: {}",
                event.rentalId());

        String body = templateService.renderPickupReminderEmail(event);
        EmailMessage message = new EmailMessage(
                event.customerEmail(),
                "Pickup Reminder - Rental #" + event.rentalId(),
                body,
                EmailType.PICKUP_REMINDER,
                event.rentalId());

        try {
            emailSender.send(message);
            log.info("Pickup reminder email sent successfully. RentalId: {}, Timestamp: {}",
                    event.rentalId(), Instant.now(clock));
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. RentalId: {}, Error: {}, SMTP Code: {}",
                    event.rentalId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }

    public void sendReturnReminder(ReturnReminderEvent event) {
        log.info("Queuing return reminder email. RentalId: {}",
                event.rentalId());

        String body = templateService.renderReturnReminderEmail(event);
        EmailMessage message = new EmailMessage(
                event.customerEmail(),
                "Return Reminder - Rental #" + event.rentalId(),
                body,
                EmailType.RETURN_REMINDER,
                event.rentalId());

        try {
            emailSender.send(message);
            log.info("Return reminder email sent successfully. RentalId: {}, Timestamp: {}",
                    event.rentalId(), Instant.now(clock));
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. RentalId: {}, Error: {}, SMTP Code: {}",
                    event.rentalId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }

    public void sendCancellationConfirmation(RentalCancelledEvent event) {
        log.info("Queuing cancellation confirmation email. RentalId: {}",
                event.rentalId());

        String body = templateService.renderCancellationEmail(event);
        EmailMessage message = new EmailMessage(
                event.customerEmail(),
                "Cancellation Confirmation - Rental #" + event.rentalId(),
                body,
                EmailType.CANCELLATION_CONFIRMATION,
                event.rentalId());

        try {
            emailSender.send(message);
            log.info("Cancellation confirmation email sent successfully. RentalId: {}, Timestamp: {}",
                    event.rentalId(), Instant.now(clock));
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. RentalId: {}, Error: {}, SMTP Code: {}",
                    event.rentalId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }
}
