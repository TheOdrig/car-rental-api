package com.akif.exception;

import com.akif.enums.OAuth2ErrorType;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class OAuth2AuthenticationException extends BaseException {

    private final OAuth2ErrorType errorType;
    private final String provider;

    public OAuth2AuthenticationException(OAuth2ErrorType errorType) {
        super(errorType.getCode(), errorType.getMessage(), mapErrorTypeToHttpStatus(errorType));
        this.errorType = errorType;
        this.provider = null;
    }

    public OAuth2AuthenticationException(OAuth2ErrorType errorType, String provider) {
        super(errorType.getCode(), errorType.getMessage(), mapErrorTypeToHttpStatus(errorType));
        this.errorType = errorType;
        this.provider = provider;
    }

    public OAuth2AuthenticationException(OAuth2ErrorType errorType, String provider, String customMessage) {
        super(errorType.getCode(), customMessage, mapErrorTypeToHttpStatus(errorType));
        this.errorType = errorType;
        this.provider = provider;
    }

    public OAuth2AuthenticationException(OAuth2ErrorType errorType, String provider, Throwable cause) {
        super(errorType.getCode(), errorType.getMessage(), mapErrorTypeToHttpStatus(errorType), cause);
        this.errorType = errorType;
        this.provider = provider;
    }

    private static HttpStatus mapErrorTypeToHttpStatus(OAuth2ErrorType errorType) {
        return switch (errorType) {
            case AUTHORIZATION_DENIED, AUTHENTICATION_FAILED, INVALID_TOKEN, SOCIAL_LOGIN_REQUIRED -> 
                HttpStatus.UNAUTHORIZED;
            case PROVIDER_UNAVAILABLE -> 
                HttpStatus.SERVICE_UNAVAILABLE;
            case EMAIL_REQUIRED, INVALID_STATE -> 
                HttpStatus.BAD_REQUEST;
            case ACCOUNT_ALREADY_LINKED -> 
                HttpStatus.CONFLICT;
        };
    }
}
