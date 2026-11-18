package com.akif.notification.internal.exception;

import com.akif.shared.exception.ErrorCode;
import com.akif.notification.domain.EmailType;
import com.akif.shared.exception.BaseException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class EmailSendException extends BaseException {

    private final String recipient;
    private final EmailType emailType;
    private final String smtpErrorCode;

    public EmailSendException(String recipient, EmailType emailType, String smtpErrorCode, String message) {
        super(ErrorCode.EMAIL_SEND_FAILED.name(), message, HttpStatus.INTERNAL_SERVER_ERROR);
        this.recipient = recipient;
        this.emailType = emailType;
        this.smtpErrorCode = smtpErrorCode;
    }

    public EmailSendException(String recipient, EmailType emailType, String smtpErrorCode, String message, Throwable cause) {
        super(ErrorCode.EMAIL_SEND_FAILED.name(), message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
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
