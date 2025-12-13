package com.akif.rental.unit.penalty;

import com.akif.payment.api.CreatePaymentRequest;
import com.akif.payment.api.PaymentDto;
import com.akif.payment.api.PaymentResult;
import com.akif.payment.api.PaymentService;
import com.akif.payment.api.PaymentStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.service.penalty.impl.PenaltyPaymentServiceImpl;
import com.akif.shared.enums.CurrencyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PenaltyPaymentService Unit Tests")
class PenaltyPaymentServiceImplTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PenaltyPaymentServiceImpl penaltyPaymentService;

    private Rental testRental;

    @BeforeEach
    void setUp() {
        testRental = Rental.builder()
                .id(1L)
                .userId(100L)
                .userEmail("customer@example.com")
                .userFullName("John Doe")
                .carId(200L)
                .carBrand("Toyota")
                .carModel("Corolla")
                .carLicensePlate("34ABC123")
                .currency(CurrencyType.TRY)
                .build();
    }

    @Nested
    @DisplayName("Create Penalty Payment Tests")
    class CreatePenaltyPaymentTests {

        @Test
        @DisplayName("Should create penalty payment successfully")
        void shouldCreatePenaltyPaymentSuccessfully() {
            BigDecimal penaltyAmount = new BigDecimal("150.00");
            PaymentDto expectedPayment = new PaymentDto(
                    1L,
                    testRental.getId(),
                    testRental.getUserEmail(),
                    testRental.getCarLicensePlate(),
                    penaltyAmount,
                    CurrencyType.TRY,
                    PaymentStatus.PENDING,
                    "PENALTY",
                    null,
                    null,
                    null,
                    null
            );

            when(paymentService.createPayment(any(CreatePaymentRequest.class))).thenReturn(expectedPayment);

            PaymentDto result = penaltyPaymentService.createPenaltyPayment(testRental, penaltyAmount);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.rentalId()).isEqualTo(testRental.getId());
            assertThat(result.amount()).isEqualTo(penaltyAmount);

            ArgumentCaptor<CreatePaymentRequest> captor = ArgumentCaptor.forClass(CreatePaymentRequest.class);
            verify(paymentService).createPayment(captor.capture());

            CreatePaymentRequest capturedRequest = captor.getValue();
            assertThat(capturedRequest.rentalId()).isEqualTo(testRental.getId());
            assertThat(capturedRequest.amount()).isEqualTo(penaltyAmount);
            assertThat(capturedRequest.paymentMethod()).isEqualTo("PENALTY");
        }

        @Test
        @DisplayName("Should throw exception for null penalty amount")
        void shouldThrowExceptionForNullPenaltyAmount() {
            assertThatThrownBy(() -> penaltyPaymentService.createPenaltyPayment(testRental, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Penalty amount must be positive");

            verifyNoInteractions(paymentService);
        }

        @Test
        @DisplayName("Should throw exception for zero penalty amount")
        void shouldThrowExceptionForZeroPenaltyAmount() {
            assertThatThrownBy(() -> penaltyPaymentService.createPenaltyPayment(testRental, BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Penalty amount must be positive");

            verifyNoInteractions(paymentService);
        }

        @Test
        @DisplayName("Should throw exception for negative penalty amount")
        void shouldThrowExceptionForNegativePenaltyAmount() {
            assertThatThrownBy(() -> penaltyPaymentService.createPenaltyPayment(testRental, new BigDecimal("-50.00")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Penalty amount must be positive");

            verifyNoInteractions(paymentService);
        }
    }

    @Nested
    @DisplayName("Charge Penalty Tests")
    class ChargePenaltyTests {

        @Test
        @DisplayName("Should charge penalty successfully")
        void shouldChargePenaltySuccessfully() {
            Long paymentId = 1L;
            Long userId = 100L;
            PaymentResult expectedResult = new PaymentResult(true, "TXN-12345", "Payment successful");

            when(paymentService.chargePayment(eq(paymentId), eq(userId.toString()))).thenReturn(expectedResult);

            PaymentResult result = penaltyPaymentService.chargePenalty(paymentId, userId);

            assertThat(result).isNotNull();
            assertThat(result.success()).isTrue();
            assertThat(result.transactionId()).isEqualTo("TXN-12345");

            verify(paymentService).chargePayment(paymentId, userId.toString());
        }

        @Test
        @DisplayName("Should return failed result when charge fails")
        void shouldReturnFailedResultWhenChargeFails() {
            Long paymentId = 1L;
            Long userId = 100L;
            PaymentResult expectedResult = new PaymentResult(false, null, "Insufficient funds");

            when(paymentService.chargePayment(eq(paymentId), eq(userId.toString()))).thenReturn(expectedResult);

            PaymentResult result = penaltyPaymentService.chargePenalty(paymentId, userId);

            assertThat(result).isNotNull();
            assertThat(result.success()).isFalse();
            assertThat(result.message()).isEqualTo("Insufficient funds");
        }
    }

    @Nested
    @DisplayName("Handle Failed Payment Tests")
    class HandleFailedPaymentTests {

        @Test
        @DisplayName("Should handle failed penalty payment without error")
        void shouldHandleFailedPenaltyPaymentWithoutError() {
            penaltyPaymentService.handleFailedPenaltyPayment(
                    1L,
                    testRental.getId(),
                    testRental.getUserEmail(),
                    "Card declined"
            );

        }
    }
}
