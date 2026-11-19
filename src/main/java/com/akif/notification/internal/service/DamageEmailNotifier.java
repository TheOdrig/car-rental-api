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
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DamageEmailNotifier {

    private final Clock clock;
    private final EmailTemplateService templateService;
    private final EmailSender emailSender;

    public void sendDamageReportedNotification(DamageReportedEvent event) {
        String customerEmail = event.customerEmail();
        Long damageId = event.damageReportId();
        Long rentalId = event.rentalId();

        log.info("Queuing damage reported email. DamageId: {}", damageId);

        String body = templateService.renderDamageReportedEmail(event);
        EmailMessage message = new EmailMessage(
                customerEmail,
                "⚠️ Damage Report Received - Rental #" + rentalId,
                body,
                EmailType.DAMAGE_REPORTED,
                rentalId);

        try {
            emailSender.send(message);
            log.info("Damage reported email sent successfully. DamageId: {}", damageId);
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. DamageId: {}, Error: {}", damageId, e.getMessage());
            throw e;
        }
    }

    public void sendDamageAssessedNotification(DamageAssessedEvent event) {
        String customerEmail = event.customerEmail();
        Long damageId = event.damageReportId();
        Long rentalId = event.rentalId();

        log.info("Queuing damage assessed email. DamageId: {}", damageId);

        String body = templateService.renderDamageAssessedEmail(event);
        EmailMessage message = new EmailMessage(
                customerEmail,
                "📋 Damage Assessment Complete - Rental #" + rentalId,
                body,
                EmailType.DAMAGE_ASSESSED,
                rentalId);

        try {
            emailSender.send(message);
            log.info("Damage assessed email sent successfully. DamageId: {}", damageId);
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. DamageId: {}, Error: {}", damageId, e.getMessage());
            throw e;
        }
    }

    public void sendDamageChargedNotification(DamageChargedEvent event) {
        String customerEmail = event.customerEmail();
        Long damageId = event.damageReportId();
        Long rentalId = event.rentalId();

        log.info("Queuing damage charged email. DamageId: {}", damageId);

        String body = templateService.renderDamageChargedEmail(event);
        EmailMessage message = new EmailMessage(
                customerEmail,
                "✅ Damage Charge Processed - Rental #" + rentalId,
                body,
                EmailType.DAMAGE_CHARGED,
                rentalId);

        try {
            emailSender.send(message);
            log.info("Damage charged email sent successfully. DamageId: {}", damageId);
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. DamageId: {}, Error: {}", damageId, e.getMessage());
            throw e;
        }
    }

    public void sendDamageDisputedNotification(DamageDisputedEvent event) {
        String customerEmail = event.customerEmail();
        Long damageId = event.damageReportId();
        Long rentalId = event.rentalId();

        log.info("Queuing damage disputed email. DamageId: {}", damageId);

        String body = templateService.renderDamageDisputedEmail(event);
        EmailMessage message = new EmailMessage(
                customerEmail,
                "📝 Dispute Received - Rental #" + rentalId,
                body,
                EmailType.DAMAGE_DISPUTED,
                rentalId);

        try {
            emailSender.send(message);
            log.info("Damage disputed email sent successfully. DamageId: {}", damageId);
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. DamageId: {}, Error: {}", damageId, e.getMessage());
            throw e;
        }
    }

    public void sendDamageResolvedNotification(DamageResolvedEvent event) {
        String customerEmail = event.customerEmail();
        Long damageId = event.damageReportId();
        Long rentalId = event.rentalId();

        log.info("Queuing damage resolved email. DamageId: {}", damageId);

        String body = templateService.renderDamageResolvedEmail(event);
        EmailMessage message = new EmailMessage(
                customerEmail,
                "✅ Dispute Resolved - Rental #" + rentalId,
                body,
                EmailType.DAMAGE_RESOLVED,
                rentalId);

        try {
            emailSender.send(message);
            log.info("Damage resolved email sent successfully. DamageId: {}", damageId);
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. DamageId: {}, Error: {}", damageId, e.getMessage());
            throw e;
        }
    }

    public void sendDamageChargeFailedNotification(DamageChargeFailedEvent event) {
        String customerEmail = event.customerEmail();
        Long damageId = event.damageReportId();
        Long rentalId = event.rentalId();

        log.info("Queuing damage charge failed email. DamageId: {}", damageId);

        String body = templateService.renderDamageChargeFailedEmail(event);
        EmailMessage message = new EmailMessage(
                customerEmail,
                "🚨 Payment Failed - Damage Charge - Rental #" + rentalId,
                body,
                EmailType.DAMAGE_CHARGE_FAILED,
                rentalId);

        try {
            emailSender.send(message);
            log.info("Damage charge failed email sent successfully. DamageId: {}", damageId);
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. DamageId: {}, Error: {}", damageId, e.getMessage());
            throw e;
        }
    }
}
