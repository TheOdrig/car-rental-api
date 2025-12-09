package com.akif.shared.handler;

import com.akif.auth.internal.oauth2.dto.response.OAuth2ErrorResponse;
import com.akif.dto.response.ErrorResponseDto;
import com.akif.shared.exception.BaseException;
import com.akif.exception.OAuth2AuthenticationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String ERROR_CODE_AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
    private static final String ERROR_CODE_ACCESS_DENIED = "ACCESS_DENIED";
    private static final String ERROR_CODE_VALIDATION_FAILED = "VALIDATION_FAILED";
    private static final String ERROR_CODE_CONSTRAINT_VIOLATION = "CONSTRAINT_VIOLATION";
    private static final String ERROR_CODE_INVALID_JSON = "INVALID_JSON";
    private static final String ERROR_CODE_MISSING_PARAMETER = "MISSING_PARAMETER";
    private static final String ERROR_CODE_INVALID_PARAMETER_TYPE = "INVALID_PARAMETER_TYPE";
    private static final String ERROR_CODE_ENDPOINT_NOT_FOUND = "ENDPOINT_NOT_FOUND";
    private static final String ERROR_CODE_INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";


    @ExceptionHandler(OAuth2AuthenticationException.class)
    public ResponseEntity<OAuth2ErrorResponse> handleOAuth2AuthenticationException(OAuth2AuthenticationException ex) {
        log.error("[{}] OAuth2 error for provider '{}': {}", 
                ex.getErrorType().getCode(), ex.getProvider(), ex.getMessage());

        OAuth2ErrorResponse errorResponse = OAuth2ErrorResponse.of(
                ex.getErrorType().getCode(),
                ex.getMessage(),
                ex.getProvider()
        );
        
        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }


    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponseDto> handleBaseException(BaseException ex, WebRequest request) {
        log.error("[{}] {}", ex.getErrorCode(), ex.getMessage());
        return buildErrorResponse(ex.getErrorCode(), ex.getMessage(), ex.getHttpStatus(), request);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        
        log.error("[{}] Validation failed: {}", ERROR_CODE_VALIDATION_FAILED, fieldErrors);
        return buildErrorResponse(ERROR_CODE_VALIDATION_FAILED, "Validation failed", 
                HttpStatus.BAD_REQUEST, request, fieldErrors);
    }


    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        
        Map<String, String> validationErrors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        this::extractPropertyName,
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing
                ));
        
        log.error("[{}] Constraint violation: {}", ERROR_CODE_CONSTRAINT_VIOLATION, validationErrors);
        return buildErrorResponse(ERROR_CODE_CONSTRAINT_VIOLATION, "Constraint violation occurred", 
                HttpStatus.BAD_REQUEST, request, validationErrors);
    }


    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        log.error("[{}] Authentication failed: {}", ERROR_CODE_AUTHENTICATION_FAILED, ex.getMessage());
        return buildErrorResponse(ERROR_CODE_AUTHENTICATION_FAILED, 
                "Authentication failed. Please check your credentials.", 
                HttpStatus.UNAUTHORIZED, request);
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        log.error("[{}] Access denied: {}", ERROR_CODE_ACCESS_DENIED, ex.getMessage());
        return buildErrorResponse(ERROR_CODE_ACCESS_DENIED, 
                "Access denied. You don't have permission to perform this action.", 
                HttpStatus.FORBIDDEN, request);
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, WebRequest request) {
        log.error("[{}] Invalid JSON: {}", ERROR_CODE_INVALID_JSON, ex.getMessage());
        return buildErrorResponse(ERROR_CODE_INVALID_JSON, 
                "Invalid JSON format in request body", 
                HttpStatus.BAD_REQUEST, request);
    }


    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDto> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex, WebRequest request) {
        log.error("[{}] Missing parameter: {}", ERROR_CODE_MISSING_PARAMETER, ex.getParameterName());
        return buildErrorResponse(ERROR_CODE_MISSING_PARAMETER, 
                String.format("Required parameter '%s' is missing", ex.getParameterName()), 
                HttpStatus.BAD_REQUEST, request);
    }


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        log.error("[{}] Type mismatch for '{}': expected {}", 
                ERROR_CODE_INVALID_PARAMETER_TYPE, ex.getName(), expectedType);
        return buildErrorResponse(ERROR_CODE_INVALID_PARAMETER_TYPE, 
                String.format("Invalid parameter type for '%s'. Expected: %s", ex.getName(), expectedType), 
                HttpStatus.BAD_REQUEST, request);
    }


    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ErrorResponseDto> handleDateTimeParseException(
            DateTimeParseException ex, WebRequest request) {
        log.error("[{}] Invalid date format: {}", ERROR_CODE_INVALID_PARAMETER_TYPE, ex.getMessage());
        return buildErrorResponse(ERROR_CODE_INVALID_PARAMETER_TYPE, 
                "Invalid date format. Expected format: yyyy-MM-dd", 
                HttpStatus.BAD_REQUEST, request);
    }


    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNoHandlerFoundException(
            NoHandlerFoundException ex, WebRequest request) {
        log.error("[{}] Endpoint not found: {} {}", 
                ERROR_CODE_ENDPOINT_NOT_FOUND, ex.getHttpMethod(), ex.getRequestURL());
        return buildErrorResponse(ERROR_CODE_ENDPOINT_NOT_FOUND, 
                "The requested endpoint was not found", 
                HttpStatus.NOT_FOUND, request);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(Exception ex, WebRequest request) {
        log.error("[{}] Unexpected error: {}", ERROR_CODE_INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
        return buildErrorResponse(ERROR_CODE_INTERNAL_SERVER_ERROR, 
                "An unexpected error occurred. Please try again later.", 
                HttpStatus.INTERNAL_SERVER_ERROR, request);
    }


    private ResponseEntity<ErrorResponseDto> buildErrorResponse(
            String errorCode, String message, HttpStatus status, WebRequest request) {
        return buildErrorResponse(errorCode, message, status, request, null);
    }

    private ResponseEntity<ErrorResponseDto> buildErrorResponse(
            String errorCode, String message, HttpStatus status, WebRequest request, 
            Map<String, String> validationErrors) {
        
        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .path(extractPath(request))
                .traceId(generateTraceId())
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }

    private String extractPropertyName(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        return lastDot >= 0 ? path.substring(lastDot + 1) : path;
    }

    public String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    public String generateTraceId() {
        String correlationId = MDC.get("correlationId");
        return correlationId != null ? correlationId : UUID.randomUUID().toString();
    }
}