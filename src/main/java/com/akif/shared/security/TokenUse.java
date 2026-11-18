package com.akif.shared.security;

public enum TokenUse {
    ACCESS("access");

    public static final String CLAIM = "token_use";

    private final String claimValue;

    TokenUse(String claimValue) {
        this.claimValue = claimValue;
    }

    public String claimValue() {
        return claimValue;
    }
}
