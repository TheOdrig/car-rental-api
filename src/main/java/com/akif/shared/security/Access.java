package com.akif.shared.security;

public enum Access {
    PUBLIC,
    AUTHENTICATED,
    USER,
    ADMIN;

    public boolean requiresAuthentication() {
        return this != PUBLIC;
    }
}
