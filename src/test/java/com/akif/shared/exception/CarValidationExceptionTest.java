package com.akif.shared.exception;

import com.akif.car.internal.exception.CarValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CarValidationException Unit Tests")
class CarValidationExceptionTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create exception with single message")
        void shouldCreateExceptionWithSingleMessage() {

            String message = "Invalid license plate format";

            CarValidationException exception = new CarValidationException(message);

            assertThat(exception.getErrorCode()).isEqualTo("CAR_VALIDATION_FAILED");
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(exception.getValidationErrors()).containsExactly(message);
        }

        @Test
        @DisplayName("Should create exception with validation errors list")
        void shouldCreateExceptionWithValidationErrorsList() {

            List<String> validationErrors = List.of(
                "License plate cannot be empty",
                "Price must be positive",
                "Production year must be between 1900 and 2024"
            );

            CarValidationException exception = new CarValidationException(validationErrors);

            assertThat(exception.getErrorCode()).isEqualTo("CAR_VALIDATION_FAILED");
            assertThat(exception.getMessage()).isEqualTo("Car validation failed: License plate cannot be empty, Price must be positive, Production year must be between 1900 and 2024");
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(exception.getValidationErrors()).hasSize(3)
                .containsExactlyElementsOf(validationErrors);
        }

        @Test
        @DisplayName("Should create exception with message and custom HTTP status")
        void shouldCreateExceptionWithMessageAndCustomHttpStatus() {

            String message = "Invalid car data";
            HttpStatus customStatus = HttpStatus.UNPROCESSABLE_ENTITY;

            CarValidationException exception = new CarValidationException(message, customStatus);

            assertThat(exception.getErrorCode()).isEqualTo("CAR_VALIDATION_FAILED");
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(exception.getValidationErrors()).containsExactly(message);
        }

        @Test
        @DisplayName("Should create exception with validation errors and custom HTTP status")
        void shouldCreateExceptionWithValidationErrorsAndCustomHttpStatus() {

            List<String> validationErrors = List.of("Invalid VIN number");
            HttpStatus customStatus = HttpStatus.BAD_REQUEST;

            CarValidationException exception = new CarValidationException(validationErrors, customStatus);

            assertThat(exception.getErrorCode()).isEqualTo("CAR_VALIDATION_FAILED");
            assertThat(exception.getMessage()).isEqualTo("Car validation failed: Invalid VIN number");
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(exception.getValidationErrors()).containsExactly("Invalid VIN number");
        }

        @Test
        @DisplayName("Should handle empty validation errors list")
        void shouldHandleEmptyValidationErrorsList() {

            List<String> validationErrors = List.of();

            CarValidationException exception = new CarValidationException(validationErrors);

            assertThat(exception.getErrorCode()).isEqualTo("CAR_VALIDATION_FAILED");
            assertThat(exception.getMessage()).isEqualTo("Car validation failed: ");
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(exception.getValidationErrors()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null validation errors")
        void shouldHandleNullValidationErrors() {

            List<String> validationErrors = null;

            CarValidationException exception = new CarValidationException(validationErrors);

            assertThat(exception.getErrorCode()).isEqualTo("CAR_VALIDATION_FAILED");
            assertThat(exception.getMessage()).isEqualTo("Car validation failed: ");
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(exception.getValidationErrors()).isNull();
        }
    }

    @Nested
    @DisplayName("toString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should return proper string representation")
        void shouldReturnProperStringRepresentation() {

            List<String> validationErrors = List.of("Error 1", "Error 2");
            CarValidationException exception = new CarValidationException(validationErrors);

            String result = exception.toString();

            assertThat(result).contains("CarValidationException")
                .contains("errorCode=CAR_VALIDATION_FAILED")
                .contains("validationErrors=[Error 1, Error 2]");
        }
    }

    @Nested
    @DisplayName("Inheritance Tests")
    class InheritanceTests {

        @Test
        @DisplayName("Should extend BaseException")
        void shouldExtendBaseException() {

            CarValidationException exception = new CarValidationException("Test message");

            assertThat(exception).isInstanceOf(BaseException.class);
        }

        @Test
        @DisplayName("Should be throwable")
        void shouldBeThrowable() {

            assertThatThrownBy(() -> {
                throw new CarValidationException("Test validation error");
            }).isInstanceOf(CarValidationException.class)
              .hasMessage("Test validation error");
        }
    }
}
