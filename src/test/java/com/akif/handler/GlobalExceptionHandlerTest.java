package com.akif.handler;

import com.akif.dto.response.ErrorResponseDto;
import com.akif.exception.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;


    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setRequestURI("/api/cars/123");
        webRequest = new ServletWebRequest(mockRequest);
    }

    @Nested
    @DisplayName("CarNotFoundException Handler Tests")
    class CarNotFoundExceptionHandlerTests {

        @Test
        @DisplayName("Should handle CarNotFoundException with NOT_FOUND status")
        void shouldHandleCarNotFoundExceptionWithNotFoundStatus() {

            CarNotFoundException exception = new CarNotFoundException("Car not found with id: 123");

            ResponseEntity<ErrorResponseDto> response = globalExceptionHandler.handleCarNotFoundException(exception, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("CAR_NOT_FOUND");
            assertThat(response.getBody().getMessage()).isEqualTo("Car not found with id: 123");
            assertThat(response.getBody().getStatus()).isEqualTo(404);
            assertThat(response.getBody().getPath()).isEqualTo("/api/cars/123");
            assertThat(response.getBody().getTraceId()).isNotNull();
            assertThat(response.getBody().getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("Should handle CarNotFoundException with custom HTTP status")
        void shouldHandleCarNotFoundExceptionWithCustomHttpStatus() {

            CarNotFoundException exception = new CarNotFoundException("Invalid car ID format", HttpStatus.BAD_REQUEST);

            ResponseEntity<ErrorResponseDto> response = globalExceptionHandler.handleCarNotFoundException(exception, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("CAR_NOT_FOUND");
            assertThat(response.getBody().getMessage()).isEqualTo("Invalid car ID format");
            assertThat(response.getBody().getStatus()).isEqualTo(400);
        }
    }

    @Nested
    @DisplayName("CarValidationException Handler Tests")
    class CarValidationExceptionHandlerTests {

        @Test
        @DisplayName("Should handle CarValidationException with validation errors")
        void shouldHandleCarValidationExceptionWithValidationErrors() {

            List<String> validationErrors = List.of(
                "License plate cannot be empty",
                "Price must be positive"
            );
            CarValidationException exception = new CarValidationException(validationErrors);

            ResponseEntity<ErrorResponseDto> response = globalExceptionHandler.handleCarValidationException(exception, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("CAR_VALIDATION_FAILED");
            assertThat(response.getBody().getMessage()).isEqualTo("Car validation failed: License plate cannot be empty, Price must be positive");
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getValidationErrors()).isNotNull();
            assertThat(response.getBody().getValidationErrors()).hasSize(2);
            assertThat(response.getBody().getValidationErrors()).containsKey("licensePlate");
            assertThat(response.getBody().getValidationErrors()).containsKey("price");
        }

        @Test
        @DisplayName("Should handle CarValidationException with single message")
        void shouldHandleCarValidationExceptionWithSingleMessage() {

            CarValidationException exception = new CarValidationException("Invalid VIN number");

            ResponseEntity<ErrorResponseDto> response = globalExceptionHandler.handleCarValidationException(exception, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("CAR_VALIDATION_FAILED");
            assertThat(response.getBody().getMessage()).isEqualTo("Invalid VIN number");
            assertThat(response.getBody().getValidationErrors()).isNotNull();
            assertThat(response.getBody().getValidationErrors()).hasSize(1);
            assertThat(response.getBody().getValidationErrors()).containsKey("invalid");
        }
    }

    @Nested
    @DisplayName("CarAlreadyExistsException Handler Tests")
    class CarAlreadyExistsExceptionHandlerTests {

        @Test
        @DisplayName("Should handle CarAlreadyExistsException with CONFLICT status")
        void shouldHandleCarAlreadyExistsExceptionWithConflictStatus() {

            CarAlreadyExistsException exception = new CarAlreadyExistsException("Car with license plate 34ABC123 already exists");

            ResponseEntity<ErrorResponseDto> response = globalExceptionHandler.handleCarAlreadyExistsException(exception, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("CAR_ALREADY_EXISTS");
            assertThat(response.getBody().getMessage()).isEqualTo("Car with license plate 34ABC123 already exists");
            assertThat(response.getBody().getStatus()).isEqualTo(409);
        }
    }

    @Nested
    @DisplayName("CarOperationNotAllowedException Handler Tests")
    class CarOperationNotAllowedExceptionHandlerTests {

        @Test
        @DisplayName("Should handle CarOperationNotAllowedException with FORBIDDEN status")
        void shouldHandleCarOperationNotAllowedExceptionWithForbiddenStatus() {

            CarOperationNotAllowedException exception = new CarOperationNotAllowedException("Car operation not allowed");

            ResponseEntity<ErrorResponseDto> response = globalExceptionHandler.handleCarOperationNotAllowedException(exception, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("CAR_OPERATION_NOT_ALLOWED");
            assertThat(response.getBody().getMessage()).isEqualTo("Car operation not allowed");
            assertThat(response.getBody().getStatus()).isEqualTo(403);
        }
    }

    @Nested
    @DisplayName("Security Exception Handler Tests")
    class SecurityExceptionHandlerTests {

        @Test
        @DisplayName("Should handle BadCredentialsException with UNAUTHORIZED status")
        void shouldHandleBadCredentialsExceptionWithUnauthorizedStatus() {

            BadCredentialsException exception = new BadCredentialsException("Invalid credentials");

            ResponseEntity<ErrorResponseDto> response = globalExceptionHandler.handleBadCredentialsException(exception, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("BAD_CREDENTIALS");
            assertThat(response.getBody().getMessage()).isEqualTo("Invalid username or password.");
            assertThat(response.getBody().getStatus()).isEqualTo(401);
        }

        @Test
        @DisplayName("Should handle AccessDeniedException with FORBIDDEN status")
        void shouldHandleAccessDeniedExceptionWithForbiddenStatus() {

            AccessDeniedException exception = new AccessDeniedException("Access denied");

            ResponseEntity<ErrorResponseDto> response = globalExceptionHandler.handleAccessDeniedException(exception, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("ACCESS_DENIED");
            assertThat(response.getBody().getMessage()).isEqualTo("Access denied. You don't have permission to perform this action.");
            assertThat(response.getBody().getStatus()).isEqualTo(403);
        }
    }

    @Nested
    @DisplayName("Validation Exception Handler Tests")
    class ValidationExceptionHandlerTests {

        @Test
        @DisplayName("Should handle MethodArgumentNotValidException")
        void shouldHandleMethodArgumentNotValidException() {



        }

        @Test
        @DisplayName("Should handle ConstraintViolationException")
        void shouldHandleConstraintViolationException() {

            ConstraintViolationException exception = new ConstraintViolationException("Validation failed", Set.of());

            ResponseEntity<ErrorResponseDto> response = globalExceptionHandler.handleConstraintViolationException(exception, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("CONSTRAINT_VIOLATION");
            assertThat(response.getBody().getMessage()).isEqualTo("Constraint violation occurred");
            assertThat(response.getBody().getStatus()).isEqualTo(400);
        }
    }

    @Nested
    @DisplayName("Generic Exception Handler Tests")
    class GenericExceptionHandlerTests {

        @Test
        @DisplayName("Should handle IllegalArgumentException with BAD_REQUEST status")
        void shouldHandleIllegalArgumentExceptionWithBadRequestStatus() {

            IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");

            ResponseEntity<ErrorResponseDto> response = globalExceptionHandler.handleIllegalArgumentException(exception, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("INVALID_ARGUMENT");
            assertThat(response.getBody().getMessage()).isEqualTo("Invalid argument");
            assertThat(response.getBody().getStatus()).isEqualTo(400);
        }

        @Test
        @DisplayName("Should handle MethodArgumentTypeMismatchException")
        void shouldHandleMethodArgumentTypeMismatchException() {

            MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "invalid", String.class, "id", null, null);

            ResponseEntity<ErrorResponseDto> response = globalExceptionHandler.handleMethodArgumentTypeMismatchException(exception, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("INVALID_PARAMETER_TYPE");
            assertThat(response.getBody().getMessage()).contains("Invalid parameter type for 'id'");
            assertThat(response.getBody().getStatus()).isEqualTo(400);
        }

        @Test
        @DisplayName("Should handle generic Exception with INTERNAL_SERVER_ERROR status")
        void shouldHandleGenericExceptionWithInternalServerErrorStatus() {

            Exception exception = new Exception("Unexpected error");

            ResponseEntity<ErrorResponseDto> response = globalExceptionHandler.handleGenericException(exception, webRequest);

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
        @DisplayName("Should generate correlation ID")
        void shouldGenerateCorrelationId() {

            String correlationId1 = globalExceptionHandler.generateCorrelationId();
            String correlationId2 = globalExceptionHandler.generateCorrelationId();

            assertThat(correlationId1).isNotNull();
            assertThat(correlationId2).isNotNull();
            assertThat(correlationId1).isNotEqualTo(correlationId2);
            assertThat(correlationId1).hasSize(36); // UUID length
        }

        @Test
        @DisplayName("Should extract path from WebRequest")
        void shouldExtractPathFromWebRequest() {

            String path = globalExceptionHandler.getPath(webRequest);

            assertThat(path).isEqualTo("/api/cars/123");
        }
    }
}
