package com.akif.notification.internal.service.email;

import com.akif.notification.internal.dto.EmailMessage;
import com.akif.notification.internal.exception.EmailSendException;

public interface EmailSender {

    void send(EmailMessage message) throws EmailSendException;
}
