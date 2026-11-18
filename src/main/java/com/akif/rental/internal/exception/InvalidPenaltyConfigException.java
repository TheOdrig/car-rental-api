package com.akif.rental.internal.exception;

import com.akif.shared.exception.ErrorCode;
import com.akif.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidPenaltyConfigException extends BaseException {
    
    public InvalidPenaltyConfigException(String message) {
        super(ErrorCode.INVALID_PENALTY_CONFIG.name(), message, HttpStatus.BAD_REQUEST);
    }
    
    public InvalidPenaltyConfigException(String message, Throwable cause) {
        super(ErrorCode.INVALID_PENALTY_CONFIG.name(), message, HttpStatus.BAD_REQUEST, cause);
    }
}
