package com.akif.shared.exception;

import com.akif.car.internal.exception.CarOperationNotAllowedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CarOperationNotAllowedException Unit Tests")
class CarOperationNotAllowedExceptionTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create exception with message and FORBIDDEN status")
        void shouldCreateExceptionWithMessageAndForbiddenStatus() {

            String message = "Car operation not allowed";

            CarOperationNotAllowedException exception = new CarOperationNotAllowedException(message);

            assertThat(exception.getErrorCode()).isEqualTo("CAR_OPERATION_NOT_ALLOWED");
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Should create exception with custom HTTP status")
        void shouldCreateExceptionWithCustomHttpStatus() {

            String message = "Operation not permitted";
            HttpStatus customStatus = HttpStatus.BAD_REQUEST;

            CarOperationNotAllowedException exception = new CarOperationNotAllowedException(message, customStatus);

            assertThat(exception.getErrorCode()).isEqualTo("CAR_OPERATION_NOT_ALLOWED");
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

            CarOperationNotAllowedException exception = new CarOperationNotAllowedException("Test message");

            assertThat(exception).isInstanceOf(BaseException.class);
        }

        @Test
        @DisplayName("Should be throwable")
        void shouldBeThrowable() {

            assertThatThrownBy(() -> {
                throw new CarOperationNotAllowedException("Test operation error");
            }).isInstanceOf(CarOperationNotAllowedException.class)
              .hasMessage("Test operation error");
        }
    }
}
