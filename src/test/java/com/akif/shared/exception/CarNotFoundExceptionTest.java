package com.akif.shared.exception;

import com.akif.car.internal.exception.CarNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CarNotFoundException Unit Tests")
class CarNotFoundExceptionTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create exception with message and NOT_FOUND status")
        void shouldCreateExceptionWithMessageAndNotFoundStatus() {
            String message = "Car not found";

            CarNotFoundException exception = new CarNotFoundException(message);

            assertThat(exception.getErrorCode()).isEqualTo("CAR_NOT_FOUND");
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Should create exception with ID and NOT_FOUND status")
        void shouldCreateExceptionWithIdAndNotFoundStatus() {
            Long id = 123L;

            CarNotFoundException exception = new CarNotFoundException(id);

            assertThat(exception.getErrorCode()).isEqualTo("CAR_NOT_FOUND");
            assertThat(exception.getMessage()).isEqualTo("Car not found with id: 123");
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Should create exception with field and value")
        void shouldCreateExceptionWithFieldAndValue() {
            String field = "licensePlate";
            String value = "34ABC123";

            CarNotFoundException exception = new CarNotFoundException(field, value);

            assertThat(exception.getErrorCode()).isEqualTo("CAR_NOT_FOUND");
            assertThat(exception.getMessage()).isEqualTo("Car not found with licensePlate: 34ABC123");
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Should create exception with custom HTTP status")
        void shouldCreateExceptionWithCustomHttpStatus() {

            String message = "Invalid car ID format";
            HttpStatus customStatus = HttpStatus.BAD_REQUEST;

            CarNotFoundException exception = new CarNotFoundException(message, customStatus);

            assertThat(exception.getErrorCode()).isEqualTo("CAR_NOT_FOUND");
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should create exception with ID and custom HTTP status")
        void shouldCreateExceptionWithIdAndCustomHttpStatus() {

            Long id = 123L;
            HttpStatus customStatus = HttpStatus.FORBIDDEN;

            CarNotFoundException exception = new CarNotFoundException(id, customStatus);

            assertThat(exception.getErrorCode()).isEqualTo("CAR_NOT_FOUND");
            assertThat(exception.getMessage()).isEqualTo("Car not found with id: 123");
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Should create exception with field, value and custom HTTP status")
        void shouldCreateExceptionWithFieldValueAndCustomHttpStatus() {

            String field = "vinNumber";
            String value = "1HGBH41JXMN109186";
            HttpStatus customStatus = HttpStatus.BAD_REQUEST;

            CarNotFoundException exception = new CarNotFoundException(field, value, customStatus);

            assertThat(exception.getErrorCode()).isEqualTo("CAR_NOT_FOUND");
            assertThat(exception.getMessage()).isEqualTo("Car not found with vinNumber: 1HGBH41JXMN109186");
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("Inheritance Tests")
    class InheritanceTests {

        @Test
        @DisplayName("Should extend BaseException")
        void shouldExtendBaseException() {

            CarNotFoundException exception = new CarNotFoundException("Test message");

            assertThat(exception).isInstanceOf(BaseException.class);
        }

        @Test
        @DisplayName("Should be throwable")
        void shouldBeThrowable() {

            assertThatThrownBy(() -> {
                throw new CarNotFoundException("Test message");
            }).isInstanceOf(CarNotFoundException.class)
              .hasMessage("Test message");
        }
    }
}
