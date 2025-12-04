package com.akif.exception;

import org.springframework.http.HttpStatus;

public class PenaltyWaiverException extends BaseException {

    public PenaltyWaiverException(String message) {
        super("PENALTY_WAIVER_ERROR", message, HttpStatus.BAD_REQUEST);
    }

    public PenaltyWaiverException(String message, Throwable cause) {
        super("PENALTY_WAIVER_ERROR", message, HttpStatus.BAD_REQUEST, cause);
    }
}
