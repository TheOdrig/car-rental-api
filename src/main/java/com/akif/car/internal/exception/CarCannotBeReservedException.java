package com.akif.car.internal.exception;

import com.akif.shared.exception.ErrorCode;
import com.akif.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class CarCannotBeReservedException extends BaseException {

    public static final String ERROR_CODE = ErrorCode.CAR_CANNOT_BE_RESERVED.name();

    public CarCannotBeReservedException(String message) {
        super(ERROR_CODE, message, HttpStatus.BAD_REQUEST);
    }

    public CarCannotBeReservedException(String message, HttpStatus httpStatus) {
        super(ERROR_CODE, message, httpStatus);
    }
}
