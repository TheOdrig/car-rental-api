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
public class AccountEmailNotifier {

    private final Clock clock;
    private final EmailTemplateService templateService;
    private final EmailSender emailSender;

    public void sendPasswordResetEmail(String email, String resetLink) {
        log.info("Queuing password reset email");

        String body = templateService.renderPasswordResetEmail(email, resetLink);
        EmailMessage message = new EmailMessage(
                email,
                "🔐 Password Reset Request",
                body,
                EmailType.PASSWORD_RESET,
                null);

        try {
            emailSender.send(message);
            log.info("Password reset email sent successfully");
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. Error: {}", e.getMessage());
            throw e;
        }
    }
}
