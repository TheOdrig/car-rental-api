package com.akif.service.email.impl;

import com.akif.dto.email.EmailMessage;
import com.akif.exception.EmailSendException;
import com.akif.service.email.IEmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!prod")
@Slf4j
public class MockEmailSender implements IEmailSender {

    @Override
    public void send(EmailMessage message) throws EmailSendException {
        log.info("========== MOCK EMAIL ==========");
        log.info("Type: {}", message.type());
        log.info("To: {}", message.to());
        log.info("Subject: {}", message.subject());
        log.info("ReferenceId: {}", message.referenceId());
        log.info("Body: {}", message.body());
        log.info("================================");
    }
}
