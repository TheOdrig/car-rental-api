package com.akif.exception;

public class InvalidStatusTransitionException extends BaseException {

    public static final String ERROR_CODE = "INVALID_STATUS_TRANSITION";

    public InvalidStatusTransitionException(String message) {
        super(ERROR_CODE, message);
    }

    public InvalidStatusTransitionException(String fromStatus, String toStatus) {
        super(ERROR_CODE, String.format("Invalid status transition from %s to %s", fromStatus, toStatus));
    }
}
