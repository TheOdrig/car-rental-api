package com.akif.service.damage;

import com.akif.event.DamageChargedEvent;
import com.akif.exception.DamageAssessmentException;
import com.akif.model.*;
import com.akif.repository.DamageReportRepository;
import com.akif.repository.PaymentRepository;
import com.akif.service.damage.impl.DamageChargeServiceImpl;
import com.akif.service.gateway.IPaymentGateway;
import com.akif.service.gateway.PaymentResult;
import com.akif.shared.enums.CurrencyType;
import com.akif.shared.enums.DamageStatus;
import com.akif.shared.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DamageChargeService Unit Tests")
class DamageChargeServiceTest {

    @Mock
    private DamageReportRepository damageReportRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private IPaymentGateway paymentGateway;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DamageChargeServiceImpl damageChargeService;

    private User testUser;
    private Car testCar;
    private Rental testRental;
    private DamageReport testDamageReport;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("customer")
                .email("customer@example.com")
                .build();

        testCar = Car.builder()
                .id(1L)
                .licensePlate("34ABC123")
                .brand("Toyota")
                .model("Corolla")
                .build();

        testRental = Rental.builder()
                .id(1L)
                .user(testUser)
                .car(testCar)
                .currency(CurrencyType.TRY)
                .build();

        testDamageReport = DamageReport.builder()
                .id(1L)
                .rental(testRental)
                .car(testCar)
                .description("Scratch on front bumper")
                .status(DamageStatus.ASSESSED)
                .customerLiability(new BigDecimal("500.00"))
                .reportedAt(LocalDateTime.now())
                .build();

        testPayment = Payment.builder()
                .id(1L)
                .rental(testRental)
                .amount(new BigDecimal("500.00"))
                .currency(CurrencyType.TRY)
                .status(PaymentStatus.PENDING)
                .paymentMethod("DAMAGE_CHARGE")
                .build();
    }

    @Nested
    @DisplayName("Create Damage Charge Operations")
    class CreateDamageChargeOperations {

        @Test
        @DisplayName("Should create damage charge successfully")
        void shouldCreateDamageChargeSuccessfully() {
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            Payment result = damageChargeService.createDamageCharge(testDamageReport);

            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualTo(new BigDecimal("500.00"));
            assertThat(result.getPaymentMethod()).isEqualTo("DAMAGE_CHARGE");

            verify(paymentRepository).save(any(Payment.class));
            verify(damageReportRepository).save(testDamageReport);
        }

        @Test
        @DisplayName("Should set payment ID on damage report")
        void shouldSetPaymentIdOnDamageReport() {
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            damageChargeService.createDamageCharge(testDamageReport);

            ArgumentCaptor<DamageReport> captor = ArgumentCaptor.forClass(DamageReport.class);
            verify(damageReportRepository).save(captor.capture());

            DamageReport savedReport = captor.getValue();
            assertThat(savedReport.getPaymentId()).isEqualTo(1L);
            assertThat(savedReport.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING.name());
        }

        @Test
        @DisplayName("Should throw exception when damage cannot be charged")
        void shouldThrowExceptionWhenDamageCannotBeCharged() {
            testDamageReport.setStatus(DamageStatus.REPORTED);

            assertThatThrownBy(() -> damageChargeService.createDamageCharge(testDamageReport))
                    .isInstanceOf(DamageAssessmentException.class);

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when customer liability is null")
        void shouldThrowExceptionWhenCustomerLiabilityIsNull() {
            testDamageReport.setCustomerLiability(null);

            assertThatThrownBy(() -> damageChargeService.createDamageCharge(testDamageReport))
                    .isInstanceOf(DamageAssessmentException.class)
                    .hasMessageContaining("Customer liability must be positive");
        }

        @Test
        @DisplayName("Should throw exception when customer liability is zero")
        void shouldThrowExceptionWhenCustomerLiabilityIsZero() {
            testDamageReport.setCustomerLiability(BigDecimal.ZERO);

            assertThatThrownBy(() -> damageChargeService.createDamageCharge(testDamageReport))
                    .isInstanceOf(DamageAssessmentException.class)
                    .hasMessageContaining("Customer liability must be positive");
        }

        @Test
        @DisplayName("Should throw exception when customer liability is negative")
        void shouldThrowExceptionWhenCustomerLiabilityIsNegative() {
            testDamageReport.setCustomerLiability(new BigDecimal("-100"));

            assertThatThrownBy(() -> damageChargeService.createDamageCharge(testDamageReport))
                    .isInstanceOf(DamageAssessmentException.class)
                    .hasMessageContaining("Customer liability must be positive");
        }
    }

    @Nested
    @DisplayName("Charge Damage Operations")
    class ChargeDamageOperations {

        @Test
        @DisplayName("Should charge damage successfully")
        void shouldChargeDamageSuccessfully() {
            PaymentResult successResult = new PaymentResult(true, "txn_123456", "Payment successful");

            when(damageReportRepository.findByPaymentId(1L)).thenReturn(Optional.of(testDamageReport));
            when(paymentGateway.authorize(any(), any(), anyString())).thenReturn(successResult);
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            PaymentResult result = damageChargeService.chargeDamage(testPayment);

            assertThat(result.success()).isTrue();
            assertThat(result.transactionId()).isEqualTo("txn_123456");

            verify(paymentRepository).save(testPayment);
            verify(damageReportRepository).save(testDamageReport);
            verify(eventPublisher).publishEvent(any(DamageChargedEvent.class));
        }

        @Test
        @DisplayName("Should update payment and report status on successful charge")
        void shouldUpdatePaymentAndReportStatusOnSuccessfulCharge() {
            PaymentResult successResult = new PaymentResult(true, "txn_123456", "Payment successful");

            when(damageReportRepository.findByPaymentId(1L)).thenReturn(Optional.of(testDamageReport));
            when(paymentGateway.authorize(any(), any(), anyString())).thenReturn(successResult);
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            damageChargeService.chargeDamage(testPayment);

            assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
            assertThat(testPayment.getTransactionId()).isEqualTo("txn_123456");

            ArgumentCaptor<DamageReport> captor = ArgumentCaptor.forClass(DamageReport.class);
            verify(damageReportRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(DamageStatus.CHARGED);
        }

        @Test
        @DisplayName("Should publish DamageChargedEvent on successful charge")
        void shouldPublishDamageChargedEventOnSuccessfulCharge() {
            PaymentResult successResult = new PaymentResult(true, "txn_123456", "Payment successful");

            when(damageReportRepository.findByPaymentId(1L)).thenReturn(Optional.of(testDamageReport));
            when(paymentGateway.authorize(any(), any(), anyString())).thenReturn(successResult);
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            damageChargeService.chargeDamage(testPayment);

            ArgumentCaptor<DamageChargedEvent> eventCaptor = ArgumentCaptor.forClass(DamageChargedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            DamageChargedEvent event = eventCaptor.getValue();
            assertThat(event.getDamageReport()).isEqualTo(testDamageReport);
            assertThat(event.getPayment()).isEqualTo(testPayment);
        }

        @Test
        @DisplayName("Should handle failed charge")
        void shouldHandleFailedCharge() {
            PaymentResult failedResult = new PaymentResult(false, null, "Insufficient funds");

            when(damageReportRepository.findByPaymentId(1L)).thenReturn(Optional.of(testDamageReport));
            when(paymentGateway.authorize(any(), any(), anyString())).thenReturn(failedResult);
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            PaymentResult result = damageChargeService.chargeDamage(testPayment);

            assertThat(result.success()).isFalse();
            verify(eventPublisher, never()).publishEvent(any(DamageChargedEvent.class));
        }

        @Test
        @DisplayName("Should throw exception when damage report not found for payment")
        void shouldThrowExceptionWhenDamageReportNotFoundForPayment() {
            when(damageReportRepository.findByPaymentId(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> damageChargeService.chargeDamage(testPayment))
                    .isInstanceOf(DamageAssessmentException.class)
                    .hasMessageContaining("Damage report not found for payment");
        }
    }

    @Nested
    @DisplayName("Handle Failed Damage Charge Operations")
    class HandleFailedDamageChargeOperations {

        @Test
        @DisplayName("Should update payment status to PENDING on failure")
        void shouldUpdatePaymentStatusToPendingOnFailure() {
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            damageChargeService.handleFailedDamageCharge(testPayment);

            assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(testPayment.getFailureReason()).isEqualTo("Payment authorization failed");
            verify(paymentRepository).save(testPayment);
        }

        @Test
        @DisplayName("Should set failure reason on payment")
        void shouldSetFailureReasonOnPayment() {
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            damageChargeService.handleFailedDamageCharge(testPayment);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());

            Payment savedPayment = captor.getValue();
            assertThat(savedPayment.getFailureReason()).isNotNull();
            assertThat(savedPayment.getFailureReason()).contains("failed");
        }
    }
}
