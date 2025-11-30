package com.akif.exception;

import com.akif.enums.OAuth2ErrorType;

public class OAuth2ProviderException extends OAuth2AuthenticationException {

    public OAuth2ProviderException(OAuth2ErrorType errorType, String provider) {
        super(errorType, provider);
    }

    public OAuth2ProviderException(OAuth2ErrorType errorType, String provider, String customMessage) {
        super(errorType, provider, customMessage);
    }

    public OAuth2ProviderException(OAuth2ErrorType errorType, String provider, Throwable cause) {
        super(errorType, provider, cause);
    }

    public static OAuth2ProviderException providerUnavailable(String provider) {
        return new OAuth2ProviderException(OAuth2ErrorType.PROVIDER_UNAVAILABLE, provider);
    }

    public static OAuth2ProviderException authenticationFailed(String provider) {
        return new OAuth2ProviderException(OAuth2ErrorType.AUTHENTICATION_FAILED, provider);
    }

    public static OAuth2ProviderException invalidToken(String provider) {
        return new OAuth2ProviderException(OAuth2ErrorType.INVALID_TOKEN, provider);
    }

    public static OAuth2ProviderException emailRequired(String provider) {
        return new OAuth2ProviderException(OAuth2ErrorType.EMAIL_REQUIRED, provider);
    }
}
