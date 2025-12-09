package com.akif.shared.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum OAuth2Provider {
    GOOGLE("google"),
    GITHUB("github");

    private final String value;

    OAuth2Provider(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static OAuth2Provider fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider value cannot be null or empty");
        }

        for (OAuth2Provider provider : values()) {
            if (provider.value.equalsIgnoreCase(value.trim())) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Invalid OAuth2 provider: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
