package com.akif.service.penalty;

import com.akif.enums.CurrencyType;
import com.akif.enums.PaymentStatus;
import com.akif.model.Car;
import com.akif.model.Payment;
import com.akif.model.Rental;
import com.akif.model.User;
import com.akif.repository.PaymentRepository;
import com.akif.service.gateway.IPaymentGateway;
import com.akif.service.gateway.PaymentResult;
import com.akif.service.penalty.impl.PenaltyPaymentServiceImpl;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PenaltyPaymentServiceImpl Unit Tests")
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class PenaltyPaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private IPaymentGateway paymentGateway;

    @InjectMocks
    private PenaltyPaymentServiceImpl service;

    private Rental testRental;
    private User testUser;
    private Car testCar;
    private Payment testPayment;

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
                .build();

        testPayment = Payment.builder()
                .id(1L)
                .rental(testRental)
                .amount(new BigDecimal("100.00"))
                .currency(CurrencyType.TRY)
                .status(PaymentStatus.PENDING)
                .paymentMethod("PENALTY")
                .build();
    }

    @Nested
    @DisplayName("Successful Penalty Payment")
    class SuccessfulPenaltyPayment {

        @Test
        @DisplayName("Should create penalty payment successfully")
        void shouldCreatePenaltyPaymentSuccessfully() {
            BigDecimal penaltyAmount = new BigDecimal("150.00");
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            Payment result = service.createPenaltyPayment(testRental, penaltyAmount);

            assertThat(result).isNotNull();
            verify(paymentRepository, times(1)).save(any(Payment.class));
        }

        @Test
        @DisplayName("Should charge penalty successfully")
        void shouldChargePenaltySuccessfully() {
            PaymentResult authorizeResult = PaymentResult.success("auth_123", "Authorized");
            PaymentResult captureResult = PaymentResult.success("txn_123", "Payment successful");
            when(paymentGateway.authorize(any(BigDecimal.class), any(CurrencyType.class), anyString()))
                    .thenReturn(authorizeResult);
            when(paymentGateway.capture(anyString(), any(BigDecimal.class)))
                    .thenReturn(captureResult);
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            PaymentResult result = service.chargePenalty(testPayment);

            assertThat(result.success()).isTrue();
            assertThat(result.transactionId()).isEqualTo("txn_123");
            verify(paymentGateway, times(1)).authorize(
                    eq(testPayment.getAmount()),
                    eq(testPayment.getCurrency()),
                    eq(testUser.getId().toString())
            );
            verify(paymentGateway, times(1)).capture(eq("auth_123"), eq(testPayment.getAmount()));
            verify(paymentRepository, times(1)).save(testPayment);
        }

        @Test
        @DisplayName("Should update payment status to CAPTURED on successful charge")
        void shouldUpdatePaymentStatusToCapturedOnSuccessfulCharge() {
            PaymentResult authorizeResult = PaymentResult.success("auth_456", "Authorized");
            PaymentResult captureResult = PaymentResult.success("txn_456", "Payment captured");
            when(paymentGateway.authorize(any(BigDecimal.class), any(CurrencyType.class), anyString()))
                    .thenReturn(authorizeResult);
            when(paymentGateway.capture(anyString(), any(BigDecimal.class)))
                    .thenReturn(captureResult);
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            service.chargePenalty(testPayment);

            assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
            assertThat(testPayment.getTransactionId()).isEqualTo("txn_456");
        }
    }

    @Nested
    @DisplayName("Failed Penalty Payment Handling")
    class FailedPenaltyPaymentHandling {

        @Test
        @DisplayName("Should handle payment gateway failure")
        void shouldHandlePaymentGatewayFailure() {
            PaymentResult failureResult = PaymentResult.failure("Insufficient funds");
            when(paymentGateway.authorize(any(BigDecimal.class), any(CurrencyType.class), anyString()))
                    .thenReturn(failureResult);
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            PaymentResult result = service.chargePenalty(testPayment);

            assertThat(result.success()).isFalse();
            assertThat(result.message()).isEqualTo("Insufficient funds");
            verify(paymentRepository, times(1)).save(testPayment);
        }

        @Test
        @DisplayName("Should update payment status to FAILED on gateway failure")
        void shouldUpdatePaymentStatusToFailedOnGatewayFailure() {
            PaymentResult failureResult = PaymentResult.failure("Card declined");
            when(paymentGateway.authorize(any(BigDecimal.class), any(CurrencyType.class), anyString()))
                    .thenReturn(failureResult);
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            service.chargePenalty(testPayment);

            assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(testPayment.getFailureReason()).isEqualTo("Card declined");
        }

        @Test
        @DisplayName("Should handle exception during payment processing")
        void shouldHandleExceptionDuringPaymentProcessing() {
            when(paymentGateway.authorize(any(BigDecimal.class), any(CurrencyType.class), anyString()))
                    .thenThrow(new RuntimeException("Network error"));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            PaymentResult result = service.chargePenalty(testPayment);

            assertThat(result.success()).isFalse();
            assertThat(result.message()).contains("Payment gateway error");
            assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("Should mark failed payment as PENDING for manual processing")
        void shouldMarkFailedPaymentAsPendingForManualProcessing() {
            testPayment.updateStatus(PaymentStatus.FAILED);
            testPayment.setFailureReason("Card declined");
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            service.handleFailedPenaltyPayment(testPayment);

            assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
            verify(paymentRepository, times(1)).save(testPayment);
        }
    }

    @Nested
    @DisplayName("Currency Consistency")
    class CurrencyConsistency {

        @Test
        @DisplayName("Should use same currency as rental for penalty payment")
        void shouldUseSameCurrencyAsRentalForPenaltyPayment() {
            testRental.setCurrency(CurrencyType.USD);
            BigDecimal penaltyAmount = new BigDecimal("50.00");
            
            Payment savedPayment = Payment.builder()
                    .id(1L)
                    .rental(testRental)
                    .amount(penaltyAmount)
                    .currency(CurrencyType.USD)
                    .status(PaymentStatus.PENDING)
                    .build();
            
            when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

            Payment result = service.createPenaltyPayment(testRental, penaltyAmount);

            assertThat(result.getCurrency()).isEqualTo(CurrencyType.USD);
            assertThat(result.getCurrency()).isEqualTo(testRental.getCurrency());
        }

        @Test
        @DisplayName("Should charge penalty in rental currency")
        void shouldChargePenaltyInRentalCurrency() {
            testRental.setCurrency(CurrencyType.EUR);
            testPayment.setCurrency(CurrencyType.EUR);
            
            PaymentResult authorizeResult = PaymentResult.success("auth_789", "Authorized");
            PaymentResult captureResult = PaymentResult.success("txn_789", "Payment captured");
            when(paymentGateway.authorize(any(BigDecimal.class), eq(CurrencyType.EUR), anyString()))
                    .thenReturn(authorizeResult);
            when(paymentGateway.capture(anyString(), any(BigDecimal.class)))
                    .thenReturn(captureResult);
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            service.chargePenalty(testPayment);

            verify(paymentGateway, times(1)).authorize(
                    any(BigDecimal.class),
                    eq(CurrencyType.EUR),
                    anyString()
            );
            verify(paymentGateway, times(1)).capture(anyString(), any(BigDecimal.class));
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Should throw exception for null penalty amount")
        void shouldThrowExceptionForNullPenaltyAmount() {
            assertThatThrownBy(() -> service.createPenaltyPayment(testRental, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Penalty amount must be positive");
        }

        @Test
        @DisplayName("Should throw exception for zero penalty amount")
        void shouldThrowExceptionForZeroPenaltyAmount() {
            assertThatThrownBy(() -> service.createPenaltyPayment(testRental, BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Penalty amount must be positive");
        }

        @Test
        @DisplayName("Should throw exception for negative penalty amount")
        void shouldThrowExceptionForNegativePenaltyAmount() {
            assertThatThrownBy(() -> service.createPenaltyPayment(testRental, new BigDecimal("-10.00")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Penalty amount must be positive");
        }

        @Test
        @DisplayName("Should throw exception when rental is null")
        void shouldThrowExceptionWhenRentalIsNull() {
            testPayment.setRental(null);

            assertThatThrownBy(() -> service.chargePenalty(testPayment))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Penalty payment must have associated rental and user");
        }

        @Test
        @DisplayName("Should throw exception when user is null")
        void shouldThrowExceptionWhenUserIsNull() {
            testRental.setUser(null);

            assertThatThrownBy(() -> service.chargePenalty(testPayment))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Penalty payment must have associated rental and user");
        }
    }
}
