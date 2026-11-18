package com.akif.rental.internal.exception;

import com.akif.shared.exception.BaseException;
import com.akif.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class RentalCancelNotAllowedException extends BaseException {

    public static final String ERROR_CODE = ErrorCode.RENTAL_CANCEL_NOT_ALLOWED.name();

    public RentalCancelNotAllowedException(String status) {
        super(ERROR_CODE,
                "A rental in status " + status + " cannot be cancelled."
                        + ("IN_USE".equals(status) ? " It ends only through the return flow." : ""),
                HttpStatus.BAD_REQUEST);
    }
}
