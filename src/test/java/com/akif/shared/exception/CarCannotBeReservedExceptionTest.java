package com.akif.shared.exception;

import com.akif.car.internal.exception.CarCannotBeReservedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CarCannotBeReservedException Unit Tests")
class CarCannotBeReservedExceptionTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create exception with message and BAD_REQUEST status")
        void shouldCreateExceptionWithMessageAndBadRequestStatus() {

            String message = "Car cannot be reserved due to current status";

            CarCannotBeReservedException exception = new CarCannotBeReservedException(message);

            assertThat(exception.getErrorCode()).isEqualTo("CAR_CANNOT_BE_RESERVED");
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should create exception with custom HTTP status")
        void shouldCreateExceptionWithCustomHttpStatus() {

            String message = "Car reservation not allowed";
            HttpStatus customStatus = HttpStatus.FORBIDDEN;

            CarCannotBeReservedException exception = new CarCannotBeReservedException(message, customStatus);

            assertThat(exception.getErrorCode()).isEqualTo("CAR_CANNOT_BE_RESERVED");
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

            CarCannotBeReservedException exception = new CarCannotBeReservedException("Test message");

            assertThat(exception).isInstanceOf(BaseException.class);
        }

        @Test
        @DisplayName("Should be throwable")
        void shouldBeThrowable() {

            assertThatThrownBy(() -> {
                throw new CarCannotBeReservedException("Test reservation error");
            }).isInstanceOf(CarCannotBeReservedException.class)
              .hasMessage("Test reservation error");
        }
    }
}
