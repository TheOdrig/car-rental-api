package com.akif.notification.internal.email.impl;

import com.akif.notification.internal.config.EmailProperties;
import com.akif.dto.email.EmailMessage;
import com.akif.exception.EmailSendException;
import com.akif.notification.internal.email.IEmailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class SendGridEmailSender implements IEmailSender {

    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;

    @Override
    public void send(EmailMessage message) throws EmailSendException {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(emailProperties.getFrom());
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.body(), true);

            mailSender.send(mimeMessage);

            log.info("Email sent successfully. Type: {}, To: {}, ReferenceId: {}",
                    message.type(), message.to(), message.referenceId());

        } catch (MessagingException e) {
            log.error("Failed to create email message. Type: {}, To: {}, ReferenceId: {}",
                    message.type(), message.to(), message.referenceId(), e);
            throw new EmailSendException(
                    message.to(),
                    message.type(),
                    "MESSAGING_ERROR",
                    "Failed to create email message: " + e.getMessage(),
                    e
            );
        } catch (MailException e) {
            log.error("Failed to send email via SMTP. Type: {}, To: {}, ReferenceId: {}",
                    message.type(), message.to(), message.referenceId(), e);
            throw new EmailSendException(
                    message.to(),
                    message.type(),
                    extractSmtpErrorCode(e),
                    "Failed to send email: " + e.getMessage(),
                    e
            );
        }
    }

    private String extractSmtpErrorCode(MailException e) {
        String message = e.getMessage();
        if (message != null && message.contains("550")) {
            return "550";
        } else if (message != null && message.contains("421")) {
            return "421";
        } else if (message != null && message.contains("451")) {
            return "451";
        }
        return "UNKNOWN";
    }
}
