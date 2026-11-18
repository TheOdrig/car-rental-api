package com.akif.shared.handler;

import com.akif.shared.exception.ErrorCode;
import com.akif.shared.exception.BaseException;
import com.akif.shared.exception.RateLimitExceededException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.lang.reflect.Method;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String ERROR_CODE_AUTHENTICATION_FAILED = ErrorCode.AUTHENTICATION_FAILED.name();
    static final String ERROR_CODE_ACCESS_DENIED = ErrorCode.ACCESS_DENIED.name();
    static final String ERROR_CODE_VALIDATION_FAILED = ErrorCode.VALIDATION_FAILED.name();
    static final String ERROR_CODE_CONSTRAINT_VIOLATION = ErrorCode.CONSTRAINT_VIOLATION.name();
    static final String ERROR_CODE_INVALID_JSON = ErrorCode.INVALID_JSON.name();
    static final String ERROR_CODE_MISSING_PARAMETER = ErrorCode.MISSING_PARAMETER.name();
    static final String ERROR_CODE_INVALID_PARAMETER_TYPE = ErrorCode.INVALID_PARAMETER_TYPE.name();
    static final String ERROR_CODE_ENDPOINT_NOT_FOUND = ErrorCode.ENDPOINT_NOT_FOUND.name();
    static final String ERROR_CODE_INTERNAL_SERVER_ERROR = ErrorCode.INTERNAL_SERVER_ERROR.name();

    private static final Comparator<FieldProblem> BY_FIELD_THEN_MESSAGE =
            Comparator.comparing(FieldProblem::field).thenComparing(FieldProblem::message);

    private final ProblemFactory problems;

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ProblemDetail> handleBaseException(BaseException ex, WebRequest request) {
        log.error("[{}] {}", ex.getErrorCode(), ex.getMessage());
        ProblemDetail problem = problems.create(ex.getHttpStatus(), ex.getErrorCode(), ex.getMessage(),
                extractPath(request), ex.getDetails());
        return respond(problem);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimitExceeded(RateLimitExceededException ex, WebRequest request) {
        log.warn("[{}] {}", ex.getErrorCode(), ex.getMessage());
        ProblemDetail problem = problems.create(ex.getHttpStatus(), ex.getErrorCode(), ex.getMessage(),
                extractPath(request), ex.getDetails());
        return ResponseEntity.status(problem.getStatus())
                .header(HttpHeaders.RETRY_AFTER, Long.toString(ex.getRetryAfterSeconds()))
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        List<FieldProblem> errors = ex.getConstraintViolations().stream()
                .map(violation -> new FieldProblem(extractPropertyName(violation), violation.getMessage()))
                .distinct()
                .sorted(BY_FIELD_THEN_MESSAGE)
                .toList();
        log.error("[{}] Constraint violation: {}", ERROR_CODE_CONSTRAINT_VIOLATION, errors);
        return respond(problems.create(HttpStatus.BAD_REQUEST, ERROR_CODE_CONSTRAINT_VIOLATION,
                "Constraint violation occurred", extractPath(request), errors));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        log.error("[{}] Authentication failed: {}", ERROR_CODE_AUTHENTICATION_FAILED, ex.getMessage());
        return respond(problems.create(HttpStatus.UNAUTHORIZED, ERROR_CODE_AUTHENTICATION_FAILED,
                "Authentication failed. Please check your credentials.", extractPath(request)));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        log.error("[{}] Access denied: {}", ERROR_CODE_ACCESS_DENIED, ex.getMessage());
        return respond(problems.create(HttpStatus.FORBIDDEN, ERROR_CODE_ACCESS_DENIED,
                "Access denied. You don't have permission to perform this action.", extractPath(request)));
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ProblemDetail> handleDateTimeParseException(
            DateTimeParseException ex, WebRequest request) {
        log.error("[{}] Invalid date format: {}", ERROR_CODE_INVALID_PARAMETER_TYPE, ex.getMessage());
        return respond(problems.create(HttpStatus.BAD_REQUEST, ERROR_CODE_INVALID_PARAMETER_TYPE,
                "Invalid date format. Expected format: yyyy-MM-dd", extractPath(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex, WebRequest request) {
        log.error("[{}] Unexpected error: {}", ERROR_CODE_INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
        return respond(problems.create(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_CODE_INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.", extractPath(request)));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldProblem> errors = new ArrayList<>();
        Object target = ex.getBindingResult().getTarget();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.add(new FieldProblem(reportedField(target, error.getField()), messageOf(error)));
        }
        for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            errors.add(new FieldProblem(error.getObjectName(), messageOf(error)));
        }
        List<FieldProblem> sorted = errors.stream().distinct().sorted(BY_FIELD_THEN_MESSAGE).toList();
        log.error("[{}] Validation failed: {}", ERROR_CODE_VALIDATION_FAILED, sorted);
        return respondObject(problems.create(HttpStatus.BAD_REQUEST, ERROR_CODE_VALIDATION_FAILED,
                "Validation failed", extractPath(request), sorted));
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldProblem> errors = new ArrayList<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            if (result instanceof ParameterErrors parameterErrors) {
                Object target = parameterErrors.getArgument();
                for (FieldError error : parameterErrors.getFieldErrors()) {
                    errors.add(new FieldProblem(reportedField(target, error.getField()), messageOf(error)));
                }
            } else {
                String name = result.getMethodParameter().getParameterName();
                String field = name != null ? name : "parameter" + result.getMethodParameter().getParameterIndex();
                result.getResolvableErrors().forEach(error -> errors.add(new FieldProblem(field, messageOf(error))));
            }
        }
        List<FieldProblem> sorted = errors.stream().distinct().sorted(BY_FIELD_THEN_MESSAGE).toList();
        log.error("[{}] Validation failed: {}", ERROR_CODE_VALIDATION_FAILED, sorted);
        return respondObject(problems.create(HttpStatus.BAD_REQUEST, ERROR_CODE_VALIDATION_FAILED,
                "Validation failed", extractPath(request), sorted));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.error("[{}] Invalid JSON: {}", ERROR_CODE_INVALID_JSON, ex.getMessage());
        return respondObject(problems.create(HttpStatus.BAD_REQUEST, ERROR_CODE_INVALID_JSON,
                "Invalid JSON format in request body", extractPath(request)));
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status,
            WebRequest request) {
        log.error("[{}] Missing parameter: {}", ERROR_CODE_MISSING_PARAMETER, ex.getParameterName());
        return respondObject(problems.create(HttpStatus.BAD_REQUEST, ERROR_CODE_MISSING_PARAMETER,
                String.format("Required parameter '%s' is missing", ex.getParameterName()), extractPath(request)));
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String name = ex instanceof MethodArgumentTypeMismatchException mismatch ? mismatch.getName()
                : ex.getPropertyName();
        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        log.error("[{}] Type mismatch for '{}': expected {}", ERROR_CODE_INVALID_PARAMETER_TYPE, name, expectedType);
        return respondObject(problems.create(HttpStatus.BAD_REQUEST, ERROR_CODE_INVALID_PARAMETER_TYPE,
                String.format("Invalid parameter type for '%s'. Expected: %s", name, expectedType),
                extractPath(request)));
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.error("[{}] Endpoint not found: {} {}", ERROR_CODE_ENDPOINT_NOT_FOUND, ex.getHttpMethod(),
                ex.getRequestURL());
        return respondObject(problems.create(HttpStatus.NOT_FOUND, ERROR_CODE_ENDPOINT_NOT_FOUND,
                "The requested endpoint was not found", extractPath(request)));
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.error("[{}] Endpoint not found: {} {}", ERROR_CODE_ENDPOINT_NOT_FOUND, ex.getHttpMethod(),
                ex.getResourcePath());
        return respondObject(problems.create(HttpStatus.NOT_FOUND, ERROR_CODE_ENDPOINT_NOT_FOUND,
                "The requested endpoint was not found", extractPath(request)));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response == null || !(response.getBody() instanceof ProblemDetail problem)) {
            return response;
        }
        problems.enrich(problem, defaultCode(statusCode), extractPath(request));
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.putAll(response.getHeaders());
        responseHeaders.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return new ResponseEntity<>(problem, responseHeaders, response.getStatusCode());
    }

    public String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    private ResponseEntity<ProblemDetail> respond(ProblemDetail problem) {
        return ResponseEntity.status(problem.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private ResponseEntity<Object> respondObject(ProblemDetail problem) {
        return ResponseEntity.status(problem.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private static String defaultCode(HttpStatusCode status) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        return resolved != null ? resolved.name() : "HTTP_" + status.value();
    }

    private static String messageOf(MessageSourceResolvable error) {
        String message = error.getDefaultMessage();
        return message != null ? message : "invalid";
    }

    private String extractPropertyName(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        return lastDot >= 0 ? path.substring(lastDot + 1) : path;
    }

    static String reportedField(Object target, String property) {
        if (target == null || property == null || property.isEmpty()) {
            return property;
        }
        String capitalized = property.substring(0, 1).toUpperCase(Locale.ROOT) + property.substring(1);
        for (Method method : target.getClass().getMethods()) {
            ReportsAsField annotation = method.getAnnotation(ReportsAsField.class);
            if (annotation != null
                    && (method.getName().equals("is" + capitalized) || method.getName().equals("get" + capitalized))) {
                return annotation.value();
            }
        }
        return property;
    }
}
