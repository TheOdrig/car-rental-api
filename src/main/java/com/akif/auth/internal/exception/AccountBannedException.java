package com.akif.auth.internal.exception;

import com.akif.shared.exception.BaseException;
import com.akif.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class AccountBannedException extends BaseException {

    public AccountBannedException() {
        super(ErrorCode.ACCOUNT_BANNED.name(), "This account has been suspended. Please contact support.",
                HttpStatus.FORBIDDEN);
    }
}
