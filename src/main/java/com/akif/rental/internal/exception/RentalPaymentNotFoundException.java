package com.akif.rental.internal.exception;

import com.akif.shared.exception.ErrorCode;
import com.akif.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class RentalPaymentNotFoundException extends BaseException {

    public static final String ERROR_CODE = ErrorCode.PAYMENT_NOT_FOUND.name();

    public RentalPaymentNotFoundException(Long rentalId) {
        super(ERROR_CODE, "Payment not found for rental: " + rentalId, HttpStatus.NOT_FOUND);
    }
}
