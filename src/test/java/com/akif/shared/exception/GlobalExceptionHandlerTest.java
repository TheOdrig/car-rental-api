package com.akif.shared.exception;

import com.akif.auth.internal.dto.oauth2.OAuth2ErrorResponse;
import com.akif.auth.internal.exceptipn.OAuth2AuthenticationException;
import com.akif.auth.domain.enums.OAuth2ErrorType;
import com.akif.car.internal.exception.CarAlreadyExistsException;
import com.akif.car.internal.exception.CarNotFoundException;
import com.akif.car.internal.exception.CarValidationException;
import com.akif.payment.internal.exception.PaymentFailedException;
import com.akif.rental.internal.exception.RentalNotFoundException;
import com.akif.shared.handler.ErrorResponseDto;
import com.akif.shared.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;
import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setRequestURI("/api/cars/123");
        webRequest = new ServletWebRequest(mockRequest);
    }


    @Nested
    @DisplayName("BaseException Handler Tests")
    class BaseExceptionHandlerTests {

        @Test
        @DisplayName("Should handle CarNotFoundException via BaseException handler")
        void shouldHandleCarNotFoundException() {
            CarNotFoundException exception = new CarNotFoundException("Car not found with id: 123");

            ResponseEntity<ErrorResponseDto> response = handler.handleBaseException(exception, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("CAR_NOT_FOUND");
            assertThat(response.getBody().getMessage()).isEqualTo("Car not found with id: 123");
            assertThat(response.getBody().getStatus()).isEqualTo(404);
            assertThat(response.getBody().getPath()).isEqualTo("/api/cars/123");
            assertThat(response.getBody().getTraceId()).isNotNull();
        }

        @Test
        @DisplayName("Should handle CarAlreadyExistsException via BaseException handler")
        void shouldHandleCarAlreadyExistsException() {
            CarAlreadyExistsException exception = new CarAlreadyExistsException("Car with license plate 34ABC123 already exists");

            ResponseEntity<ErrorResponseDto> response = handler.handleBaseException(exception, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("CAR_ALREADY_EXISTS");
            assertThat(response.getBody().getStatus()).isEqualTo(409);
        }

        @Test
        @DisplayName("Should handle RentalNotFoundException via BaseException handler")
        void shouldHandleRentalNotFoundException() {
            RentalNotFoundException exception = new RentalNotFoundException(456L);

            ResponseEntity<ErrorResponseDto> response = handler.handleBaseException(exception, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("RENTAL_NOT_FOUND");
        }

        @Test
        @DisplayName("Should handle PaymentFailedException via BaseException handler")
        void shouldHandlePaymentFailedException() {
            PaymentFailedException exception = new PaymentFailedException("Payment processing failed");

            ResponseEntity<ErrorResponseDto> response = handler.handleBaseException(exception, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("PAYMENT_FAILED");
        }

        @Test
        @DisplayName("Should handle CarValidationException via BaseException handler")
        void shouldHandleCarValidationException() {
            CarValidationException exception = new CarValidationException("Invalid license plate format");

            ResponseEntity<ErrorResponseDto> response = handler.handleBaseException(exception, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("CAR_VALIDATION_FAILED");
        }
    }


    @Nested
    @DisplayName("OAuth2AuthenticationException Handler Tests")
    class OAuth2ExceptionHandlerTests {

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


    @Nested
    @DisplayName("Security Exception Handler Tests")
    class SecurityExceptionHandlerTests {

        @Test
        @DisplayName("Should handle BadCredentialsException via AuthenticationException handler")
        void shouldHandleBadCredentialsException() {
            BadCredentialsException exception = new BadCredentialsException("Invalid credentials");

            ResponseEntity<ErrorResponseDto> response = handler.handleAuthenticationException(exception, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("AUTHENTICATION_FAILED");
            assertThat(response.getBody().getStatus()).isEqualTo(401);
        }

        @Test
        @DisplayName("Should handle AccessDeniedException with FORBIDDEN status")
        void shouldHandleAccessDeniedException() {
            AccessDeniedException exception = new AccessDeniedException("Access denied");

            ResponseEntity<ErrorResponseDto> response = handler.handleAccessDeniedException(exception, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("ACCESS_DENIED");
            assertThat(response.getBody().getStatus()).isEqualTo(403);
        }
    }


    @Nested
    @DisplayName("Validation Exception Handler Tests")
    class ValidationExceptionHandlerTests {

        @Test
        @DisplayName("Should handle ConstraintViolationException")
        void shouldHandleConstraintViolationException() {
            ConstraintViolationException exception = new ConstraintViolationException("Validation failed", Set.of());

            ResponseEntity<ErrorResponseDto> response = handler.handleConstraintViolationException(exception, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("CONSTRAINT_VIOLATION");
            assertThat(response.getBody().getStatus()).isEqualTo(400);
        }
    }


    @Nested
    @DisplayName("HTTP Exception Handler Tests")
    class HttpExceptionHandlerTests {

        @Test
        @DisplayName("Should handle MethodArgumentTypeMismatchException")
        void shouldHandleMethodArgumentTypeMismatchException() throws NoSuchMethodException {

            Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("setUp");
            MethodParameter methodParameter = new MethodParameter(method, -1);
            
            MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                    "invalid", Long.class, "id", methodParameter, null);

            ResponseEntity<ErrorResponseDto> response = handler.handleMethodArgumentTypeMismatchException(exception, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("INVALID_PARAMETER_TYPE");
            assertThat(response.getBody().getMessage()).contains("Invalid parameter type for 'id'");
            assertThat(response.getBody().getMessage()).contains("Long");
        }
    }


    @Nested
    @DisplayName("Generic Exception Handler Tests")
    class GenericExceptionHandlerTests {

        @Test
        @DisplayName("Should handle generic Exception with INTERNAL_SERVER_ERROR status")
        void shouldHandleGenericException() {
            Exception exception = new Exception("Unexpected error");

            ResponseEntity<ErrorResponseDto> response = handler.handleGenericException(exception, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("INTERNAL_SERVER_ERROR");
            assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
            assertThat(response.getBody().getStatus()).isEqualTo(500);
        }
    }


    @Nested
    @DisplayName("Helper Method Tests")
    class HelperMethodTests {

        @Test
        @DisplayName("Should generate unique trace IDs")
        void shouldGenerateUniqueTraceIds() {
            String traceId1 = handler.generateTraceId();
            String traceId2 = handler.generateTraceId();

            assertThat(traceId1).isNotNull();
            assertThat(traceId2).isNotNull();
            assertThat(traceId1).isNotEqualTo(traceId2);
            assertThat(traceId1).hasSize(36);
        }

        @Test
        @DisplayName("Should extract path from WebRequest")
        void shouldExtractPathFromWebRequest() {
            String path = handler.extractPath(webRequest);

            assertThat(path).isEqualTo("/api/cars/123");
        }
    }
}
