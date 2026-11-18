package com.akif.auth.unit;

import com.akif.auth.domain.enums.OAuth2ErrorType;
import com.akif.auth.internal.dto.oauth2.OAuth2ErrorResponse;
import com.akif.auth.internal.exception.OAuth2AuthenticationException;
import com.akif.auth.web.AuthExceptionHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthExceptionHandler Unit Tests")
class AuthExceptionHandlerTest {

    private final AuthExceptionHandler handler = new AuthExceptionHandler(
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));

    @Test
    @DisplayName("Should return OAuth2ErrorResponse for OAuth2AuthenticationException")
    void shouldReturnOAuth2ErrorResponse() {
        OAuth2AuthenticationException exception = new OAuth2AuthenticationException(
                OAuth2ErrorType.AUTHENTICATION_FAILED, "google");

        ResponseEntity<OAuth2ErrorResponse> response = handler.handleOAuth2AuthenticationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("authentication_failed");
        assertThat(response.getBody().provider()).isEqualTo("google");
    }

    @Test
    @DisplayName("Should handle OAuth2 provider unavailable error")
    void shouldHandleProviderUnavailable() {
        OAuth2AuthenticationException exception = new OAuth2AuthenticationException(
                OAuth2ErrorType.PROVIDER_UNAVAILABLE, "github");

        ResponseEntity<OAuth2ErrorResponse> response = handler.handleOAuth2AuthenticationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().error()).isEqualTo("provider_unavailable");
    }
}
