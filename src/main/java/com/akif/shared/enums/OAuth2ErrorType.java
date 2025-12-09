package com.akif.shared.enums;

import lombok.Getter;

@Getter
public enum OAuth2ErrorType {

    AUTHORIZATION_DENIED("authorization_denied", "User denied authorization"),
    AUTHENTICATION_FAILED("authentication_failed", "Authentication failed"),
    INVALID_TOKEN("invalid_token", "Invalid or expired token"),
    PROVIDER_UNAVAILABLE("provider_unavailable", "Provider is temporarily unavailable"),
    EMAIL_REQUIRED("email_required", "Email is required but not provided"),
    INVALID_STATE("invalid_state", "Invalid state parameter - possible CSRF attack"),
    ACCOUNT_ALREADY_LINKED("account_already_linked", "This social account is already linked to another user"),
    SOCIAL_LOGIN_REQUIRED("social_login_required", "This account requires social login");

    private final String code;
    private final String message;

    OAuth2ErrorType(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
