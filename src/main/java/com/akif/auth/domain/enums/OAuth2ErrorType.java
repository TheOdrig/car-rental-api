package com.akif.auth.domain.enums;

import com.akif.shared.exception.ErrorCode;
import lombok.Getter;

@Getter
public enum OAuth2ErrorType {

    AUTHORIZATION_DENIED(ErrorCode.OAUTH2_AUTHORIZATION_DENIED, "User denied authorization"),
    AUTHENTICATION_FAILED(ErrorCode.OAUTH2_AUTHENTICATION_FAILED, "Authentication failed"),
    INVALID_TOKEN(ErrorCode.OAUTH2_INVALID_TOKEN, "Invalid or expired token"),
    PROVIDER_UNAVAILABLE(ErrorCode.OAUTH2_PROVIDER_UNAVAILABLE, "Provider is temporarily unavailable"),
    EMAIL_REQUIRED(ErrorCode.OAUTH2_EMAIL_REQUIRED, "Email is required but not provided"),
    INVALID_STATE(ErrorCode.OAUTH2_INVALID_STATE, "Invalid state parameter - possible CSRF attack"),
    ACCOUNT_ALREADY_LINKED(ErrorCode.OAUTH2_ACCOUNT_ALREADY_LINKED,
            "This social account is already linked to another user"),
    SOCIAL_LOGIN_REQUIRED(ErrorCode.OAUTH2_SOCIAL_LOGIN_REQUIRED, "This account requires social login"),
    LOCAL_ACCOUNT_UNVERIFIED(ErrorCode.OAUTH2_LOCAL_ACCOUNT_UNVERIFIED,
            "An account with this email exists but its email is not verified. "
                    + "Log in with your password and link this provider with /api/v1/oauth2/link."),
    PROVIDER_NOT_SUPPORTED(ErrorCode.OAUTH2_PROVIDER_NOT_SUPPORTED, "OAuth2 provider is not supported");

    private final ErrorCode errorCode;
    private final String message;

    OAuth2ErrorType(ErrorCode errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    public String getCode() {
        return errorCode.name();
    }
}
