package com.akif.handler;

import com.akif.dto.oauth2.OAuth2ErrorResponse;
import com.akif.dto.response.ErrorResponseDto;
import com.akif.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String ERROR_CODE_AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
    private static final String ERROR_CODE_BAD_CREDENTIALS = "BAD_CREDENTIALS";
    private static final String ERROR_CODE_ACCESS_DENIED = "ACCESS_DENIED";
    private static final String ERROR_CODE_VALIDATION_FAILED = "VALIDATION_FAILED";
    private static final String ERROR_CODE_CONSTRAINT_VIOLATION = "CONSTRAINT_VIOLATION";
    private static final String ERROR_CODE_INVALID_JSON = "INVALID_JSON";
    private static final String ERROR_CODE_MISSING_PARAMETER = "MISSING_PARAMETER";
    private static final String ERROR_CODE_INVALID_PARAMETER_TYPE = "INVALID_PARAMETER_TYPE";
    private static final String ERROR_CODE_ENDPOINT_NOT_FOUND = "ENDPOINT_NOT_FOUND";
    private static final String ERROR_CODE_INVALID_ARGUMENT = "INVALID_ARGUMENT";
    private static final String ERROR_CODE_INVALID_STATE = "INVALID_STATE";
    private static final String ERROR_CODE_INVALID_DATE_FORMAT = "INVALID_DATE_FORMAT";
    private static final String ERROR_CODE_INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";


    @ExceptionHandler(CarNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleCarNotFoundException(CarNotFoundException ex, WebRequest request) {
        log.error("Car not found: {}", ex.getMessage());
        return buildErrorResponse(ex.getErrorCode(), ex.getMessage(), ex.getHttpStatus(), request);
    }

    @ExceptionHandler(CarAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleCarAlreadyExistsException(CarAlreadyExistsException ex, WebRequest request) {
        log.error("Car already exists: {}", ex.getMessage());
        return buildErrorResponse(ex.getErrorCode(), ex.getMessage(), ex.getHttpStatus(), request);
    }

    @ExceptionHandler(CarCannotBeSoldException.class)
    public ResponseEntity<ErrorResponseDto> handleCarCannotBeSoldException(CarCannotBeSoldException ex, WebRequest request) {
        log.error("Car cannot be sold: {}", ex.getMessage());
        return buildErrorResponse(ex.getErrorCode(), ex.getMessage(), ex.getHttpStatus(), request);
    }

    @ExceptionHandler(CarCannotBeReservedException.class)
    public ResponseEntity<ErrorResponseDto> handleCarCannotBeReservedException(CarCannotBeReservedException ex, WebRequest request) {
        log.error("Car cannot be reserved: {}", ex.getMessage());
        return buildErrorResponse(ex.getErrorCode(), ex.getMessage(), ex.getHttpStatus(), request);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidStatusTransitionException(InvalidStatusTransitionException ex, WebRequest request) {
        log.error("Invalid status transition: {}", ex.getMessage());
        return buildErrorResponse(ex.getErrorCode(), ex.getMessage(), ex.getHttpStatus(), request);
    }

    @ExceptionHandler(CarValidationException.class)
    public ResponseEntity<ErrorResponseDto> handleCarValidationException(CarValidationException ex, WebRequest request) {
        log.error("Car validation failed: {}", ex.getMessage());
        
        Map<String, String> validationErrors = convertValidationErrorsToMap(ex.getValidationErrors());
        return buildErrorResponse(ex.getErrorCode(), ex.getMessage(), ex.getHttpStatus(), request, validationErrors);
    }

    @ExceptionHandler(CarOperationNotAllowedException.class)
    public ResponseEntity<ErrorResponseDto> handleCarOperationNotAllowedException(CarOperationNotAllowedException ex, WebRequest request) {
        log.error("Car operation not allowed: {}", ex.getMessage());
        return buildErrorResponse(ex.getErrorCode(), ex.getMessage(), ex.getHttpStatus(), request);
    }


    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        log.error("Authentication failed: {}", ex.getMessage());
        return buildErrorResponse(ERROR_CODE_AUTHENTICATION_FAILED, 
                "Authentication failed. Please check your credentials.", 
                HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        log.error("Bad credentials: {}", ex.getMessage());
        return buildErrorResponse(ERROR_CODE_BAD_CREDENTIALS, 
                "Invalid username or password.", 
                HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.error("Access denied: {}", ex.getMessage());
        return buildErrorResponse(ERROR_CODE_ACCESS_DENIED, 
                "Access denied. You don't have permission to perform this action.", 
                HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleUserAlreadyExistsException(UserAlreadyExistsException ex, WebRequest request) {
        log.error("User already exists: {}", ex.getMessage());
        return buildErrorResponse(ex.getErrorCode(), ex.getErrorMessage(), ex.getHttpStatus(), request);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidTokenException(InvalidTokenException ex, WebRequest request) {
        log.error("Invalid token: {}", ex.getMessage());
        return buildErrorResponse(ex.getErrorCode(), ex.getErrorMessage(), ex.getHttpStatus(), request);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponseDto> handleTokenExpiredException(TokenExpiredException ex, WebRequest request) {
        log.error("Token expired: {}", ex.getMessage());
        return buildErrorResponse(ex.getErrorCode(), ex.getErrorMessage(), ex.getHttpStatus(), request);
    }

    @ExceptionHandler(OAuth2AuthenticationException.class)
    public ResponseEntity<OAuth2ErrorResponse> handleOAuth2AuthenticationException(OAuth2AuthenticationException ex) {
        log.error("OAuth2 authentication error: {} - {} (provider: {})", 
                ex.getErrorType().getCode(), ex.getMessage(), ex.getProvider());
        
        OAuth2ErrorResponse errorResponse = OAuth2ErrorResponse.builder()
                .error(ex.getErrorType().getCode())
                .message(ex.getMessage())
                .provider(ex.getProvider())
                .build();
        
        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        log.error("Constraint violation: {}", ex.getMessage());

        Map<String, String> validationErrors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing // Handle duplicate keys
                ));

        return buildErrorResponse(ERROR_CODE_CONSTRAINT_VIOLATION, 
                "Constraint violation occurred", 
                HttpStatus.BAD_REQUEST, request, validationErrors);
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, WebRequest request) {
        log.error("HTTP message not readable: {}", ex.getMessage());
        return buildErrorResponse(ERROR_CODE_INVALID_JSON, 
                "Invalid JSON format in request body", 
                HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDto> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex, WebRequest request) {
        log.error("Missing request parameter: {}", ex.getMessage());
        return buildErrorResponse(ERROR_CODE_MISSING_PARAMETER, 
                String.format("Required parameter '%s' is missing", ex.getParameterName()), 
                HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex, WebRequest request) {
        log.error("Method argument type mismatch: {}", ex.getMessage());
        return buildErrorResponse(ERROR_CODE_INVALID_PARAMETER_TYPE, 
                String.format("Invalid parameter type for '%s'. Expected: %s", 
                        ex.getName(), 
                        ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"), 
                HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNoHandlerFoundException(NoHandlerFoundException ex, WebRequest request) {
        log.error("No handler found: {}", ex.getMessage());
        return buildErrorResponse(ERROR_CODE_ENDPOINT_NOT_FOUND, 
                "The requested endpoint was not found", 
                HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.error("Illegal argument: {}", ex.getMessage());
        return buildErrorResponse(ERROR_CODE_INVALID_ARGUMENT, ex.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(java.time.format.DateTimeParseException.class)
    public ResponseEntity<ErrorResponseDto> handleDateTimeParseException(java.time.format.DateTimeParseException ex, WebRequest request) {
        log.error("Invalid date format: {}", ex.getMessage());
        return buildErrorResponse(ERROR_CODE_INVALID_DATE_FORMAT, "Invalid date or month format. Please use yyyy-MM-dd for dates and yyyy-MM for months.", HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        log.error("Illegal state: {}", ex.getMessage());
        return buildErrorResponse(ERROR_CODE_INVALID_STATE, ex.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, WebRequest request) {
        log.error("Bean validation failed: {}", ex.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        return buildErrorResponse(ERROR_CODE_VALIDATION_FAILED, "Validation failed", HttpStatus.BAD_REQUEST, request, fieldErrors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return buildErrorResponse(ERROR_CODE_INTERNAL_SERVER_ERROR, 
                "An unexpected error occurred. Please try again later.", 
                HttpStatus.INTERNAL_SERVER_ERROR, request);
    }


    @ExceptionHandler(RentalNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleRentalNotFoundException(RentalNotFoundException ex, WebRequest request) {
        log.error("Rental not found: {}", ex.getMessage());
        return buildErrorResponse(ex.getErrorCode(), ex.getMessage(), ex.getHttpStatus(), request);
    }

    @ExceptionHandler(RentalValidationException.class)
    public ResponseEntity<ErrorResponseDto> handleRentalValidationException(RentalValidationException ex, WebRequest request) {
        log.error("Rental validation failed: {}", ex.getMessage());

        Map<String, String> validationErrors = convertValidationErrorsToMap(ex.getValidationErrors());
        return buildErrorResponse(ex.getErrorCode(), ex.getMessage(), ex.getHttpStatus(), request, validationErrors);
    }

    @ExceptionHandler(InvalidRentalStateException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidRentalStateException(InvalidRentalStateException ex, WebRequest request) {
        log.error("Invalid rental state: {}", ex.getMessage());
        return buildErrorResponse(ex.getErrorCode(), ex.getMessage(), ex.getHttpStatus(), request);
    }

    @ExceptionHandler(CarNotAvailableException.class)
    public ResponseEntity<ErrorResponseDto> handleCarNotAvailableException(CarNotAvailableException ex, WebRequest request) {
        log.error("Car not available: {}", ex.getMessage());
        return buildErrorResponse(ex.getErrorCode(), ex.getMessage(), ex.getHttpStatus(), request);
    }

    @ExceptionHandler(RentalDateOverlapException.class)
    public ResponseEntity<ErrorResponseDto> handleRentalDateOverlapException(RentalDateOverlapException ex, WebRequest request) {
        log.error("Rental date overlap: {}", ex.getMessage());
        return buildErrorResponse(ex.getErrorCode(), ex.getMessage(), ex.getHttpStatus(), request);
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ErrorResponseDto> handlePaymentFailedException(PaymentFailedException ex, WebRequest request) {
        log.error("Payment failed: {}", ex.getMessage());
        return buildErrorResponse(ex.getErrorCode(), ex.getMessage(), ex.getHttpStatus(), request);
    }

    @ExceptionHandler(StripeIntegrationException.class)
    public ResponseEntity<ErrorResponseDto> handleStripeIntegrationException(StripeIntegrationException ex, WebRequest request) {
        log.error("Stripe integration error: {} - {}", ex.getStripeErrorCode(), ex.getStripeErrorMessage());
        return buildErrorResponse(ex.getErrorCode(), ex.getMessage(), ex.getHttpStatus(), request);
    }

    @ExceptionHandler(WebhookSignatureException.class)
    public ResponseEntity<ErrorResponseDto> handleWebhookSignatureException(WebhookSignatureException ex, WebRequest request) {
        log.error("Webhook signature verification failed for event: {}", ex.getEventId());
        return buildErrorResponse(ex.getErrorCode(), ex.getMessage(), ex.getHttpStatus(), request);
    }

    @ExceptionHandler(ReconciliationException.class)
    public ResponseEntity<ErrorResponseDto> handleReconciliationException(ReconciliationException ex, WebRequest request) {
        log.error("Reconciliation failed for date: {}", ex.getReconciliationDate());
        return buildErrorResponse(ex.getErrorCode(), ex.getMessage(), ex.getHttpStatus(), request);
    }


    private ResponseEntity<ErrorResponseDto> buildErrorResponse(String errorCode, String message, 
                                                             HttpStatus status, WebRequest request) {
        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .path(getPath(request))
                .traceId(generateCorrelationId())
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }

    private ResponseEntity<ErrorResponseDto> buildErrorResponse(String errorCode, String message, 
                                                             HttpStatus status, WebRequest request, 
                                                             Map<String, String> validationErrors) {
        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .path(getPath(request))
                .traceId(generateCorrelationId())
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }

    private Map<String, String> convertValidationErrorsToMap(List<String> validationErrors) {
        Map<String, String> errorMap = new HashMap<>();
        if (validationErrors != null) {
            for (String error : validationErrors) {
                String fieldName = extractFieldNameFromError(error);
                errorMap.put(fieldName, error);
            }
        }
        return errorMap;
    }

    private String extractFieldNameFromError(String error) {
        if (error == null || error.trim().isEmpty()) {
            return "unknown";
        }

        String processed = error.toLowerCase()
                .replaceAll("[^a-z\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();

        String[] words = processed.split("\\s+");
        if (words.length > 0) {
            String firstWord = words[0];
            
            if (firstWord.contains("license") || firstWord.contains("plate")) {
                return "licensePlate";
            } else if (firstWord.contains("price")) {
                return "price";
            } else if (firstWord.contains("brand")) {
                return "brand";
            } else if (firstWord.contains("model")) {
                return "model";
            } else if (firstWord.contains("year")) {
                return "productionYear";
            } else if (firstWord.contains("currency")) {
                return "currencyType";
            } else if (firstWord.contains("status")) {
                return "carStatusType";
            } else if (firstWord.contains("vin")) {
                return "vinNumber";
            }
            return firstWord;
        }
        return "unknown";
    }


    public String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    public String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}