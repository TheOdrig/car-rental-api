package com.akif.auth.internal.exception;

import com.akif.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends BaseException {

    public static final String ERROR_CODE = "USER_NOT_FOUND";

    public UserNotFoundException(String message) {
        super(ERROR_CODE, message, HttpStatus.NOT_FOUND);
    }

    public UserNotFoundException(Long id) {
        super(ERROR_CODE, "User not found with id: " + id, HttpStatus.NOT_FOUND);
    }
}
