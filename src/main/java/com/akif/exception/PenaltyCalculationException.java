package com.akif.exception;

import org.springframework.http.HttpStatus;

public class PenaltyCalculationException extends BaseException {
    
    public PenaltyCalculationException(String message) {
        super("PENALTY_CALCULATION_ERROR", message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    public PenaltyCalculationException(String message, Throwable cause) {
        super("PENALTY_CALCULATION_ERROR", message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
