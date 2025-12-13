package com.akif.damage.unit;

import com.akif.auth.api.AuthService;
import com.akif.auth.api.UserDto;
import com.akif.car.api.CarResponse;
import com.akif.car.api.CarService;
import com.akif.car.domain.enums.CarStatusType;
import com.akif.damage.api.DamageAssessedEvent;
import com.akif.damage.domain.enums.DamageCategory;
import com.akif.damage.domain.enums.DamageSeverity;
import com.akif.damage.domain.enums.DamageStatus;
import com.akif.damage.domain.model.DamageReport;
import com.akif.damage.internal.config.DamageConfig;
import com.akif.damage.internal.dto.damage.request.DamageAssessmentRequest;
import com.akif.damage.internal.dto.damage.response.DamageAssessmentResponse;
import com.akif.damage.internal.exception.DamageAssessmentException;
import com.akif.damage.internal.exception.DamageReportException;
import com.akif.damage.internal.repository.DamageReportRepository;
import com.akif.damage.internal.service.damage.impl.DamageAssessmentServiceImpl;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DamageAssessmentService Unit Tests")
class DamageAssessmentServiceTest {

    @Mock
    private DamageReportRepository damageReportRepository;

    @Mock
    private CarService carService;

    @Mock
    private AuthService authService;

    @Mock
    private DamageConfig damageConfig;

    @Mock
    private ApplicationEventPublisher eventPublisher;

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
                Set.of(Role.ADMIN),
                true
        );

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
                .carId(testCar.getId())
                .carBrand(testCar.getBrand())
                .carModel(testCar.getModel())
                .carLicensePlate(testCar.getLicensePlate())
                .customerEmail(testUser.email())
                .customerFullName(testUser.firstName() + " " + testUser.lastName())
                .customerUserId(testUser.id())
                .description("Scratch on front bumper")
                .status(DamageStatus.REPORTED)
                .reportedBy(1L)
                .reportedAt(LocalDateTime.now())
                .build();

        testAssessmentRequest = new DamageAssessmentRequest(
                DamageSeverity.MINOR,
                DamageCategory.SCRATCH,
                new BigDecimal("500.00"),
                true,
                new BigDecimal("100.00"),
                "Minor scratch requiring paint touch-up"
        );
    }

    @Nested
    @DisplayName("Assess Damage Operations")
    class AssessDamageOperations {

        @Test
        @DisplayName("Should assess damage successfully")
        void shouldAssessDamageSuccessfully() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(authService.getUserByUsername("admin")).thenReturn(testUser);
            when(damageConfig.getDefaultInsuranceDeductible()).thenReturn(new BigDecimal("100.00"));
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            DamageAssessmentResponse result = damageAssessmentService.assessDamage(1L, testAssessmentRequest, "admin");

            assertThat(result).isNotNull();
            assertThat(result.damageId()).isEqualTo(1L);
            verify(damageReportRepository).save(any(DamageReport.class));
            verify(eventPublisher).publishEvent(any(DamageAssessedEvent.class));
        }

        @Test
        @DisplayName("Should set customer liability correctly with insurance")
        void shouldSetCustomerLiabilityCorrectlyWithInsurance() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(authService.getUserByUsername("admin")).thenReturn(testUser);
            when(damageConfig.getDefaultInsuranceDeductible()).thenReturn(new BigDecimal("100.00"));
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            damageAssessmentService.assessDamage(1L, testAssessmentRequest, "admin");

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
                    "No insurance"
            );

            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(authService.getUserByUsername("admin")).thenReturn(testUser);
            when(damageConfig.getDefaultInsuranceDeductible()).thenReturn(new BigDecimal("100.00"));
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            damageAssessmentService.assessDamage(1L, requestWithoutInsurance, "admin");

            ArgumentCaptor<DamageReport> captor = ArgumentCaptor.forClass(DamageReport.class);
            verify(damageReportRepository).save(captor.capture());

            DamageReport savedReport = captor.getValue();
            assertThat(savedReport.getCustomerLiability()).isEqualTo(new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("Should throw exception when damage report not found")
        void shouldThrowExceptionWhenDamageReportNotFound() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> damageAssessmentService.assessDamage(999L, testAssessmentRequest, "admin"))
                    .isInstanceOf(DamageReportException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should throw exception when damage cannot be assessed")
        void shouldThrowExceptionWhenDamageCannotBeAssessed() {
            testDamageReport.setStatus(DamageStatus.CHARGED);

            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(authService.getUserByUsername("admin")).thenReturn(testUser);

            assertThatThrownBy(() -> damageAssessmentService.assessDamage(1L, testAssessmentRequest, "admin"))
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
                    "Major mechanical damage"
            );

            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(authService.getUserByUsername("admin")).thenReturn(testUser);
            when(damageConfig.getDefaultInsuranceDeductible()).thenReturn(new BigDecimal("500.00"));
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            DamageAssessmentResponse result = damageAssessmentService.assessDamage(1L, majorDamageRequest, "admin");

            verify(carService).markAsMaintenance(testCar.getId());
            assertThat(result.carStatusUpdated()).isEqualTo("MAINTENANCE");
        }

        @Test
        @DisplayName("Should publish DamageAssessedEvent when assessment completed")
        void shouldPublishDamageAssessedEventWhenAssessmentCompleted() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(authService.getUserByUsername("admin")).thenReturn(testUser);
            when(damageConfig.getDefaultInsuranceDeductible()).thenReturn(new BigDecimal("100.00"));
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            damageAssessmentService.assessDamage(1L, testAssessmentRequest, "admin");

            ArgumentCaptor<DamageAssessedEvent> eventCaptor = ArgumentCaptor.forClass(DamageAssessedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            DamageAssessedEvent event = eventCaptor.getValue();
            assertThat(event.getDamageReportId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Update Assessment Operations")
    class UpdateAssessmentOperations {

        @Test
        @DisplayName("Should update assessment successfully when status is UNDER_ASSESSMENT")
        void shouldUpdateAssessmentSuccessfullyWhenStatusIsUnderAssessment() {
            testDamageReport.setStatus(DamageStatus.UNDER_ASSESSMENT);

            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(authService.getUserByUsername("admin")).thenReturn(testUser);
            when(damageConfig.getDefaultInsuranceDeductible()).thenReturn(new BigDecimal("100.00"));
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);

            DamageAssessmentResponse result = damageAssessmentService.assessDamage(1L, testAssessmentRequest, "admin");

            assertThat(result).isNotNull();
            verify(damageReportRepository).save(any(DamageReport.class));
        }

        @Test
        @DisplayName("Should throw exception when updating already charged damage")
        void shouldThrowExceptionWhenUpdatingAlreadyChargedDamage() {
            testDamageReport.setStatus(DamageStatus.CHARGED);

            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));

            assertThatThrownBy(() -> damageAssessmentService.updateAssessment(1L, testAssessmentRequest, "admin"))
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
            BigDecimal result = damageAssessmentService.calculateCustomerLiability(BigDecimal.ZERO, true, new BigDecimal("100"));

            assertThat(result).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should return deductible when repair cost exceeds deductible with insurance")
        void shouldReturnDeductibleWhenRepairCostExceedsDeductibleWithInsurance() {
            BigDecimal result = damageAssessmentService.calculateCustomerLiability(
                    new BigDecimal("1000"),
                    true,
                    new BigDecimal("250")
            );

            assertThat(result).isEqualTo(new BigDecimal("250"));
        }

        @Test
        @DisplayName("Should return repair cost when less than deductible with insurance")
        void shouldReturnRepairCostWhenLessThanDeductibleWithInsurance() {
            BigDecimal result = damageAssessmentService.calculateCustomerLiability(
                    new BigDecimal("100"),
                    true,
                    new BigDecimal("250")
            );

            assertThat(result).isEqualTo(new BigDecimal("100"));
        }

        @Test
        @DisplayName("Should return full repair cost without insurance")
        void shouldReturnFullRepairCostWithoutInsurance() {
            BigDecimal result = damageAssessmentService.calculateCustomerLiability(
                    new BigDecimal("1000"),
                    false,
                    null
            );

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
