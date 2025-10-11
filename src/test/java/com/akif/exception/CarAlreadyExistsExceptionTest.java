package com.akif.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CarAlreadyExistsException Unit Tests")
class CarAlreadyExistsExceptionTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create exception with message and CONFLICT status")
        void shouldCreateExceptionWithMessageAndConflictStatus() {

            String message = "Car with license plate 34ABC123 already exists";

            CarAlreadyExistsException exception = new CarAlreadyExistsException(message);

            assertThat(exception.getErrorCode()).isEqualTo("CAR_ALREADY_EXISTS");
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("Should create exception with license plate")
        void shouldCreateExceptionWithLicensePlate() {

            String licensePlate = "34ABC123";

            CarAlreadyExistsException exception = new CarAlreadyExistsException("licensePlate", licensePlate);

            assertThat(exception.getErrorCode()).isEqualTo("CAR_ALREADY_EXISTS");
            assertThat(exception.getMessage()).isEqualTo("Car already exists with licensePlate: 34ABC123");
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("Should create exception with VIN number")
        void shouldCreateExceptionWithVinNumber() {

            String vinNumber = "1HGBH41JXMN109186";


            CarAlreadyExistsException exception = new CarAlreadyExistsException("VIN number", vinNumber);


            assertThat(exception.getErrorCode()).isEqualTo("CAR_ALREADY_EXISTS");
            assertThat(exception.getMessage()).isEqualTo("Car already exists with VIN number: 1HGBH41JXMN109186");
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("Should create exception with custom HTTP status")
        void shouldCreateExceptionWithCustomHttpStatus() {

            String message = "Duplicate car entry";
            HttpStatus customStatus = HttpStatus.BAD_REQUEST;

            CarAlreadyExistsException exception = new CarAlreadyExistsException(message, customStatus);

            assertThat(exception.getErrorCode()).isEqualTo("CAR_ALREADY_EXISTS");
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("Inheritance Tests")
    class InheritanceTests {

        @Test
        @DisplayName("Should extend BaseException")
        void shouldExtendBaseException() {

            CarAlreadyExistsException exception = new CarAlreadyExistsException("Test message");

            assertThat(exception).isInstanceOf(BaseException.class);
        }

        @Test
        @DisplayName("Should be throwable")
        void shouldBeThrowable() {

            assertThatThrownBy(() -> {
                throw new CarAlreadyExistsException("Test duplicate error");
            }).isInstanceOf(CarAlreadyExistsException.class)
              .hasMessage("Test duplicate error");
        }
    }
}
