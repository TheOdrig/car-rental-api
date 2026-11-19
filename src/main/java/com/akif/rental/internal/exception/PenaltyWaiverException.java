package com.akif.rental.internal.exception;

import com.akif.shared.exception.ErrorCode;
import com.akif.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class PenaltyWaiverException extends BaseException {
    
    public PenaltyWaiverException(String message) {
        super(ErrorCode.PENALTY_WAIVER_ERROR.name(), message, HttpStatus.BAD_REQUEST);
    }
    
    public PenaltyWaiverException(String message, String field) {
        super(ErrorCode.PENALTY_WAIVER_ERROR.name(), message, HttpStatus.BAD_REQUEST);
        setFieldError(field, message);
    }

    public PenaltyWaiverException(String message, Throwable cause) {
        super(ErrorCode.PENALTY_WAIVER_ERROR.name(), message, HttpStatus.BAD_REQUEST, cause);
    }
}
