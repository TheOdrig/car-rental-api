package com.akif.shared.exception;

import com.akif.car.internal.exception.CarCannotBeSoldException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CarCannotBeSoldException Unit Tests")
class CarCannotBeSoldExceptionTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create exception with message and BAD_REQUEST status")
        void shouldCreateExceptionWithMessageAndBadRequestStatus() {

            String message = "Car cannot be sold due to current status";

            CarCannotBeSoldException exception = new CarCannotBeSoldException(message);

            assertThat(exception.getErrorCode()).isEqualTo("CAR_CANNOT_BE_SOLD");
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should create exception with custom HTTP status")
        void shouldCreateExceptionWithCustomHttpStatus() {

            String message = "Car sale not allowed";
            HttpStatus customStatus = HttpStatus.FORBIDDEN;

            CarCannotBeSoldException exception = new CarCannotBeSoldException(message, customStatus);

            assertThat(exception.getErrorCode()).isEqualTo("CAR_CANNOT_BE_SOLD");
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("Inheritance Tests")
    class InheritanceTests {

        @Test
        @DisplayName("Should extend BaseException")
        void shouldExtendBaseException() {

            CarCannotBeSoldException exception = new CarCannotBeSoldException("Test message");

            assertThat(exception).isInstanceOf(BaseException.class);
        }

        @Test
        @DisplayName("Should be throwable")
        void shouldBeThrowable() {

            assertThatThrownBy(() -> {
                throw new CarCannotBeSoldException("Test sale error");
            }).isInstanceOf(CarCannotBeSoldException.class)
              .hasMessage("Test sale error");
        }
    }
}
