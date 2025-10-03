package com.akif.exception;

public class CarOperationNotAllowedException extends BaseException {

    public static final String ERROR_CODE = "CAR_OPERATION_NOT_ALLOWED";

    public CarOperationNotAllowedException(String message) {
        super(ERROR_CODE, message);
    }

    public CarOperationNotAllowedException(String operation, String reason) {
        super(ERROR_CODE, String.format("Operation '%s' not allowed: %s", operation, reason));
    }
}
