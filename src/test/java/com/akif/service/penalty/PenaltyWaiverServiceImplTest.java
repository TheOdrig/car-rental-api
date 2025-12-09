package com.akif.service.penalty;

import com.akif.shared.enums.CurrencyType;
import com.akif.shared.enums.PaymentStatus;
import com.akif.exception.PenaltyWaiverException;
import com.akif.exception.RentalNotFoundException;
import com.akif.model.Car;
import com.akif.model.Payment;
import com.akif.model.PenaltyWaiver;
import com.akif.model.Rental;
import com.akif.model.User;
import com.akif.repository.PaymentRepository;
import com.akif.repository.PenaltyWaiverRepository;
import com.akif.repository.RentalRepository;
import com.akif.service.gateway.IPaymentGateway;
import com.akif.service.gateway.PaymentResult;
import com.akif.service.penalty.impl.PenaltyWaiverServiceImpl;
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
    private PaymentRepository paymentRepository;

    @Mock
    private IPaymentGateway paymentGateway;

    @InjectMocks
    private PenaltyWaiverServiceImpl service;

    private Rental testRental;
    private User testUser;
    private Car testCar;
    private Payment testPayment;
    private PenaltyWaiver testWaiver;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        testCar = Car.builder()
                .id(1L)
                .brand("Toyota")
                .model("Corolla")
                .licensePlate("34ABC123")
                .build();

        testRental = Rental.builder()
                .id(1L)
                .user(testUser)
                .car(testCar)
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().minusDays(2))
                .dailyPrice(new BigDecimal("500.00"))
                .currency(CurrencyType.TRY)
                .penaltyAmount(new BigDecimal("200.00"))
                .penaltyPaid(false)
                .build();

        testPayment = Payment.builder()
                .id(1L)
                .rental(testRental)
                .amount(new BigDecimal("200.00"))
                .currency(CurrencyType.TRY)
                .status(PaymentStatus.AUTHORIZED)
                .transactionId("txn_123")
                .paymentMethod("PENALTY")
                .build();

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
            testPayment.updateStatus(PaymentStatus.CAPTURED);
            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(penaltyWaiverRepository.save(any(PenaltyWaiver.class))).thenReturn(testWaiver);
            when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);
            when(paymentRepository.findByRentalIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
            
            PaymentResult refundResult = PaymentResult.success("refund_123", "Refund successful");
            when(paymentGateway.refund(anyString(), any(BigDecimal.class))).thenReturn(refundResult);

            service.waivePenalty(1L, new BigDecimal("50.00"), "Refund reason", 100L);

            verify(paymentGateway, times(1)).refund(eq("txn_123"), eq(new BigDecimal("50.00")));
        }

        @Test
        @DisplayName("Should update waiver with refund transaction ID")
        void shouldUpdateWaiverWithRefundTransactionId() {
            testPayment.updateStatus(PaymentStatus.CAPTURED);
            when(paymentRepository.findByRentalIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testPayment));
            when(penaltyWaiverRepository.save(any(PenaltyWaiver.class))).thenReturn(testWaiver);
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
            
            PaymentResult refundResult = PaymentResult.success("refund_456", "Refund successful");
            when(paymentGateway.refund(anyString(), any(BigDecimal.class))).thenReturn(refundResult);

            service.processRefundForWaiver(testWaiver);

            assertThat(testWaiver.getRefundInitiated()).isTrue();
            assertThat(testWaiver.getRefundTransactionId()).isEqualTo("refund_456");
        }

        @Test
        @DisplayName("Should not process refund if already initiated")
        void shouldNotProcessRefundIfAlreadyInitiated() {
            testWaiver.setRefundInitiated(true);

            service.processRefundForWaiver(testWaiver);

            verify(paymentGateway, never()).refund(anyString(), any(BigDecimal.class));
        }

        @Test
        @DisplayName("Should handle refund failure gracefully")
        void shouldHandleRefundFailureGracefully() {
            testPayment.updateStatus(PaymentStatus.CAPTURED);
            when(paymentRepository.findByRentalIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testPayment));
            
            PaymentResult refundFailure = PaymentResult.failure("Refund failed");
            when(paymentGateway.refund(anyString(), any(BigDecimal.class))).thenReturn(refundFailure);

            assertThatThrownBy(() -> service.processRefundForWaiver(testWaiver))
                    .isInstanceOf(PenaltyWaiverException.class)
                    .hasMessageContaining("Failed to process refund");
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
