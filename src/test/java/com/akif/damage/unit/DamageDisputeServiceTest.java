package com.akif.damage.unit;

import com.akif.auth.api.AuthService;
import com.akif.auth.api.UserDto;
import com.akif.damage.api.DamageDisputedEvent;
import com.akif.damage.api.DamageResolvedEvent;
import com.akif.damage.domain.enums.DamageStatus;
import com.akif.damage.domain.model.DamageReport;
import com.akif.damage.internal.dto.damage.request.DamageDisputeRequest;
import com.akif.damage.internal.dto.damage.request.DamageDisputeResolutionDto;
import com.akif.damage.internal.dto.damage.response.DamageDisputeResponse;
import com.akif.damage.internal.exception.DamageDisputeException;
import com.akif.damage.internal.exception.DamageReportException;
import com.akif.damage.internal.repository.DamageReportRepository;
import com.akif.damage.internal.service.damage.impl.DamageDisputeServiceImpl;
import com.akif.payment.api.PaymentResult;
import com.akif.payment.api.PaymentService;
import com.akif.rental.api.RentalService;
import com.akif.rental.api.RentalSummaryDto;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import com.akif.shared.enums.Role;

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
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuthService authService;

    @Mock
    private RentalService rentalService;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private DamageDisputeServiceImpl damageDisputeService;

    private UserDto testUser;
    private UserDto testAdmin;
    private RentalSummaryDto testRental;
    private DamageReport testDamageReport;
    private DamageDisputeRequest testDisputeRequest;
    private DamageDisputeResolutionDto testResolutionDto;

    @BeforeEach
    void setUp() {
        testUser = new UserDto(
                1L,
                "customer",
                "customer@example.com",
                "Customer",
                "User",
                Set.of(Role.USER),
                true
        );

        testAdmin = new UserDto(
                100L,
                "admin",
                "admin@example.com",
                "Admin",
                "User",
                Set.of(Role.ADMIN),
                true
        );

        testRental = new RentalSummaryDto(
                1L,
                1L,
                1L,
                "Toyota",
                "Corolla",
                "34ABC123",
                "customer@example.com",
                "Customer User",
                LocalDate.now().minusDays(5),
                LocalDate.now().plusDays(2),
                true,
                1
        );

        testDamageReport = DamageReport.builder()
                .id(1L)
                .rentalId(testRental.id())
                .carId(1L)
                .carBrand("Toyota")
                .carModel("Corolla")
                .carLicensePlate("34ABC123")
                .customerEmail("customer@example.com")
                .customerFullName("Customer User")
                .customerUserId(testUser.id())
                .description("Scratch on front bumper")
                .status(DamageStatus.CHARGED)
                .customerLiability(new BigDecimal("500.00"))
                .repairCostEstimate(new BigDecimal("500.00"))
                .paymentId(1L)
                .transactionId("txn_123456")
                .reportedAt(LocalDateTime.now())
                .build();

        testDisputeRequest = new DamageDisputeRequest(
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
            when(authService.getUserByUsername("customer")).thenReturn(testUser);
            when(rentalService.getRentalSummaryById(testDamageReport.getRentalId())).thenReturn(testRental);
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            DamageDisputeResponse result = damageDisputeService.createDispute(1L, testDisputeRequest, "customer");

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
            when(authService.getUserByUsername("customer")).thenReturn(testUser);
            when(rentalService.getRentalSummaryById(testDamageReport.getRentalId())).thenReturn(testRental);
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
            when(authService.getUserByUsername("customer")).thenReturn(testUser);

            assertThatThrownBy(() -> damageDisputeService.createDispute(1L, testDisputeRequest, "customer"))
                    .isInstanceOf(DamageDisputeException.class);
        }

        @Test
        @DisplayName("Should throw exception when user is not the rental owner")
        void shouldThrowExceptionWhenUserIsNotTheRentalOwner() {
            UserDto differentUser = new UserDto(999L, "other", "other@email.com", "Other", "User", Set.of(Role.USER), true);
            
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(authService.getUserByUsername("other")).thenReturn(differentUser);
            when(rentalService.getRentalSummaryById(testDamageReport.getRentalId())).thenReturn(testRental);

            assertThatThrownBy(() -> damageDisputeService.createDispute(1L, testDisputeRequest, "other"))
                    .isInstanceOf(DamageDisputeException.class);
        }

        @Test
        @DisplayName("Should publish DamageDisputedEvent when dispute created")
        void shouldPublishDamageDisputedEventWhenDisputeCreated() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(authService.getUserByUsername("customer")).thenReturn(testUser);
            when(rentalService.getRentalSummaryById(testDamageReport.getRentalId())).thenReturn(testRental);
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            damageDisputeService.createDispute(1L, testDisputeRequest, "customer");

            ArgumentCaptor<DamageDisputedEvent> eventCaptor = ArgumentCaptor.forClass(DamageDisputedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            DamageDisputedEvent event = eventCaptor.getValue();
            assertThat(event.getDamageReportId()).isNotNull();
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
            when(authService.getUserByUsername("admin")).thenReturn(testAdmin);
            when(paymentService.refund(anyString(), any(BigDecimal.class)))
                    .thenReturn(new PaymentResult(true, "refund_123", "Refund successful"));
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            DamageDisputeResponse result = damageDisputeService.resolveDispute(1L, testResolutionDto, "admin");

            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(DamageStatus.RESOLVED);
            assertThat(result.refundAmount()).isEqualTo(new BigDecimal("250.00"));

            verify(paymentService).refund("txn_123456", new BigDecimal("250.00"));
            verify(eventPublisher).publishEvent(any(DamageResolvedEvent.class));
        }

        @Test
        @DisplayName("Should update damage report with resolution details")
        void shouldUpdateDamageReportWithResolutionDetails() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(authService.getUserByUsername("admin")).thenReturn(testAdmin);
            when(paymentService.refund(anyString(), any(BigDecimal.class)))
                    .thenReturn(new PaymentResult(true, "refund_123", "Refund successful"));
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            damageDisputeService.resolveDispute(1L, testResolutionDto, "admin");

            ArgumentCaptor<DamageReport> captor = ArgumentCaptor.forClass(DamageReport.class);
            verify(damageReportRepository).save(captor.capture());

            DamageReport savedReport = captor.getValue();
            assertThat(savedReport.getCustomerLiability()).isEqualTo(new BigDecimal("250.00"));
            assertThat(savedReport.getRepairCostEstimate()).isEqualTo(new BigDecimal("250.00"));
            assertThat(savedReport.getResolutionNotes()).contains("Partial refund approved");
            assertThat(savedReport.getResolvedBy()).isEqualTo(100L);
            assertThat(savedReport.getResolvedAt()).isNotNull();
            assertThat(savedReport.getStatus()).isEqualTo(DamageStatus.RESOLVED);
        }

        @Test
        @DisplayName("Should throw exception when damage is not in DISPUTED status")
        void shouldThrowExceptionWhenDamageIsNotInDisputedStatus() {
            testDamageReport.setStatus(DamageStatus.CHARGED);

            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(authService.getUserByUsername("admin")).thenReturn(testAdmin);

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
            when(authService.getUserByUsername("admin")).thenReturn(testAdmin);
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            DamageDisputeResponse result = damageDisputeService.resolveDispute(1L, noRefundResolution, "admin");

            assertThat(result.refundAmount()).isEqualTo(BigDecimal.ZERO);
            verify(paymentService, never()).refund(anyString(), any());
        }

        @Test
        @DisplayName("Should publish DamageResolvedEvent with refund amount")
        void shouldPublishDamageResolvedEventWithRefundAmount() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(authService.getUserByUsername("admin")).thenReturn(testAdmin);
            when(paymentService.refund(anyString(), any(BigDecimal.class)))
                    .thenReturn(new PaymentResult(true, "refund_123", "Refund successful"));
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            damageDisputeService.resolveDispute(1L, testResolutionDto, "admin");

            ArgumentCaptor<DamageResolvedEvent> eventCaptor = ArgumentCaptor.forClass(DamageResolvedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            DamageResolvedEvent event = eventCaptor.getValue();
            assertThat(event.getDamageReportId()).isNotNull();
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
            when(paymentService.refund("txn_123456", new BigDecimal("250.00")))
                    .thenReturn(new PaymentResult(true, "refund_123", "Refund successful"));

            damageDisputeService.processRefundForAdjustment(testDamageReport, new BigDecimal("250.00"));

            verify(paymentService).refund("txn_123456", new BigDecimal("250.00"));
        }

        @Test
        @DisplayName("Should skip refund when no transaction ID")
        void shouldSkipRefundWhenNoTransactionId() {
            testDamageReport.setTransactionId(null);

            damageDisputeService.processRefundForAdjustment(testDamageReport, new BigDecimal("250.00"));

            verify(paymentService, never()).refund(any(), any());
        }

        @Test
        @DisplayName("Should skip refund when adjusted amount equals original")
        void shouldSkipRefundWhenAdjustedAmountEqualsOriginal() {
            damageDisputeService.processRefundForAdjustment(testDamageReport, new BigDecimal("500.00"));

            verify(paymentService, never()).refund(any(), any());
        }
    }
}
