package com.akif.exception;

import com.akif.enums.OAuth2ErrorType;

public class SocialLoginRequiredException extends OAuth2AuthenticationException {

    public SocialLoginRequiredException(String provider) {
        super(OAuth2ErrorType.SOCIAL_LOGIN_REQUIRED, provider);
    }

    public SocialLoginRequiredException(String provider, String customMessage) {
        super(OAuth2ErrorType.SOCIAL_LOGIN_REQUIRED, provider, customMessage);
    }
}
