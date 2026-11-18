package com.akif.car.internal.exception;

import com.akif.shared.exception.ErrorCode;
import com.akif.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class CarOperationNotAllowedException extends BaseException {

    public static final String ERROR_CODE = ErrorCode.CAR_OPERATION_NOT_ALLOWED.name();

    public CarOperationNotAllowedException(String message) {
        super(ERROR_CODE, message, HttpStatus.FORBIDDEN);
    }

    public CarOperationNotAllowedException(String message, HttpStatus httpStatus) {
        super(ERROR_CODE, message, httpStatus);
    }
}
