package com.akif.shared.security.ratelimit;

public enum RateLimitCategory {
    LOGIN,
    REGISTER,
    REFRESH,
    PASSWORD_RESET,
    OAUTH2_CALLBACK,
    RENTAL_REQUEST,
    PAYMENT_INTENT,
    LOGIN_USERNAME,
    LOGIN_USERNAME_IP,
    PASSWORD_RESET_USERNAME,
    PASSWORD_RESET_EMAIL_IP
}
