package com.akif.dto.oauth2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2ErrorResponse {

    private String error;
    private String message;
    private String provider;

    @Builder.Default
    private Instant timestamp = Instant.now();

    public OAuth2ErrorResponse(String error, String message, String provider) {
        this.error = error;
        this.message = message;
        this.provider = provider;
        this.timestamp = Instant.now();
    }
}
