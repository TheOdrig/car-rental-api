package com.akif.service.email;

import com.akif.dto.email.EmailMessage;
import com.akif.exception.EmailSendException;

public interface IEmailSender {

    void send(EmailMessage message) throws EmailSendException;
}
