package com.akif.exception;

import org.springframework.http.HttpStatus;

public class InvalidPenaltyConfigException extends BaseException {
    
    public InvalidPenaltyConfigException(String message) {
        super("INVALID_PENALTY_CONFIG", message, HttpStatus.BAD_REQUEST);
    }
    
    public InvalidPenaltyConfigException(String message, Throwable cause) {
        super("INVALID_PENALTY_CONFIG", message, HttpStatus.BAD_REQUEST, cause);
    }
}
