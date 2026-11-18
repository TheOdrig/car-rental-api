package com.akif.rental.internal.exception;

import com.akif.shared.exception.ErrorCode;
import com.akif.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class PenaltyCalculationException extends BaseException {
    
    public PenaltyCalculationException(String message) {
        super(ErrorCode.PENALTY_CALCULATION_ERROR.name(), message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    public PenaltyCalculationException(String message, Throwable cause) {
        super(ErrorCode.PENALTY_CALCULATION_ERROR.name(), message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
