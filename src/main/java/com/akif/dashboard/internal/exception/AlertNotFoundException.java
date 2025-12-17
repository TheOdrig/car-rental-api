package com.akif.dashboard.internal.exception;

import com.akif.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class AlertNotFoundException extends BaseException {

    public static final String ERROR_CODE = "ALERT_NOT_FOUND";

    public AlertNotFoundException(String message) {
        super(ERROR_CODE, message, HttpStatus.NOT_FOUND);
    }

    public AlertNotFoundException(Long id) {
        super(ERROR_CODE, "Alert not found with id: " + id, HttpStatus.NOT_FOUND);
    }
}
