package com.akif.exception;


public class CarCannotBeSoldException extends BaseException {

    public static final String ERROR_CODE = "CAR_CANNOT_BE_SOLD";

    public CarCannotBeSoldException(String message) {
        super(ERROR_CODE, message);
    }

    public CarCannotBeSoldException(String reason, String currentStatus) {
        super(ERROR_CODE, String.format("Car cannot be sold: %s. Current status: %s", reason, currentStatus));
    }
}
