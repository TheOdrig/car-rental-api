package com.akif.auth.internal.oauth2.dto.response;

import java.time.Instant;

public record OAuth2ErrorResponse(
        String error,
        String message,
        String provider,
        Instant timestamp
) {
    public OAuth2ErrorResponse {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public static OAuth2ErrorResponse of(String error, String message, String provider) {
        return new OAuth2ErrorResponse(error, message, provider, Instant.now());
    }
}

