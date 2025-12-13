package com.akif.rental.unit.penalty;

import com.akif.payment.api.PaymentDto;
import com.akif.payment.api.PaymentResult;
import com.akif.payment.api.PaymentService;
import com.akif.payment.api.PaymentStatus;
import com.akif.rental.domain.model.PenaltyWaiver;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.exception.PenaltyWaiverException;
import com.akif.rental.internal.exception.RentalNotFoundException;
import com.akif.rental.internal.repository.PenaltyWaiverRepository;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.rental.internal.service.penalty.impl.PenaltyWaiverServiceImpl;
import com.akif.shared.enums.CurrencyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PenaltyWaiverServiceImpl Unit Tests")
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class PenaltyWaiverServiceImplTest {

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private PenaltyWaiverRepository penaltyWaiverRepository;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PenaltyWaiverServiceImpl service;

    private Rental testRental;
    private PaymentDto testPaymentDto;
    private PenaltyWaiver testWaiver;

    @BeforeEach
    void setUp() {
        testRental = Rental.builder()
                .id(1L)
                .userId(1L)
                .userEmail("test@example.com")
                .userFullName("Test User")
                .carId(1L)
                .carBrand("Toyota")
                .carModel("Corolla")
                .carLicensePlate("34ABC123")
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().minusDays(2))
                .dailyPrice(new BigDecimal("500.00"))
                .currency(CurrencyType.TRY)
                .penaltyAmount(new BigDecimal("200.00"))
                .penaltyPaid(false)
                .build();

        testPaymentDto = new PaymentDto(
                1L,                           // id
                testRental.getId(),           // rentalId
                testRental.getUserEmail(),    // userEmail
                testRental.getCarLicensePlate(), // carLicensePlate
                new BigDecimal("200.00"),     // amount
                CurrencyType.TRY,             // currency
                PaymentStatus.CAPTURED,       // status
                "PENALTY",                    // paymentMethod
                "txn_123",                    // transactionId
                null,                         // stripeSessionId
                null,                         // stripePaymentIntentId
                null                          // failureReason
        );

        testWaiver = PenaltyWaiver.builder()
                .id(1L)
                .rental(testRental)
                .originalPenalty(new BigDecimal("200.00"))
                .waivedAmount(new BigDecimal("50.00"))
                .remainingPenalty(new BigDecimal("150.00"))
                .reason("Customer complaint")
                .adminId(100L)
                .refundInitiated(false)
                .build();
    }

    @Nested
    @DisplayName("Full Waiver")
    class FullWaiver {

        @Test
        @DisplayName("Should waive full penalty successfully")
        void shouldWaiveFullPenaltySuccessfully() {
            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(penaltyWaiverRepository.save(any(PenaltyWaiver.class))).thenReturn(testWaiver);
            when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);

            PenaltyWaiver result = service.waiveFullPenalty(1L, "Customer satisfaction", 100L);

            assertThat(result).isNotNull();
            verify(penaltyWaiverRepository, times(1)).save(any(PenaltyWaiver.class));
            verify(rentalRepository, times(1)).save(testRental);
        }

        @Test
        @DisplayName("Should set remaining penalty to zero for full waiver")
        void shouldSetRemainingPenaltyToZeroForFullWaiver() {
            PenaltyWaiver fullWaiver = PenaltyWaiver.builder()
                    .id(1L)
                    .rental(testRental)
                    .originalPenalty(new BigDecimal("200.00"))
                    .waivedAmount(new BigDecimal("200.00"))
                    .remainingPenalty(BigDecimal.ZERO)
                    .reason("Full waiver")
                    .adminId(100L)
                    .build();

            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(penaltyWaiverRepository.save(any(PenaltyWaiver.class))).thenReturn(fullWaiver);
            when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);

            service.waiveFullPenalty(1L, "Full waiver", 100L);

            assertThat(testRental.getPenaltyAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should throw exception when rental has no penalty for full waiver")
        void shouldThrowExceptionWhenRentalHasNoPenaltyForFullWaiver() {
            testRental.setPenaltyAmount(BigDecimal.ZERO);
            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));

            assertThatThrownBy(() -> service.waiveFullPenalty(1L, "Reason", 100L))
                    .isInstanceOf(PenaltyWaiverException.class)
                    .hasMessageContaining("Rental has no penalty to waive");
        }
    }

    @Nested
    @DisplayName("Partial Waiver Calculation")
    class PartialWaiverCalculation {

        @Test
        @DisplayName("Should calculate remaining penalty correctly for partial waiver")
        void shouldCalculateRemainingPenaltyCorrectlyForPartialWaiver() {
            BigDecimal waiverAmount = new BigDecimal("50.00");
            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(penaltyWaiverRepository.save(any(PenaltyWaiver.class))).thenReturn(testWaiver);
            when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);

            service.waivePenalty(1L, waiverAmount, "Partial waiver", 100L);

            BigDecimal expectedRemaining = new BigDecimal("150.00");
            assertThat(testRental.getPenaltyAmount()).isEqualByComparingTo(expectedRemaining);
        }

        @Test
        @DisplayName("Should waive 75% of penalty correctly")
        void shouldWaive75PercentOfPenaltyCorrectly() {
            BigDecimal waiverAmount = new BigDecimal("150.00");
            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(penaltyWaiverRepository.save(any(PenaltyWaiver.class))).thenReturn(testWaiver);
            when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);

            service.waivePenalty(1L, waiverAmount, "Partial waiver", 100L);

            BigDecimal expectedRemaining = new BigDecimal("50.00");
            assertThat(testRental.getPenaltyAmount()).isEqualByComparingTo(expectedRemaining);
        }

        @Test
        @DisplayName("Should throw exception when waiver amount exceeds penalty")
        void shouldThrowExceptionWhenWaiverAmountExceedsPenalty() {
            BigDecimal excessiveWaiver = new BigDecimal("300.00");
            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));

            assertThatThrownBy(() -> service.waivePenalty(1L, excessiveWaiver, "Reason", 100L))
                    .isInstanceOf(PenaltyWaiverException.class)
                    .hasMessageContaining("cannot exceed penalty amount");
        }
    }

    @Nested
    @DisplayName("Waiver After Payment (Refund)")
    class WaiverAfterPaymentRefund {

        @Test
        @DisplayName("Should initiate refund when penalty is already paid")
        void shouldInitiateRefundWhenPenaltyIsAlreadyPaid() {
            testRental.setPenaltyPaid(true);
            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(penaltyWaiverRepository.save(any(PenaltyWaiver.class))).thenReturn(testWaiver);
            when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);
            when(paymentService.getPaymentByRentalId(1L)).thenReturn(Optional.of(testPaymentDto));
            when(paymentService.refundPayment(eq(1L), any(BigDecimal.class)))
                    .thenReturn(PaymentResult.success("refund_123", "Refund successful"));

            service.waivePenalty(1L, new BigDecimal("50.00"), "Refund reason", 100L);

            verify(paymentService).refundPayment(eq(1L), eq(new BigDecimal("50.00")));
        }

        @Test
        @DisplayName("Should update waiver with refund transaction ID")
        void shouldUpdateWaiverWithRefundTransactionId() {
            when(paymentService.getPaymentByRentalId(1L)).thenReturn(Optional.of(testPaymentDto));
            when(paymentService.refundPayment(eq(1L), any(BigDecimal.class)))
                    .thenReturn(PaymentResult.success("refund_456", "Refund successful"));
            when(penaltyWaiverRepository.save(any(PenaltyWaiver.class))).thenReturn(testWaiver);

            service.processRefundForWaiver(testWaiver);

            assertThat(testWaiver.getRefundInitiated()).isTrue();
            assertThat(testWaiver.getRefundTransactionId()).isEqualTo("refund_456");
        }

        @Test
        @DisplayName("Should not process refund if already initiated")
        void shouldNotProcessRefundIfAlreadyInitiated() {
            testWaiver.setRefundInitiated(true);

            service.processRefundForWaiver(testWaiver);

            verify(paymentService, never()).refundPayment(any(), any());
        }

        @Test
        @DisplayName("Should handle refund failure gracefully")
        void shouldHandleRefundFailureGracefully() {
            when(paymentService.getPaymentByRentalId(1L)).thenReturn(Optional.of(testPaymentDto));
            when(paymentService.refundPayment(eq(1L), any(BigDecimal.class)))
                    .thenReturn(PaymentResult.failure("Refund failed"));

            assertThatThrownBy(() -> service.processRefundForWaiver(testWaiver))
                    .isInstanceOf(PenaltyWaiverException.class)
                    .hasMessageContaining("Refund failed");
        }
    }

    @Nested
    @DisplayName("Invalid Waiver Amount")
    class InvalidWaiverAmount {

        @Test
        @DisplayName("Should throw exception for null waiver amount")
        void shouldThrowExceptionForNullWaiverAmount() {
            assertThatThrownBy(() -> service.waivePenalty(1L, null, "Reason", 100L))
                    .isInstanceOf(PenaltyWaiverException.class)
                    .hasMessageContaining("Waiver amount must be positive");
        }

        @Test
        @DisplayName("Should throw exception for zero waiver amount")
        void shouldThrowExceptionForZeroWaiverAmount() {
            assertThatThrownBy(() -> service.waivePenalty(1L, BigDecimal.ZERO, "Reason", 100L))
                    .isInstanceOf(PenaltyWaiverException.class)
                    .hasMessageContaining("Waiver amount must be positive");
        }

        @Test
        @DisplayName("Should throw exception for negative waiver amount")
        void shouldThrowExceptionForNegativeWaiverAmount() {
            assertThatThrownBy(() -> service.waivePenalty(1L, new BigDecimal("-10.00"), "Reason", 100L))
                    .isInstanceOf(PenaltyWaiverException.class)
                    .hasMessageContaining("Waiver amount must be positive");
        }

        @Test
        @DisplayName("Should throw exception for null rental ID")
        void shouldThrowExceptionForNullRentalId() {
            assertThatThrownBy(() -> service.waivePenalty(null, new BigDecimal("50.00"), "Reason", 100L))
                    .isInstanceOf(PenaltyWaiverException.class)
                    .hasMessageContaining("Rental ID cannot be null");
        }

        @Test
        @DisplayName("Should throw exception for null reason")
        void shouldThrowExceptionForNullReason() {
            assertThatThrownBy(() -> service.waivePenalty(1L, new BigDecimal("50.00"), null, 100L))
                    .isInstanceOf(PenaltyWaiverException.class)
                    .hasMessageContaining("Waiver reason is mandatory");
        }

        @Test
        @DisplayName("Should throw exception for empty reason")
        void shouldThrowExceptionForEmptyReason() {
            assertThatThrownBy(() -> service.waivePenalty(1L, new BigDecimal("50.00"), "   ", 100L))
                    .isInstanceOf(PenaltyWaiverException.class)
                    .hasMessageContaining("Waiver reason is mandatory");
        }

        @Test
        @DisplayName("Should throw exception for null admin ID")
        void shouldThrowExceptionForNullAdminId() {
            assertThatThrownBy(() -> service.waivePenalty(1L, new BigDecimal("50.00"), "Reason", null))
                    .isInstanceOf(PenaltyWaiverException.class)
                    .hasMessageContaining("Admin ID cannot be null");
        }
    }

    @Nested
    @DisplayName("Penalty History")
    class PenaltyHistory {

        @Test
        @DisplayName("Should return penalty history for rental")
        void shouldReturnPenaltyHistoryForRental() {
            PenaltyWaiver waiver1 = PenaltyWaiver.builder().id(1L).waivedAmount(new BigDecimal("50.00")).build();
            PenaltyWaiver waiver2 = PenaltyWaiver.builder().id(2L).waivedAmount(new BigDecimal("30.00")).build();
            List<PenaltyWaiver> history = Arrays.asList(waiver1, waiver2);

            when(rentalRepository.existsById(1L)).thenReturn(true);
            when(penaltyWaiverRepository.findByRentalIdAndIsDeletedFalse(1L)).thenReturn(history);

            List<PenaltyWaiver> result = service.getPenaltyHistory(1L);

            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(waiver1, waiver2);
        }

        @Test
        @DisplayName("Should return empty list when no waivers exist")
        void shouldReturnEmptyListWhenNoWaiversExist() {
            when(rentalRepository.existsById(1L)).thenReturn(true);
            when(penaltyWaiverRepository.findByRentalIdAndIsDeletedFalse(1L)).thenReturn(Collections.emptyList());

            List<PenaltyWaiver> result = service.getPenaltyHistory(1L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when rental not found")
        void shouldThrowExceptionWhenRentalNotFound() {
            when(rentalRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> service.getPenaltyHistory(999L))
                    .isInstanceOf(RentalNotFoundException.class)
                    .hasMessageContaining("Rental not found");
        }
    }
}
