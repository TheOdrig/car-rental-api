package com.akif.auth.web;

import com.akif.auth.internal.dto.oauth2.OAuth2ErrorResponse;
import com.akif.auth.internal.exception.OAuth2AuthenticationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class AuthExceptionHandler {

    @ExceptionHandler(OAuth2AuthenticationException.class)
    public ResponseEntity<OAuth2ErrorResponse> handleOAuth2AuthenticationException(
            OAuth2AuthenticationException ex) {
        log.error("[{}] OAuth2 error for provider '{}': {}",
                ex.getErrorType().getCode(), ex.getProvider(), ex.getMessage());

        OAuth2ErrorResponse errorResponse = OAuth2ErrorResponse.of(
                ex.getErrorType().getCode(),
                ex.getMessage(),
                ex.getProvider());

        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }
}
