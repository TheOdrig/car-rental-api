package com.akif.damage.unit;

import com.akif.auth.api.UserLookup;
import org.springframework.test.util.ReflectionTestUtils;
import com.akif.auth.api.UserDto;
import com.akif.car.api.CarResponse;
import com.akif.car.api.CarFleetOperations;
import com.akif.car.api.CarStatusType;
import com.akif.damage.api.DamageAssessedEvent;
import com.akif.damage.api.DamageCategory;
import com.akif.damage.api.DamageSeverity;
import com.akif.damage.api.DamageStatus;
import com.akif.damage.domain.model.DamageReport;
import com.akif.damage.internal.config.DamageConfig;
import com.akif.damage.internal.dto.damage.request.DamageAssessmentRequest;
import com.akif.damage.internal.dto.damage.response.DamageAssessmentResponse;
import com.akif.damage.internal.exception.DamageAssessmentException;
import com.akif.damage.internal.exception.DamageReportException;
import com.akif.damage.internal.repository.DamageReportRepository;
import com.akif.damage.internal.service.damage.impl.DamageAssessmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import java.time.Clock;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import com.akif.shared.enums.Role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DamageAssessmentService Unit Tests")
class DamageAssessmentServiceTest {

    @Mock
    private DamageReportRepository damageReportRepository;

    @Mock
    private CarFleetOperations carFleetOperations;

    @Mock
    private UserLookup userLookup;

    @Mock
    private DamageConfig damageConfig;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private com.akif.rental.api.RentalService rentalService;

    @Spy
    private Clock clock = Clock.systemDefaultZone();

    @InjectMocks
    private DamageAssessmentServiceImpl damageAssessmentService;

    private UserDto testUser;
    private CarResponse testCar;
    private DamageReport testDamageReport;
    private DamageAssessmentRequest testAssessmentRequest;

    @BeforeEach
    void setUp() {
        testUser = new UserDto(
                1L,
                "admin",
                "admin@example.com",
                "Admin",
                "User",
                null,
                null,
                Set.of(Role.ADMIN),
                true);

        testCar = CarResponse.builder()
                .id(1L)
                .licensePlate("34ABC123")
                .brand("Toyota")
                .model("Corolla")
                .carStatusType(CarStatusType.AVAILABLE)
                .build();

        testDamageReport = DamageReport.builder()
                .id(1L)
                .rentalId(1L)
                .carId(testCar.id())
                .carBrand(testCar.brand())
                .carModel(testCar.model())
                .carLicensePlate(testCar.licensePlate())
                .customerEmail(testUser.email())
                .customerFullName(testUser.firstName() + " " + testUser.lastName())
                .customerUserId(testUser.id())
                .description("Scratch on front bumper")
                .status(DamageStatus.REPORTED)
                .reportedBy(1L)
                .reportedAt(Instant.now())
                .build();

        when(rentalService.getRentalSummaryById(any())).thenReturn(new com.akif.rental.api.RentalSummaryDto(
                1L, 1L, 1L, "Toyota", "Corolla", "34ABC123", "c@example.com", "C U",
                java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(3), false, 0,
                com.akif.shared.enums.CurrencyType.TRY, com.akif.rental.api.RentalStatus.RETURNED, java.time.Instant.now()));

        testAssessmentRequest = new DamageAssessmentRequest(
                DamageSeverity.MINOR,
                DamageCategory.SCRATCH,
                new BigDecimal("500.00"),
                true,
                new BigDecimal("100.00"),
                "Minor scratch requiring paint touch-up");
    }

    @Nested
    @DisplayName("Assess Damage Operations")
    class AssessDamageOperations {

        @Test
        @DisplayName("Should assess damage successfully")
        void shouldAssessDamageSuccessfully() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(userLookup.getUserById(2L)).thenReturn(testUser);
            when(damageConfig.getDefaultInsuranceDeductible()).thenReturn(new BigDecimal("100.00"));
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            DamageAssessmentResponse result = damageAssessmentService.assessDamage(1L, testAssessmentRequest, 2L);

            assertThat(result).isNotNull();
            assertThat(result.damageId()).isEqualTo(1L);
            verify(damageReportRepository).save(any(DamageReport.class));
            verify(eventPublisher).publishEvent(any(DamageAssessedEvent.class));
        }

        @Test
        @DisplayName("Should default the damage currency to the rental currency at assessment time")
        void shouldDefaultCurrencyToRentalCurrency() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(userLookup.getUserById(2L)).thenReturn(testUser);
            when(damageConfig.getDefaultInsuranceDeductible()).thenReturn(new BigDecimal("100.00"));
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            damageAssessmentService.assessDamage(1L, testAssessmentRequest, 2L);

            assertThat(testDamageReport.getCurrency()).isEqualTo(com.akif.shared.enums.CurrencyType.TRY);
        }

        @Test
        @DisplayName("Should round repair cost, deductible and liability to whole yen for JPY")
        void shouldRoundLiabilityToWholeYen() {
            when(rentalService.getRentalSummaryById(any())).thenReturn(new com.akif.rental.api.RentalSummaryDto(
                    1L, 1L, 1L, "Toyota", "Corolla", "34ABC123", "c@example.com", "C U",
                    java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(3), false, 0,
                    com.akif.shared.enums.CurrencyType.JPY, com.akif.rental.api.RentalStatus.RETURNED, java.time.Instant.now()));
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(userLookup.getUserById(2L)).thenReturn(testUser);
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);
            DamageAssessmentRequest request = new DamageAssessmentRequest(DamageSeverity.MINOR, DamageCategory.SCRATCH,
                    new BigDecimal("12345.67"), false, null, "n", com.akif.shared.enums.CurrencyType.JPY);
            when(damageConfig.getDefaultInsuranceDeductible()).thenReturn(new BigDecimal("100.00"));

            damageAssessmentService.assessDamage(1L, request, 2L);

            assertThat(testDamageReport.getCustomerLiability()).isEqualTo(new BigDecimal("12346"));
            assertThat(testDamageReport.getRepairCostEstimate()).isEqualTo(new BigDecimal("12346"));
            assertThat(testDamageReport.getInsuranceDeductible().scale()).isZero();
        }

        @Test
        @DisplayName("Should keep an explicit assessment currency")
        void shouldKeepExplicitCurrency() {
            when(rentalService.getRentalSummaryById(any())).thenReturn(new com.akif.rental.api.RentalSummaryDto(
                    1L, 1L, 1L, "Toyota", "Corolla", "34ABC123", "c@example.com", "C U",
                    java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(3), false, 0,
                    com.akif.shared.enums.CurrencyType.EUR, com.akif.rental.api.RentalStatus.RETURNED, java.time.Instant.now()));
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(userLookup.getUserById(2L)).thenReturn(testUser);
            when(damageConfig.getDefaultInsuranceDeductible()).thenReturn(new BigDecimal("100.00"));
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);
            DamageAssessmentRequest request = new DamageAssessmentRequest(DamageSeverity.MINOR, DamageCategory.SCRATCH,
                    new BigDecimal("500.00"), true, new BigDecimal("100.00"), "n", com.akif.shared.enums.CurrencyType.EUR);

            damageAssessmentService.assessDamage(1L, request, 2L);

            assertThat(testDamageReport.getCurrency()).isEqualTo(com.akif.shared.enums.CurrencyType.EUR);
        }

        @Test
        @DisplayName("Should reject an assessment currency that differs from the rental currency")
        void shouldRejectCurrencyDifferentFromRental() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(userLookup.getUserById(2L)).thenReturn(testUser);
            DamageAssessmentRequest request = new DamageAssessmentRequest(DamageSeverity.MINOR, DamageCategory.SCRATCH,
                    new BigDecimal("500.00"), true, new BigDecimal("100.00"), "n", com.akif.shared.enums.CurrencyType.EUR);

            org.assertj.core.api.Assertions.assertThatThrownBy(
                            () -> damageAssessmentService.assessDamage(1L, request, 2L))
                    .isInstanceOf(com.akif.damage.internal.exception.DamageAssessmentException.class)
                    .hasMessageContaining("TRY");
        }

        @Test
        @DisplayName("Should set customer liability correctly with insurance")
        void shouldSetCustomerLiabilityCorrectlyWithInsurance() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(userLookup.getUserById(2L)).thenReturn(testUser);
            when(damageConfig.getDefaultInsuranceDeductible()).thenReturn(new BigDecimal("100.00"));
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            damageAssessmentService.assessDamage(1L, testAssessmentRequest, 2L);

            ArgumentCaptor<DamageReport> captor = ArgumentCaptor.forClass(DamageReport.class);
            verify(damageReportRepository).save(captor.capture());

            DamageReport savedReport = captor.getValue();
            assertThat(savedReport.getCustomerLiability()).isEqualTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("Should set full repair cost as liability without insurance")
        void shouldSetFullRepairCostAsLiabilityWithoutInsurance() {
            DamageAssessmentRequest requestWithoutInsurance = new DamageAssessmentRequest(
                    DamageSeverity.MINOR,
                    DamageCategory.SCRATCH,
                    new BigDecimal("500.00"),
                    false,
                    null,
                    "No insurance");

            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(userLookup.getUserById(2L)).thenReturn(testUser);
            when(damageConfig.getDefaultInsuranceDeductible()).thenReturn(new BigDecimal("100.00"));
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            damageAssessmentService.assessDamage(1L, requestWithoutInsurance, 2L);

            ArgumentCaptor<DamageReport> captor = ArgumentCaptor.forClass(DamageReport.class);
            verify(damageReportRepository).save(captor.capture());

            DamageReport savedReport = captor.getValue();
            assertThat(savedReport.getCustomerLiability()).isEqualTo(new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("Should throw exception when damage report not found")
        void shouldThrowExceptionWhenDamageReportNotFound() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> damageAssessmentService.assessDamage(999L, testAssessmentRequest, 2L))
                    .isInstanceOf(DamageReportException.class);

            verify(eventPublisher, never()).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("Should throw exception when damage cannot be assessed")
        void shouldThrowExceptionWhenDamageCannotBeAssessed() {
            ReflectionTestUtils.setField(testDamageReport, "status", DamageStatus.CHARGED);

            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(userLookup.getUserById(2L)).thenReturn(testUser);

            assertThatThrownBy(() -> damageAssessmentService.assessDamage(1L, testAssessmentRequest, 2L))
                    .isInstanceOf(DamageAssessmentException.class);
        }

        @Test
        @DisplayName("Should update car status to MAINTENANCE for major damage")
        void shouldUpdateCarStatusToMaintenanceForMajorDamage() {
            DamageAssessmentRequest majorDamageRequest = new DamageAssessmentRequest(
                    DamageSeverity.MAJOR,
                    DamageCategory.MECHANICAL_DAMAGE,
                    new BigDecimal("5000.00"),
                    true,
                    new BigDecimal("500.00"),
                    "Major mechanical damage");

            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(userLookup.getUserById(2L)).thenReturn(testUser);
            when(damageConfig.getDefaultInsuranceDeductible()).thenReturn(new BigDecimal("500.00"));
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            DamageAssessmentResponse result = damageAssessmentService.assessDamage(1L, majorDamageRequest, 2L);

            verify(carFleetOperations).markAsMaintenance(testCar.id());
            assertThat(result.carStatusUpdated()).isEqualTo("MAINTENANCE");
        }

        @Test
        @DisplayName("Should publish DamageAssessedEvent when assessment completed")
        void shouldPublishDamageAssessedEventWhenAssessmentCompleted() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(userLookup.getUserById(2L)).thenReturn(testUser);
            when(damageConfig.getDefaultInsuranceDeductible()).thenReturn(new BigDecimal("100.00"));
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            damageAssessmentService.assessDamage(1L, testAssessmentRequest, 2L);

            ArgumentCaptor<DamageAssessedEvent> eventCaptor = ArgumentCaptor.forClass(DamageAssessedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            DamageAssessedEvent event = eventCaptor.getValue();
            assertThat(event.damageReportId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Update Assessment Operations")
    class UpdateAssessmentOperations {

        @Test
        @DisplayName("Should update assessment successfully when status is UNDER_ASSESSMENT")
        void shouldUpdateAssessmentSuccessfullyWhenStatusIsUnderAssessment() {
            ReflectionTestUtils.setField(testDamageReport, "status", DamageStatus.UNDER_ASSESSMENT);

            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(userLookup.getUserById(2L)).thenReturn(testUser);
            when(damageConfig.getDefaultInsuranceDeductible()).thenReturn(new BigDecimal("100.00"));
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            DamageAssessmentResponse result = damageAssessmentService.assessDamage(1L, testAssessmentRequest, 2L);

            assertThat(result).isNotNull();
            verify(damageReportRepository).save(any(DamageReport.class));
        }

        @Test
        @DisplayName("Should throw exception when updating already charged damage")
        void shouldThrowExceptionWhenUpdatingAlreadyChargedDamage() {
            ReflectionTestUtils.setField(testDamageReport, "status", DamageStatus.CHARGED);

            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));

            assertThatThrownBy(() -> damageAssessmentService.updateAssessment(1L, testAssessmentRequest, 2L))
                    .isInstanceOf(DamageAssessmentException.class);
        }
    }

    @Nested
    @DisplayName("Calculate Customer Liability Operations")
    class CalculateCustomerLiabilityOperations {

        @Test
        @DisplayName("Should return zero for null repair cost")
        void shouldReturnZeroForNullRepairCost() {
            BigDecimal result = damageAssessmentService.calculateCustomerLiability(null, true, new BigDecimal("100"));

            assertThat(result).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should return zero for zero repair cost")
        void shouldReturnZeroForZeroRepairCost() {
            BigDecimal result = damageAssessmentService.calculateCustomerLiability(BigDecimal.ZERO, true,
                    new BigDecimal("100"));

            assertThat(result).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should return deductible when repair cost exceeds deductible with insurance")
        void shouldReturnDeductibleWhenRepairCostExceedsDeductibleWithInsurance() {
            BigDecimal result = damageAssessmentService.calculateCustomerLiability(
                    new BigDecimal("1000"),
                    true,
                    new BigDecimal("250"));

            assertThat(result).isEqualTo(new BigDecimal("250"));
        }

        @Test
        @DisplayName("Should return repair cost when less than deductible with insurance")
        void shouldReturnRepairCostWhenLessThanDeductibleWithInsurance() {
            BigDecimal result = damageAssessmentService.calculateCustomerLiability(
                    new BigDecimal("100"),
                    true,
                    new BigDecimal("250"));

            assertThat(result).isEqualTo(new BigDecimal("100"));
        }

        @Test
        @DisplayName("Should return full repair cost without insurance")
        void shouldReturnFullRepairCostWithoutInsurance() {
            BigDecimal result = damageAssessmentService.calculateCustomerLiability(
                    new BigDecimal("1000"),
                    false,
                    null);

            assertThat(result).isEqualTo(new BigDecimal("1000"));
        }
    }

    @Nested
    @DisplayName("Determine Severity Operations")
    class DetermineSeverityOperations {

        @BeforeEach
        void setUpThresholds() {
            when(damageConfig.getMinorThreshold()).thenReturn(new BigDecimal("500"));
            when(damageConfig.getModerateThreshold()).thenReturn(new BigDecimal("2000"));
            when(damageConfig.getMajorThreshold()).thenReturn(new BigDecimal("10000"));
        }

        @Test
        @DisplayName("Should return MINOR for null repair cost")
        void shouldReturnMinorForNullRepairCost() {
            BigDecimal result = null;
            DamageSeverity severity = damageAssessmentService.determineSeverity(result);

            assertThat(severity).isEqualTo(DamageSeverity.MINOR);
        }

        @Test
        @DisplayName("Should return MINOR for repair cost below minor threshold")
        void shouldReturnMinorForRepairCostBelowMinorThreshold() {
            DamageSeverity severity = damageAssessmentService.determineSeverity(new BigDecimal("300"));

            assertThat(severity).isEqualTo(DamageSeverity.MINOR);
        }

        @Test
        @DisplayName("Should return MODERATE for repair cost between minor and moderate thresholds")
        void shouldReturnModerateForRepairCostBetweenMinorAndModerateThresholds() {
            DamageSeverity severity = damageAssessmentService.determineSeverity(new BigDecimal("1000"));

            assertThat(severity).isEqualTo(DamageSeverity.MODERATE);
        }

        @Test
        @DisplayName("Should return MAJOR for repair cost between moderate and major thresholds")
        void shouldReturnMajorForRepairCostBetweenModerateAndMajorThresholds() {
            DamageSeverity severity = damageAssessmentService.determineSeverity(new BigDecimal("5000"));

            assertThat(severity).isEqualTo(DamageSeverity.MAJOR);
        }

        @Test
        @DisplayName("Should return TOTAL_LOSS for repair cost above major threshold")
        void shouldReturnTotalLossForRepairCostAboveMajorThreshold() {
            DamageSeverity severity = damageAssessmentService.determineSeverity(new BigDecimal("15000"));

            assertThat(severity).isEqualTo(DamageSeverity.TOTAL_LOSS);
        }
    }
}
