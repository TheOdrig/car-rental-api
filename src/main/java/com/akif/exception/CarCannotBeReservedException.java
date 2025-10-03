package com.akif.exception;

public class CarCannotBeReservedException extends BaseException{

    public static final String ERROR_CODE = "CAR_CANNOT_BE_RESERVED";

    public CarCannotBeReservedException(String message) {
        super(ERROR_CODE, message);
    }

    public CarCannotBeReservedException(String reason, String currentStatus) {
        super(ERROR_CODE, String.format("Car cannot be reserved: %s. Current status: %s", reason, currentStatus));
    }
}
