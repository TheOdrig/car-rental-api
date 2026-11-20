package com.akif.rental.unit.detection;

import com.akif.shared.time.UtcDates;
import java.time.ZoneOffset;
import java.time.Duration;
import com.akif.auth.domain.User;
import org.springframework.test.util.ReflectionTestUtils;
import com.akif.car.domain.Car;
import com.akif.rental.domain.enums.LateReturnStatus;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.config.PenaltyConfig;
import com.akif.rental.internal.dto.penalty.PenaltyResult;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.rental.internal.service.detection.impl.LateReturnDetectionServiceImpl;
import com.akif.rental.internal.service.penalty.PenaltyCalculationService;
import com.akif.shared.enums.CurrencyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.time.Clock;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LateReturnDetectionServiceImpl Unit Tests")
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class LateReturnDetectionServiceImplTest {

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private PenaltyConfig penaltyConfig;

    @Mock
    private PenaltyCalculationService penaltyCalculationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-11-20T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private LateReturnDetectionServiceImpl service;

    private Rental testRental;
    private Car testCar;
    private User testUser;

    private void overdue(Rental... rentals) {
        when(rentalRepository.findOverdueRentalIds(any(LocalDate.class)))
                .thenReturn(Arrays.stream(rentals).map(Rental::getId).toList());
        for (Rental rental : rentals) {
            when(rentalRepository.findByIdForUpdate(rental.getId())).thenReturn(java.util.Optional.of(rental));
        }
    }

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        when(penaltyConfig.getGracePeriodMinutes()).thenReturn(60);
        when(penaltyConfig.getSeverelyLateThresholdHours()).thenReturn(24);

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
                .userId(testUser.getId())
                .userEmail(testUser.getEmail())
                .userFullName("Test User")
                .carId(testCar.getId())
                .carBrand(testCar.getBrand())
                .carModel(testCar.getModel())
                .carLicensePlate(testCar.getLicensePlate())
                .startDate(LocalDate.now(clock).minusDays(5))
                .endDate(LocalDate.now(clock).minusDays(2))
                .dailyPrice(new BigDecimal("500.00"))
                .currency(CurrencyType.TRY)
                .status(RentalStatus.IN_USE)
                .lateReturnStatus(LateReturnStatus.ON_TIME)
                .build();
    }

    @Nested
    @DisplayName("On-Time Rental (No Late Status)")
    class OnTimeRental {

        @Test
        @DisplayName("Should return ON_TIME when rental is not yet due")
        void shouldReturnOnTimeWhenRentalIsNotYetDue() {
            Instant currentTime = testRental.getEndDate().atTime(12, 0, 0).toInstant(ZoneOffset.UTC);

            LateReturnStatus status = service.calculateLateStatus(testRental, currentTime);

            assertThat(status).isEqualTo(LateReturnStatus.ON_TIME);
        }

        @Test
        @DisplayName("Should return ON_TIME when returned exactly at end time")
        void shouldReturnOnTimeWhenReturnedExactlyAtEndTime() {
            Instant currentTime = UtcDates.endOfDayWholeSecond(testRental.getEndDate());

            LateReturnStatus status = service.calculateLateStatus(testRental, currentTime);

            assertThat(status).isEqualTo(LateReturnStatus.ON_TIME);
        }

        @Test
        @DisplayName("Should calculate zero late hours for on-time rental")
        void shouldCalculateZeroLateHoursForOnTimeRental() {
            Instant currentTime = testRental.getEndDate().atTime(12, 0, 0).toInstant(ZoneOffset.UTC);

            long lateHours = service.calculateLateHours(testRental, currentTime);

            assertThat(lateHours).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Grace Period Detection")
    class GracePeriodDetection {

        @Test
        @DisplayName("Should return GRACE_PERIOD when within grace period")
        void shouldReturnGracePeriodWhenWithinGracePeriod() {
            Instant currentTime = UtcDates.endOfDayWholeSecond(testRental.getEndDate()).plus(Duration.ofMinutes(30));

            LateReturnStatus status = service.calculateLateStatus(testRental, currentTime);

            assertThat(status).isEqualTo(LateReturnStatus.GRACE_PERIOD);
        }

        @Test
        @DisplayName("Should return GRACE_PERIOD at exactly grace period limit")
        void shouldReturnGracePeriodAtExactlyGracePeriodLimit() {
            Instant currentTime = UtcDates.endOfDayWholeSecond(testRental.getEndDate()).plus(Duration.ofMinutes(60));

            LateReturnStatus status = service.calculateLateStatus(testRental, currentTime);

            assertThat(status).isEqualTo(LateReturnStatus.GRACE_PERIOD);
        }

        @Test
        @DisplayName("Should calculate zero late hours during grace period")
        void shouldCalculateZeroLateHoursDuringGracePeriod() {
            Instant currentTime = UtcDates.endOfDayWholeSecond(testRental.getEndDate()).plus(Duration.ofMinutes(45));

            long lateHours = service.calculateLateHours(testRental, currentTime);

            assertThat(lateHours).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Late Detection (1-24 hours)")
    class LateDetection {

        @Test
        @DisplayName("Should return LATE when 1 minute after grace period")
        void shouldReturnLateWhen1MinuteAfterGracePeriod() {
            Instant currentTime = UtcDates.endOfDayWholeSecond(testRental.getEndDate()).plus(Duration.ofMinutes(62));

            LateReturnStatus status = service.calculateLateStatus(testRental, currentTime);

            assertThat(status).isEqualTo(LateReturnStatus.LATE);
        }

        @Test
        @DisplayName("Should return LATE when 5 hours late")
        void shouldReturnLateWhen5HoursLate() {
            Instant currentTime = UtcDates.endOfDayWholeSecond(testRental.getEndDate()).plus(Duration.ofHours(6));

            LateReturnStatus status = service.calculateLateStatus(testRental, currentTime);

            assertThat(status).isEqualTo(LateReturnStatus.LATE);
        }

        @Test
        @DisplayName("Should return LATE when 23 hours late")
        void shouldReturnLateWhen23HoursLate() {
            Instant currentTime = UtcDates.endOfDayWholeSecond(testRental.getEndDate()).plus(Duration.ofHours(24));

            LateReturnStatus status = service.calculateLateStatus(testRental, currentTime);

            assertThat(status).isEqualTo(LateReturnStatus.LATE);
        }

        @Test
        @DisplayName("Should calculate correct late hours for 3 hours late")
        void shouldCalculateCorrectLateHoursFor3HoursLate() {
            Instant currentTime = UtcDates.endOfDayWholeSecond(testRental.getEndDate()).plus(Duration.ofHours(4));

            long lateHours = service.calculateLateHours(testRental, currentTime);

            assertThat(lateHours).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Severely Late Detection (24+ hours)")
    class SeverelyLateDetection {

        @Test
        @DisplayName("Should return SEVERELY_LATE when exactly 24 hours late")
        void shouldReturnSeverelyLateWhenExactly24HoursLate() {
            Instant currentTime = UtcDates.endOfDayWholeSecond(testRental.getEndDate()).plus(Duration.ofHours(25));

            LateReturnStatus status = service.calculateLateStatus(testRental, currentTime);

            assertThat(status).isEqualTo(LateReturnStatus.SEVERELY_LATE);
        }

        @Test
        @DisplayName("Should return SEVERELY_LATE when 48 hours late")
        void shouldReturnSeverelyLateWhen48HoursLate() {
            Instant currentTime = UtcDates.endOfDayWholeSecond(testRental.getEndDate()).plus(Duration.ofHours(49));

            LateReturnStatus status = service.calculateLateStatus(testRental, currentTime);

            assertThat(status).isEqualTo(LateReturnStatus.SEVERELY_LATE);
        }

        @Test
        @DisplayName("Should return SEVERELY_LATE when 5 days late")
        void shouldReturnSeverelyLateWhen5DaysLate() {
            Instant currentTime = UtcDates.endOfDayWholeSecond(testRental.getEndDate()).plus(Duration.ofDays(5));

            LateReturnStatus status = service.calculateLateStatus(testRental, currentTime);

            assertThat(status).isEqualTo(LateReturnStatus.SEVERELY_LATE);
        }

        @Test
        @DisplayName("Should calculate correct late hours for 48 hours late")
        void shouldCalculateCorrectLateHoursFor48HoursLate() {
            Instant currentTime = UtcDates.endOfDayWholeSecond(testRental.getEndDate()).plus(Duration.ofHours(49));

            long lateHours = service.calculateLateHours(testRental, currentTime);

            assertThat(lateHours).isEqualTo(48);
        }

        @Test
        @DisplayName("Should calculate correct late days for 3 days late")
        void shouldCalculateCorrectLateDaysFor3DaysLate() {
            Instant currentTime = UtcDates.endOfDayWholeSecond(testRental.getEndDate()).plus(Duration.ofDays(3)).plus(Duration.ofHours(1));

            long lateDays = service.calculateLateDays(testRental, currentTime);

            assertThat(lateDays).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Multiple Rentals Detection")
    class MultipleRentalsDetection {

        @Test
        @DisplayName("Should process multiple overdue rentals successfully")
        void shouldProcessMultipleOverdueRentalsSuccessfully() {
            Rental rental1 = Rental.builder()
                    .id(1L)
                    .userId(testUser.getId())
                    .userEmail(testUser.getEmail())
                    .userFullName("Test User")
                    .carId(testCar.getId())
                    .carBrand(testCar.getBrand())
                    .carModel(testCar.getModel())
                    .carLicensePlate(testCar.getLicensePlate())
                    .endDate(LocalDate.now(clock).minusDays(1))
                    .dailyPrice(new BigDecimal("500.00"))
                    .currency(CurrencyType.TRY)
                    .status(RentalStatus.IN_USE)
                    .lateReturnStatus(LateReturnStatus.ON_TIME)
                    .build();

            Rental rental2 = Rental.builder()
                    .id(2L)
                    .userId(testUser.getId())
                    .userEmail(testUser.getEmail())
                    .userFullName("Test User")
                    .carId(testCar.getId())
                    .carBrand(testCar.getBrand())
                    .carModel(testCar.getModel())
                    .carLicensePlate(testCar.getLicensePlate())
                    .endDate(LocalDate.now(clock).minusDays(2))
                    .dailyPrice(new BigDecimal("600.00"))
                    .currency(CurrencyType.TRY)
                    .status(RentalStatus.IN_USE)
                    .lateReturnStatus(LateReturnStatus.ON_TIME)
                    .build();

            overdue(rental1, rental2);

            PenaltyResult penaltyResult = new PenaltyResult(
                    new BigDecimal("100.00"),
                    new BigDecimal("500.00"),
                    2,
                    0,
                    LateReturnStatus.LATE,
                    "Test",
                    false
            );
            when(penaltyCalculationService.calculatePenalty(any(Rental.class), any(Instant.class)))
                    .thenReturn(penaltyResult);

            service.detectLateReturns();

            verify(rentalRepository, times(2)).save(any(Rental.class));
        }

        @Test
        @DisplayName("Should handle empty overdue rentals list")
        void shouldHandleEmptyOverdueRentalsList() {
            overdue();

            service.detectLateReturns();

            verify(rentalRepository, never()).save(any(Rental.class));
        }

        @Test
        @DisplayName("Should continue processing after individual rental error")
        void shouldContinueProcessingAfterIndividualRentalError() {
            Rental rental1 = Rental.builder()
                    .id(1L)
                    .userId(testUser.getId())
                    .userEmail(testUser.getEmail())
                    .userFullName("Test User")
                    .carId(testCar.getId())
                    .carBrand(testCar.getBrand())
                    .carModel(testCar.getModel())
                    .carLicensePlate(testCar.getLicensePlate())
                    .endDate(LocalDate.now(clock).minusDays(1))
                    .dailyPrice(new BigDecimal("500.00"))
                    .currency(CurrencyType.TRY)
                    .status(RentalStatus.IN_USE)
                    .lateReturnStatus(LateReturnStatus.ON_TIME)
                    .build();

            Rental rental2 = Rental.builder()
                    .id(2L)
                    .userId(testUser.getId())
                    .userEmail(testUser.getEmail())
                    .userFullName("Test User")
                    .carId(testCar.getId())
                    .carBrand(testCar.getBrand())
                    .carModel(testCar.getModel())
                    .carLicensePlate(testCar.getLicensePlate())
                    .endDate(LocalDate.now(clock).minusDays(2))
                    .dailyPrice(new BigDecimal("600.00"))
                    .currency(CurrencyType.TRY)
                    .status(RentalStatus.IN_USE)
                    .lateReturnStatus(LateReturnStatus.ON_TIME)
                    .build();

            overdue(rental1, rental2);

            when(rentalRepository.save(rental1)).thenThrow(new RuntimeException("Database error"));
            when(rentalRepository.save(rental2)).thenReturn(rental2);

            PenaltyResult penaltyResult = new PenaltyResult(
                    new BigDecimal("100.00"),
                    new BigDecimal("500.00"),
                    2,
                    0,
                    LateReturnStatus.LATE,
                    "Test",
                    false
            );
            when(penaltyCalculationService.calculatePenalty(any(Rental.class), any(Instant.class)))
                    .thenReturn(penaltyResult);

            service.detectLateReturns();

            verify(rentalRepository, times(2)).save(any(Rental.class));
        }

        @Test
        @DisplayName("Should commit each rental in its own transaction so one failure does not roll back the others")
        void shouldCommitEachRentalInItsOwnTransaction() {
            Rental rental1 = lateRental(1L, 1);
            Rental rental2 = lateRental(2L, 2);
            overdue(rental1, rental2);
            when(rentalRepository.save(rental1)).thenThrow(new RuntimeException("flush failed"));

            service.detectLateReturns();

            verify(transactionManager, times(2)).getTransaction(any());
            verify(transactionManager, times(1)).rollback(any());
            verify(transactionManager, times(1)).commit(any());
            assertThat(rental2.getLateReturnStatus()).isNotEqualTo(LateReturnStatus.ON_TIME);
        }

        @Test
        @DisplayName("Should skip a rental that was returned after the overdue list was read")
        void shouldSkipARentalReturnedInTheMeantime() {
            Rental returned = lateRental(1L, 2);
            ReflectionTestUtils.setField(returned, "status", RentalStatus.RETURNED);
            overdue(returned);

            service.detectLateReturns();

            verify(rentalRepository, never()).save(any(Rental.class));
        }

        private Rental lateRental(Long id, int daysAgo) {
            return Rental.builder()
                    .id(id)
                    .userId(testUser.getId())
                    .userEmail(testUser.getEmail())
                    .userFullName("Test User")
                    .carId(testCar.getId())
                    .carBrand(testCar.getBrand())
                    .carModel(testCar.getModel())
                    .carLicensePlate(testCar.getLicensePlate())
                    .endDate(LocalDate.now(clock).minusDays(daysAgo))
                    .dailyPrice(new BigDecimal("500.00"))
                    .currency(CurrencyType.TRY)
                    .status(RentalStatus.IN_USE)
                    .lateReturnStatus(LateReturnStatus.ON_TIME)
                    .build();
        }
    }

    @Nested
    @DisplayName("Edge Cases and Validation")
    class EdgeCasesAndValidation {

        @Test
        @DisplayName("Should throw exception when rental is null")
        void shouldThrowExceptionWhenRentalIsNull() {
            Instant currentTime = Instant.now(clock);

            assertThatThrownBy(() -> service.calculateLateStatus(null, currentTime))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Rental and end date cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when end date is null")
        void shouldThrowExceptionWhenEndDateIsNull() {
            ReflectionTestUtils.setField(testRental, "endDate", null);
            Instant currentTime = Instant.now(clock);

            assertThatThrownBy(() -> service.calculateLateStatus(testRental, currentTime))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Rental and end date cannot be null");
        }

        @Test
        @DisplayName("Should return zero late hours when rental is null")
        void shouldReturnZeroLateHoursWhenRentalIsNull() {
            Instant currentTime = Instant.now(clock);

            long lateHours = service.calculateLateHours(null, currentTime);

            assertThat(lateHours).isEqualTo(0);
        }

        @Test
        @DisplayName("Should set late detected timestamp on first late detection")
        void shouldSetLateDetectedTimestampOnFirstLateDetection() {
            ReflectionTestUtils.setField(testRental, "endDate", LocalDate.now(clock).minusDays(3));
            ReflectionTestUtils.setField(testRental, "lateDetectedAt", null);
            ReflectionTestUtils.setField(testRental, "lateReturnStatus", LateReturnStatus.ON_TIME);

            overdue(testRental);

            PenaltyResult penaltyResult = new PenaltyResult(
                    new BigDecimal("100.00"),
                    new BigDecimal("500.00"),
                    48,
                    2,
                    LateReturnStatus.SEVERELY_LATE,
                    "Test",
                    false
            );
            when(penaltyCalculationService.calculatePenalty(any(Rental.class), any(Instant.class)))
                    .thenReturn(penaltyResult);

            service.detectLateReturns();

            assertThat(testRental.getLateDetectedAt()).isNotNull();
            assertThat(testRental.getLateReturnStatus()).isEqualTo(LateReturnStatus.SEVERELY_LATE);
        }

        @Test
        @DisplayName("Should not update late detected timestamp if already set")
        void shouldNotUpdateLateDetectedTimestampIfAlreadySet() {
            Instant originalDetectionTime = Instant.now(clock).minus(Duration.ofHours(5));
            ReflectionTestUtils.setField(testRental, "lateDetectedAt", originalDetectionTime);
            ReflectionTestUtils.setField(testRental, "lateReturnStatus", LateReturnStatus.LATE);

            overdue(testRental);

            PenaltyResult penaltyResult = new PenaltyResult(
                    new BigDecimal("200.00"),
                    new BigDecimal("500.00"),
                    4,
                    0,
                    LateReturnStatus.SEVERELY_LATE,
                    "Test",
                    false
            );
            when(penaltyCalculationService.calculatePenalty(any(Rental.class), any(Instant.class)))
                    .thenReturn(penaltyResult);

            service.detectLateReturns();

            assertThat(testRental.getLateDetectedAt()).isEqualTo(originalDetectionTime);
        }
    }
}
