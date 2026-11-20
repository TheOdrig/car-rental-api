package com.akif.auth.internal.exception;

import com.akif.auth.domain.exception.OAuth2AuthenticationException;
import com.akif.auth.domain.enums.OAuth2ErrorType;


public class AccountAlreadyLinkedException extends OAuth2AuthenticationException {

    public AccountAlreadyLinkedException(String provider) {
        super(OAuth2ErrorType.ACCOUNT_ALREADY_LINKED, provider);
    }

    public AccountAlreadyLinkedException(String provider, String customMessage) {
        super(OAuth2ErrorType.ACCOUNT_ALREADY_LINKED, provider, customMessage);
    }
}
