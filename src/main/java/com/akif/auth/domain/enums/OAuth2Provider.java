package com.akif.auth.domain.enums;

import com.akif.auth.domain.exception.OAuth2AuthenticationException;
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
            throw new OAuth2AuthenticationException(OAuth2ErrorType.PROVIDER_NOT_SUPPORTED, value);
        }

        for (OAuth2Provider provider : values()) {
            if (provider.value.equalsIgnoreCase(value.trim())) {
                return provider;
            }
        }
        throw new OAuth2AuthenticationException(OAuth2ErrorType.PROVIDER_NOT_SUPPORTED, value);
    }

    @Override
    public String toString() {
        return value;
    }
}
