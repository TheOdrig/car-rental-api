package com.akif.exception;

import com.akif.shared.enums.EmailType;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class EmailSendException extends BaseException {

    private final String recipient;
    private final EmailType emailType;
    private final String smtpErrorCode;

    public EmailSendException(String recipient, EmailType emailType, String smtpErrorCode, String message) {
        super("EMAIL_SEND_FAILED", message, HttpStatus.INTERNAL_SERVER_ERROR);
        this.recipient = recipient;
        this.emailType = emailType;
        this.smtpErrorCode = smtpErrorCode;
    }

    public EmailSendException(String recipient, EmailType emailType, String smtpErrorCode, String message, Throwable cause) {
        super("EMAIL_SEND_FAILED", message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
        this.recipient = recipient;
        this.emailType = emailType;
        this.smtpErrorCode = smtpErrorCode;
    }

    @Override
    public String toString() {
        return String.format("EmailSendException[recipient=%s, emailType=%s, smtpErrorCode=%s, message=%s]",
                recipient, emailType, smtpErrorCode, getErrorMessage());
    }
}
