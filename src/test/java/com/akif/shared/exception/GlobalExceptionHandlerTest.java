package com.akif.shared.exception;

import com.akif.car.api.CarDatesUnavailableException;
import com.akif.car.api.CarNotAvailableException;
import com.akif.car.internal.exception.CarAlreadyExistsException;
import com.akif.car.internal.exception.CarNotFoundException;
import com.akif.car.internal.exception.CarValidationException;
import com.akif.payment.api.PaymentFailedException;
import com.akif.rental.api.RentalRequest;
import com.akif.rental.internal.exception.PaymentWindowNotOpenException;
import com.akif.rental.internal.exception.RentalNotFoundException;
import com.akif.shared.handler.FieldProblem;
import com.akif.shared.handler.GlobalExceptionHandler;
import com.akif.shared.handler.ProblemFactory;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private GlobalExceptionHandler handler;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        handler = new GlobalExceptionHandler(new ProblemFactory(clock));
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setRequestURI("/api/v1/cars/123");
        webRequest = new ServletWebRequest(mockRequest);
    }

    private static String code(ResponseEntity<?> response) {
        ProblemDetail problem = (ProblemDetail) response.getBody();
        assertThat(problem).isNotNull();
        return (String) problem.getProperties().get("code");
    }

    private static ProblemDetail problem(ResponseEntity<?> response) {
        assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
        return (ProblemDetail) response.getBody();
    }

    @Nested
    @DisplayName("BaseException Handler Tests")
    class BaseExceptionHandlerTests {

        @Test
        @DisplayName("Should render the full RFC 9457 shape")
        void shouldRenderFullShape() {
            ResponseEntity<ProblemDetail> response =
                    handler.handleBaseException(new CarNotFoundException("Car not found with id: 123"), webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
            ProblemDetail body = problem(response);
            assertThat(body.getType().toString()).isEqualTo("urn:car-rental:problem:car-not-found");
            assertThat(body.getTitle()).isEqualTo("Not Found");
            assertThat(body.getStatus()).isEqualTo(404);
            assertThat(body.getDetail()).isEqualTo("Car not found with id: 123");
            assertThat(body.getInstance().toString()).isEqualTo("/api/v1/cars/123");
            assertThat(body.getProperties()).containsEntry("code", "CAR_NOT_FOUND");
            assertThat(body.getProperties()).containsEntry("timestamp", NOW);
            assertThat(body.getProperties().get("traceId")).isNotNull();
        }

        @Test
        @DisplayName("Should handle CarAlreadyExistsException")
        void shouldHandleCarAlreadyExistsException() {
            ResponseEntity<ProblemDetail> response = handler.handleBaseException(
                    new CarAlreadyExistsException("Car with license plate 34ABC123 already exists"), webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(code(response)).isEqualTo("CAR_ALREADY_EXISTS");
            assertThat(problem(response).getStatus()).isEqualTo(409);
        }

        @Test
        @DisplayName("Should handle RentalNotFoundException")
        void shouldHandleRentalNotFoundException() {
            ResponseEntity<ProblemDetail> response =
                    handler.handleBaseException(new RentalNotFoundException(456L), webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(code(response)).isEqualTo("RENTAL_NOT_FOUND");
        }

        @Test
        @DisplayName("Should handle PaymentFailedException")
        void shouldHandlePaymentFailedException() {
            ResponseEntity<ProblemDetail> response =
                    handler.handleBaseException(new PaymentFailedException("Payment processing failed"), webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
            assertThat(code(response)).isEqualTo("PAYMENT_FAILED");
        }

        @Test
        @DisplayName("Should handle CarValidationException")
        void shouldHandleCarValidationException() {
            ResponseEntity<ProblemDetail> response = handler.handleBaseException(
                    new CarValidationException("Invalid license plate format"), webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(code(response)).isEqualTo("CAR_VALIDATION_FAILED");
        }

        @Test
        @DisplayName("Should answer fleet conditions with 400 CAR_NOT_AVAILABLE")
        void shouldHandleCarNotAvailable() {
            ResponseEntity<ProblemDetail> response = handler.handleBaseException(
                    new CarNotAvailableException(7L, "Car status is: Maintenance"), webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(code(response)).isEqualTo("CAR_NOT_AVAILABLE");
        }

        @Test
        @DisplayName("Should answer date conflicts with 409 CAR_DATES_UNAVAILABLE")
        void shouldHandleCarDatesUnavailable() {
            ResponseEntity<ProblemDetail> overlap = handler.handleBaseException(
                    CarDatesUnavailableException.bookedForDates(7L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5)),
                    webRequest);
            ResponseEntity<ProblemDetail> inUse = handler.handleBaseException(
                    CarDatesUnavailableException.stillInUse(7L), webRequest);

            assertThat(overlap.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(code(overlap)).isEqualTo("CAR_DATES_UNAVAILABLE");
            assertThat(inUse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(code(inUse)).isEqualTo("CAR_DATES_UNAVAILABLE");
        }

        @Test
        @DisplayName("Should keep the details map as an extension member")
        @SuppressWarnings("unchecked")
        void shouldKeepDetails() {
            ResponseEntity<ProblemDetail> response = handler.handleBaseException(
                    new PaymentWindowNotOpenException(LocalDate.of(2026, 3, 1)), webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(code(response)).isEqualTo("PAYMENT_WINDOW_NOT_OPEN");
            Map<String, Object> details = (Map<String, Object>) problem(response).getProperties().get("details");
            assertThat(details).containsKey("paymentOpensOn");
        }
    }

    @Nested
    @DisplayName("Security Exception Handler Tests")
    class SecurityExceptionHandlerTests {

        @Test
        @DisplayName("Should handle BadCredentialsException via AuthenticationException handler")
        void shouldHandleBadCredentialsException() {
            ResponseEntity<ProblemDetail> response =
                    handler.handleAuthenticationException(new BadCredentialsException("Invalid credentials"), webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(code(response)).isEqualTo("AUTHENTICATION_FAILED");
            assertThat(problem(response).getStatus()).isEqualTo(401);
        }

        @Test
        @DisplayName("Should handle AccessDeniedException with FORBIDDEN status")
        void shouldHandleAccessDeniedException() {
            ResponseEntity<ProblemDetail> response =
                    handler.handleAccessDeniedException(new AccessDeniedException("Access denied"), webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(code(response)).isEqualTo("ACCESS_DENIED");
            assertThat(problem(response).getStatus()).isEqualTo(403);
        }
    }

    @Nested
    @DisplayName("Validation Exception Handler Tests")
    class ValidationExceptionHandlerTests {

        @Test
        @DisplayName("Should handle ConstraintViolationException")
        void shouldHandleConstraintViolationException() {
            ResponseEntity<ProblemDetail> response = handler.handleConstraintViolationException(
                    new ConstraintViolationException("Validation failed", Set.of()), webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(code(response)).isEqualTo("CONSTRAINT_VIOLATION");
            assertThat(problem(response).getStatus()).isEqualTo(400);
        }

        @Test
        @DisplayName("Should report method level rules under the field they concern")
        @SuppressWarnings("unchecked")
        void shouldMapAssertTrueMethodsToFields() throws Exception {
            RentalRequest target = new RentalRequest(1L, LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 1),
                    null);
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "rentalRequest");
            bindingResult.addError(new FieldError("rentalRequest", "endDateAfterStartDate",
                    "End date must be after start date"));
            bindingResult.addError(new FieldError("rentalRequest", "withinMaxLeadTime",
                    "Start date cannot be more than 180 days ahead"));
            bindingResult.addError(new FieldError("rentalRequest", "carId", "Car id is required"));
            Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("setUp");
            MethodArgumentNotValidException exception =
                    new MethodArgumentNotValidException(new MethodParameter(method, -1), bindingResult);

            ResponseEntity<Object> response = handler.handleException(exception, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(code(response)).isEqualTo("VALIDATION_FAILED");
            List<FieldProblem> errors = (List<FieldProblem>) problem(response).getProperties().get("errors");
            assertThat(errors).containsExactly(
                    new FieldProblem("carId", "Car id is required"),
                    new FieldProblem("endDate", "End date must be after start date"),
                    new FieldProblem("startDate", "Start date cannot be more than 180 days ahead"));
        }
    }

    @Nested
    @DisplayName("HTTP Exception Handler Tests")
    class HttpExceptionHandlerTests {

        @Test
        @DisplayName("Should handle MethodArgumentTypeMismatchException")
        void shouldHandleMethodArgumentTypeMismatchException() throws Exception {
            Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("setUp");
            MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                    "invalid", Long.class, "id", new MethodParameter(method, -1), null);

            ResponseEntity<Object> response = handler.handleException(exception, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(code(response)).isEqualTo("INVALID_PARAMETER_TYPE");
            assertThat(problem(response).getDetail()).contains("Invalid parameter type for 'id'").contains("Long");
        }

        @Test
        @DisplayName("Should render built in MVC exceptions as problem details with a code")
        void shouldEnrichBuiltInProblems() throws Exception {
            ResponseEntity<Object> response = handler.handleException(
                    new HttpRequestMethodNotSupportedException("DELETE", List.of("GET")), webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
            assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
            ProblemDetail body = problem(response);
            assertThat(body.getProperties()).containsEntry("code", "METHOD_NOT_ALLOWED");
            assertThat(body.getInstance().toString()).isEqualTo("/api/v1/cars/123");
            assertThat(body.getTitle()).isEqualTo("Method Not Allowed");
            assertThat(body.getProperties()).containsKey("timestamp");
        }
    }

    @Nested
    @DisplayName("Persistence and upload failures")
    class PersistenceAndUploadTests {

        @Test
        @DisplayName("Should answer a data integrity violation with 409 DATA_CONFLICT")
        void shouldMapDataIntegrityViolation() {
            ResponseEntity<ProblemDetail> response = handler.handleDataIntegrityViolation(
                    new DataIntegrityViolationException("fk_rentals_car"), webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(code(response)).isEqualTo("DATA_CONFLICT");
        }

        @Test
        @DisplayName("Should answer an optimistic lock failure with 409 CONCURRENT_MODIFICATION")
        void shouldMapOptimisticLock() {
            ResponseEntity<ProblemDetail> response = handler.handleOptimisticLock(
                    new OptimisticLockingFailureException("stale"), webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(code(response)).isEqualTo("CONCURRENT_MODIFICATION");
        }

        @Test
        @DisplayName("Should answer an oversized upload with 413 PAYLOAD_TOO_LARGE")
        void shouldMapMaxUploadSize() {
            ResponseEntity<Object> response = handler.handleMaxUploadSizeExceededException(
                    new MaxUploadSizeExceededException(1024L), new HttpHeaders(), HttpStatus.CONTENT_TOO_LARGE, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
            assertThat(code(response)).isEqualTo("PAYLOAD_TOO_LARGE");
        }

        @Test
        @DisplayName("Should answer a malformed multipart request with 400 INVALID_MULTIPART_REQUEST")
        void shouldMapMultipart() {
            ResponseEntity<ProblemDetail> response = handler.handleMultipart(
                    new MultipartException("broken"), webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(code(response)).isEqualTo("INVALID_MULTIPART_REQUEST");
        }
    }

    @Nested
    @DisplayName("Generic Exception Handler Tests")
    class GenericExceptionHandlerTests {

        @Test
        @DisplayName("Should handle generic Exception with INTERNAL_SERVER_ERROR status")
        void shouldHandleGenericException() {
            ResponseEntity<ProblemDetail> response =
                    handler.handleGenericException(new Exception("Unexpected error"), webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(code(response)).isEqualTo("INTERNAL_SERVER_ERROR");
            assertThat(problem(response).getDetail()).isEqualTo("An unexpected error occurred. Please try again later.");
            assertThat(problem(response).getStatus()).isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("Helper Method Tests")
    class HelperMethodTests {

        @Test
        @DisplayName("Should generate unique trace IDs")
        void shouldGenerateUniqueTraceIds() {
            ProblemFactory factory = new ProblemFactory(Clock.fixed(NOW, ZoneOffset.UTC));

            String traceId1 = factory.traceId();
            String traceId2 = factory.traceId();

            assertThat(traceId1).isNotEqualTo(traceId2).hasSize(36);
        }

        @Test
        @DisplayName("Should extract path from WebRequest")
        void shouldExtractPathFromWebRequest() {
            assertThat(handler.extractPath(webRequest)).isEqualTo("/api/v1/cars/123");
        }
    }
}
