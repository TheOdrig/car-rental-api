package com.akif.service.damage;

import com.akif.dto.damage.request.DamageDisputeRequestDto;
import com.akif.dto.damage.request.DamageDisputeResolutionDto;
import com.akif.dto.damage.response.DamageDisputeResponseDto;
import com.akif.enums.*;
import com.akif.event.DamageDisputedEvent;
import com.akif.event.DamageResolvedEvent;
import com.akif.exception.DamageDisputeException;
import com.akif.exception.DamageReportException;
import com.akif.model.*;
import com.akif.repository.DamageReportRepository;
import com.akif.repository.PaymentRepository;
import com.akif.repository.UserRepository;
import com.akif.service.damage.impl.DamageDisputeServiceImpl;
import com.akif.service.gateway.IPaymentGateway;
import com.akif.service.gateway.PaymentResult;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DamageDisputeService Unit Tests")
class DamageDisputeServiceTest {

    @Mock
    private DamageReportRepository damageReportRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IPaymentGateway paymentGateway;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DamageDisputeServiceImpl damageDisputeService;

    private User testUser;
    private Car testCar;
    private Rental testRental;
    private DamageReport testDamageReport;
    private Payment testPayment;
    private DamageDisputeRequestDto testDisputeRequest;
    private DamageDisputeResolutionDto testResolutionDto;

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
                .status(DamageStatus.CHARGED)
                .customerLiability(new BigDecimal("500.00"))
                .repairCostEstimate(new BigDecimal("500.00"))
                .paymentId(1L)
                .reportedAt(LocalDateTime.now())
                .build();

        testPayment = Payment.builder()
                .id(1L)
                .rental(testRental)
                .amount(new BigDecimal("500.00"))
                .currency(CurrencyType.TRY)
                .status(PaymentStatus.CAPTURED)
                .transactionId("txn_123456")
                .paymentMethod("DAMAGE_CHARGE")
                .build();

        testDisputeRequest = new DamageDisputeRequestDto(
                "The damage was pre-existing",
                "Photos show the damage was there before rental"
        );

        testResolutionDto = new DamageDisputeResolutionDto(
                new BigDecimal("250.00"),
                new BigDecimal("250.00"),
                "Partial refund approved - damage was partially pre-existing"
        );
    }

    @Nested
    @DisplayName("Create Dispute Operations")
    class CreateDisputeOperations {

        @Test
        @DisplayName("Should create dispute successfully")
        void shouldCreateDisputeSuccessfully() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(userRepository.findByUsernameAndIsDeletedFalse("customer")).thenReturn(Optional.of(testUser));
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            DamageDisputeResponseDto result = damageDisputeService.createDispute(1L, testDisputeRequest, "customer");

            assertThat(result).isNotNull();
            assertThat(result.damageId()).isEqualTo(1L);
            assertThat(result.status()).isEqualTo(DamageStatus.DISPUTED);
            assertThat(result.disputeReason()).isEqualTo("The damage was pre-existing");

            verify(damageReportRepository).save(any(DamageReport.class));
            verify(eventPublisher).publishEvent(any(DamageDisputedEvent.class));
        }

        @Test
        @DisplayName("Should set dispute details on damage report")
        void shouldSetDisputeDetailsOnDamageReport() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(userRepository.findByUsernameAndIsDeletedFalse("customer")).thenReturn(Optional.of(testUser));
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            damageDisputeService.createDispute(1L, testDisputeRequest, "customer");

            ArgumentCaptor<DamageReport> captor = ArgumentCaptor.forClass(DamageReport.class);
            verify(damageReportRepository).save(captor.capture());

            DamageReport savedReport = captor.getValue();
            assertThat(savedReport.getDisputeReason()).isEqualTo("The damage was pre-existing");
            assertThat(savedReport.getDisputeComments()).isEqualTo("Photos show the damage was there before rental");
            assertThat(savedReport.getDisputedBy()).isEqualTo(1L);
            assertThat(savedReport.getDisputedAt()).isNotNull();
            assertThat(savedReport.getStatus()).isEqualTo(DamageStatus.DISPUTED);
        }

        @Test
        @DisplayName("Should throw exception when damage report not found")
        void shouldThrowExceptionWhenDamageReportNotFound() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> damageDisputeService.createDispute(999L, testDisputeRequest, "customer"))
                    .isInstanceOf(DamageReportException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should throw exception when damage cannot be disputed")
        void shouldThrowExceptionWhenDamageCannotBeDisputed() {
            testDamageReport.setStatus(DamageStatus.REPORTED);

            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(userRepository.findByUsernameAndIsDeletedFalse("customer")).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> damageDisputeService.createDispute(1L, testDisputeRequest, "customer"))
                    .isInstanceOf(DamageDisputeException.class);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(userRepository.findByUsernameAndIsDeletedFalse("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> damageDisputeService.createDispute(1L, testDisputeRequest, "unknown"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("Should publish DamageDisputedEvent when dispute created")
        void shouldPublishDamageDisputedEventWhenDisputeCreated() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(userRepository.findByUsernameAndIsDeletedFalse("customer")).thenReturn(Optional.of(testUser));
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            damageDisputeService.createDispute(1L, testDisputeRequest, "customer");

            ArgumentCaptor<DamageDisputedEvent> eventCaptor = ArgumentCaptor.forClass(DamageDisputedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            DamageDisputedEvent event = eventCaptor.getValue();
            assertThat(event.getDamageReport()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Resolve Dispute Operations")
    class ResolveDisputeOperations {

        @BeforeEach
        void setupDisputedStatus() {
            testDamageReport.setStatus(DamageStatus.DISPUTED);
        }

        @Test
        @DisplayName("Should resolve dispute successfully with partial refund")
        void shouldResolveDisputeSuccessfullyWithPartialRefund() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(userRepository.findByUsernameAndIsDeletedFalse("admin")).thenReturn(Optional.of(testUser));
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
            when(paymentGateway.refund(anyString(), any(BigDecimal.class)))
                    .thenReturn(new PaymentResult(true, "refund_123", "Refund successful"));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            DamageDisputeResponseDto result = damageDisputeService.resolveDispute(1L, testResolutionDto, "admin");

            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(DamageStatus.RESOLVED);
            assertThat(result.refundAmount()).isEqualTo(new BigDecimal("250.00"));

            verify(paymentGateway).refund("txn_123456", new BigDecimal("250.00"));
            verify(eventPublisher).publishEvent(any(DamageResolvedEvent.class));
        }

        @Test
        @DisplayName("Should update damage report with resolution details")
        void shouldUpdateDamageReportWithResolutionDetails() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(userRepository.findByUsernameAndIsDeletedFalse("admin")).thenReturn(Optional.of(testUser));
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
            when(paymentGateway.refund(anyString(), any(BigDecimal.class)))
                    .thenReturn(new PaymentResult(true, "refund_123", "Refund successful"));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            damageDisputeService.resolveDispute(1L, testResolutionDto, "admin");

            ArgumentCaptor<DamageReport> captor = ArgumentCaptor.forClass(DamageReport.class);
            verify(damageReportRepository).save(captor.capture());

            DamageReport savedReport = captor.getValue();
            assertThat(savedReport.getCustomerLiability()).isEqualTo(new BigDecimal("250.00"));
            assertThat(savedReport.getRepairCostEstimate()).isEqualTo(new BigDecimal("250.00"));
            assertThat(savedReport.getResolutionNotes()).contains("Partial refund approved");
            assertThat(savedReport.getResolvedBy()).isEqualTo(1L);
            assertThat(savedReport.getResolvedAt()).isNotNull();
            assertThat(savedReport.getStatus()).isEqualTo(DamageStatus.RESOLVED);
        }

        @Test
        @DisplayName("Should throw exception when damage is not in DISPUTED status")
        void shouldThrowExceptionWhenDamageIsNotInDisputedStatus() {
            testDamageReport.setStatus(DamageStatus.CHARGED);

            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(userRepository.findByUsernameAndIsDeletedFalse("admin")).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> damageDisputeService.resolveDispute(1L, testResolutionDto, "admin"))
                    .isInstanceOf(DamageDisputeException.class);
        }

        @Test
        @DisplayName("Should not refund when adjusted liability equals original")
        void shouldNotRefundWhenAdjustedLiabilityEqualsOriginal() {
            DamageDisputeResolutionDto noRefundResolution = new DamageDisputeResolutionDto(
                    new BigDecimal("500.00"),
                    new BigDecimal("500.00"),
                    "Dispute rejected - no refund"
            );

            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(userRepository.findByUsernameAndIsDeletedFalse("admin")).thenReturn(Optional.of(testUser));
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            DamageDisputeResponseDto result = damageDisputeService.resolveDispute(1L, noRefundResolution, "admin");

            assertThat(result.refundAmount()).isEqualTo(BigDecimal.ZERO);
            verify(paymentGateway, never()).refund(anyString(), any());
        }

        @Test
        @DisplayName("Should publish DamageResolvedEvent with refund amount")
        void shouldPublishDamageResolvedEventWithRefundAmount() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(userRepository.findByUsernameAndIsDeletedFalse("admin")).thenReturn(Optional.of(testUser));
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
            when(paymentGateway.refund(anyString(), any(BigDecimal.class)))
                    .thenReturn(new PaymentResult(true, "refund_123", "Refund successful"));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            damageDisputeService.resolveDispute(1L, testResolutionDto, "admin");

            ArgumentCaptor<DamageResolvedEvent> eventCaptor = ArgumentCaptor.forClass(DamageResolvedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            DamageResolvedEvent event = eventCaptor.getValue();
            assertThat(event.getDamageReport()).isNotNull();
            assertThat(event.getRefundAmount()).isEqualTo(new BigDecimal("250.00"));
        }
    }

    @Nested
    @DisplayName("Process Refund Operations")
    class ProcessRefundOperations {

        @BeforeEach
        void setupDisputedStatus() {
            testDamageReport.setStatus(DamageStatus.DISPUTED);
        }

        @Test
        @DisplayName("Should process refund successfully")
        void shouldProcessRefundSuccessfully() {
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
            when(paymentGateway.refund("txn_123456", new BigDecimal("250.00")))
                    .thenReturn(new PaymentResult(true, "refund_123", "Refund successful"));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            damageDisputeService.processRefundForAdjustment(testDamageReport, new BigDecimal("250.00"));

            verify(paymentRepository).save(testPayment);
            assertThat(testPayment.getRefundedAmount()).isEqualTo(new BigDecimal("250.00"));
        }

        @Test
        @DisplayName("Should skip refund when no payment ID")
        void shouldSkipRefundWhenNoPaymentId() {
            testDamageReport.setPaymentId(null);

            damageDisputeService.processRefundForAdjustment(testDamageReport, new BigDecimal("250.00"));

            verify(paymentRepository, never()).findById(any());
            verify(paymentGateway, never()).refund(any(), any());
        }

        @Test
        @DisplayName("Should skip refund when adjusted amount equals original")
        void shouldSkipRefundWhenAdjustedAmountEqualsOriginal() {
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

            damageDisputeService.processRefundForAdjustment(testDamageReport, new BigDecimal("500.00"));

            verify(paymentGateway, never()).refund(any(), any());
        }

        @Test
        @DisplayName("Should throw exception when payment not found")
        void shouldThrowExceptionWhenPaymentNotFound() {
            when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> damageDisputeService.processRefundForAdjustment(testDamageReport, new BigDecimal("250.00")))
                    .isInstanceOf(DamageDisputeException.class)
                    .hasMessageContaining("Payment not found");
        }

        @Test
        @DisplayName("Should throw exception when payment cannot be refunded")
        void shouldThrowExceptionWhenPaymentCannotBeRefunded() {
            testPayment.setStatus(PaymentStatus.REFUNDED);

            when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

            assertThatThrownBy(() -> damageDisputeService.processRefundForAdjustment(testDamageReport, new BigDecimal("250.00")))
                    .isInstanceOf(DamageDisputeException.class)
                    .hasMessageContaining("cannot be refunded");
        }

        @Test
        @DisplayName("Should throw exception when refund fails")
        void shouldThrowExceptionWhenRefundFails() {
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
            when(paymentGateway.refund(anyString(), any(BigDecimal.class)))
                    .thenReturn(new PaymentResult(false, null, "Refund failed - insufficient balance"));

            assertThatThrownBy(() -> damageDisputeService.processRefundForAdjustment(testDamageReport, new BigDecimal("250.00")))
                    .isInstanceOf(DamageDisputeException.class)
                    .hasMessageContaining("Refund failed");
        }

        @Test
        @DisplayName("Should accumulate refunded amount for multiple refunds")
        void shouldAccumulateRefundedAmountForMultipleRefunds() {
            testPayment.setRefundedAmount(new BigDecimal("100.00"));

            when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
            when(paymentGateway.refund(anyString(), any(BigDecimal.class)))
                    .thenReturn(new PaymentResult(true, "refund_123", "Refund successful"));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            damageDisputeService.processRefundForAdjustment(testDamageReport, new BigDecimal("250.00"));

            assertThat(testPayment.getRefundedAmount()).isEqualTo(new BigDecimal("350.00"));
        }
    }
}
