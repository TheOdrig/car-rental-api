package com.akif.rental.unit.detection;

import com.akif.auth.domain.User;
import com.akif.car.domain.Car;
import com.akif.rental.domain.enums.LateReturnStatus;
import com.akif.rental.domain.enums.RentalStatus;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @InjectMocks
    private LateReturnDetectionServiceImpl service;

    private Rental testRental;
    private Car testCar;
    private User testUser;

    @BeforeEach
    void setUp() {
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
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().minusDays(2))
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
            LocalDateTime currentTime = testRental.getEndDate().atTime(12, 0, 0);

            LateReturnStatus status = service.calculateLateStatus(testRental, currentTime);

            assertThat(status).isEqualTo(LateReturnStatus.ON_TIME);
        }

        @Test
        @DisplayName("Should return ON_TIME when returned exactly at end time")
        void shouldReturnOnTimeWhenReturnedExactlyAtEndTime() {
            LocalDateTime currentTime = testRental.getEndDate().atTime(23, 59, 59);

            LateReturnStatus status = service.calculateLateStatus(testRental, currentTime);

            assertThat(status).isEqualTo(LateReturnStatus.ON_TIME);
        }

        @Test
        @DisplayName("Should calculate zero late hours for on-time rental")
        void shouldCalculateZeroLateHoursForOnTimeRental() {
            LocalDateTime currentTime = testRental.getEndDate().atTime(12, 0, 0);

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
            LocalDateTime currentTime = testRental.getEndDate().atTime(23, 59, 59).plusMinutes(30);

            LateReturnStatus status = service.calculateLateStatus(testRental, currentTime);

            assertThat(status).isEqualTo(LateReturnStatus.GRACE_PERIOD);
        }

        @Test
        @DisplayName("Should return GRACE_PERIOD at exactly grace period limit")
        void shouldReturnGracePeriodAtExactlyGracePeriodLimit() {
            LocalDateTime currentTime = testRental.getEndDate().atTime(23, 59, 59).plusMinutes(60);

            LateReturnStatus status = service.calculateLateStatus(testRental, currentTime);

            assertThat(status).isEqualTo(LateReturnStatus.GRACE_PERIOD);
        }

        @Test
        @DisplayName("Should calculate zero late hours during grace period")
        void shouldCalculateZeroLateHoursDuringGracePeriod() {
            LocalDateTime currentTime = testRental.getEndDate().atTime(23, 59, 59).plusMinutes(45);

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
            LocalDateTime currentTime = testRental.getEndDate().atTime(23, 59, 59).plusMinutes(62);

            LateReturnStatus status = service.calculateLateStatus(testRental, currentTime);

            assertThat(status).isEqualTo(LateReturnStatus.LATE);
        }

        @Test
        @DisplayName("Should return LATE when 5 hours late")
        void shouldReturnLateWhen5HoursLate() {
            LocalDateTime currentTime = testRental.getEndDate().atTime(23, 59, 59).plusHours(6); // 5 hours after grace

            LateReturnStatus status = service.calculateLateStatus(testRental, currentTime);

            assertThat(status).isEqualTo(LateReturnStatus.LATE);
        }

        @Test
        @DisplayName("Should return LATE when 23 hours late")
        void shouldReturnLateWhen23HoursLate() {
            LocalDateTime currentTime = testRental.getEndDate().atTime(23, 59, 59).plusHours(24); // 23 hours after grace

            LateReturnStatus status = service.calculateLateStatus(testRental, currentTime);

            assertThat(status).isEqualTo(LateReturnStatus.LATE);
        }

        @Test
        @DisplayName("Should calculate correct late hours for 3 hours late")
        void shouldCalculateCorrectLateHoursFor3HoursLate() {
            LocalDateTime currentTime = testRental.getEndDate().atTime(23, 59, 59).plusHours(4); // 3 hours after grace

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
            LocalDateTime currentTime = testRental.getEndDate().atTime(23, 59, 59).plusHours(25); // 24 hours after grace

            LateReturnStatus status = service.calculateLateStatus(testRental, currentTime);

            assertThat(status).isEqualTo(LateReturnStatus.SEVERELY_LATE);
        }

        @Test
        @DisplayName("Should return SEVERELY_LATE when 48 hours late")
        void shouldReturnSeverelyLateWhen48HoursLate() {
            LocalDateTime currentTime = testRental.getEndDate().atTime(23, 59, 59).plusHours(49);

            LateReturnStatus status = service.calculateLateStatus(testRental, currentTime);

            assertThat(status).isEqualTo(LateReturnStatus.SEVERELY_LATE);
        }

        @Test
        @DisplayName("Should return SEVERELY_LATE when 5 days late")
        void shouldReturnSeverelyLateWhen5DaysLate() {
            LocalDateTime currentTime = testRental.getEndDate().atTime(23, 59, 59).plusDays(5);

            LateReturnStatus status = service.calculateLateStatus(testRental, currentTime);

            assertThat(status).isEqualTo(LateReturnStatus.SEVERELY_LATE);
        }

        @Test
        @DisplayName("Should calculate correct late hours for 48 hours late")
        void shouldCalculateCorrectLateHoursFor48HoursLate() {
            LocalDateTime currentTime = testRental.getEndDate().atTime(23, 59, 59).plusHours(49);

            long lateHours = service.calculateLateHours(testRental, currentTime);

            assertThat(lateHours).isEqualTo(48);
        }

        @Test
        @DisplayName("Should calculate correct late days for 3 days late")
        void shouldCalculateCorrectLateDaysFor3DaysLate() {
            LocalDateTime currentTime = testRental.getEndDate().atTime(23, 59, 59).plusDays(3).plusHours(1);

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
                    .endDate(LocalDate.now().minusDays(1))
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
                    .endDate(LocalDate.now().minusDays(2))
                    .dailyPrice(new BigDecimal("600.00"))
                    .currency(CurrencyType.TRY)
                    .status(RentalStatus.IN_USE)
                    .lateReturnStatus(LateReturnStatus.ON_TIME)
                    .build();

            Page<Rental> page = new PageImpl<>(Arrays.asList(rental1, rental2));
            when(rentalRepository.findOverdueRentals(any(LocalDate.class), any(Pageable.class)))
                    .thenReturn(page);

            PenaltyResult penaltyResult = new PenaltyResult(
                    new BigDecimal("100.00"),
                    new BigDecimal("500.00"),
                    2,
                    0,
                    LateReturnStatus.LATE,
                    "Test",
                    false
            );
            when(penaltyCalculationService.calculatePenalty(any(Rental.class), any(LocalDateTime.class)))
                    .thenReturn(penaltyResult);

            service.detectLateReturns();

            verify(rentalRepository, times(1)).findOverdueRentals(any(LocalDate.class), any(Pageable.class));
            verify(rentalRepository, times(2)).save(any(Rental.class));
        }

        @Test
        @DisplayName("Should handle empty overdue rentals list")
        void shouldHandleEmptyOverdueRentalsList() {
            Page<Rental> emptyPage = new PageImpl<>(Collections.emptyList());
            when(rentalRepository.findOverdueRentals(any(LocalDate.class), any(Pageable.class)))
                    .thenReturn(emptyPage);

            service.detectLateReturns();

            verify(rentalRepository, times(1)).findOverdueRentals(any(LocalDate.class), any(Pageable.class));
            verify(rentalRepository, never()).save(any(Rental.class));
        }

        @Test
        @DisplayName("Should handle pagination correctly")
        void shouldHandlePaginationCorrectly() {
            Rental rental1 = Rental.builder()
                    .id(1L)
                    .userId(testUser.getId())
                    .userEmail(testUser.getEmail())
                    .userFullName("Test User")
                    .carId(testCar.getId())
                    .carBrand(testCar.getBrand())
                    .carModel(testCar.getModel())
                    .carLicensePlate(testCar.getLicensePlate())
                    .endDate(LocalDate.now().minusDays(1))
                    .dailyPrice(new BigDecimal("500.00"))
                    .currency(CurrencyType.TRY)
                    .status(RentalStatus.IN_USE)
                    .lateReturnStatus(LateReturnStatus.ON_TIME)
                    .build();

            Page<Rental> page1 = new PageImpl<>(Collections.singletonList(rental1), PageRequest.of(0, 50), 51);
            Page<Rental> page2 = new PageImpl<>(Collections.emptyList(), PageRequest.of(1, 50), 51);

            when(rentalRepository.findOverdueRentals(any(LocalDate.class), eq(PageRequest.of(0, 50))))
                    .thenReturn(page1);
            when(rentalRepository.findOverdueRentals(any(LocalDate.class), eq(PageRequest.of(1, 50))))
                    .thenReturn(page2);

            PenaltyResult penaltyResult = new PenaltyResult(
                    new BigDecimal("100.00"),
                    new BigDecimal("500.00"),
                    2,
                    0,
                    LateReturnStatus.LATE,
                    "Test",
                    false
            );
            when(penaltyCalculationService.calculatePenalty(any(Rental.class), any(LocalDateTime.class)))
                    .thenReturn(penaltyResult);

            service.detectLateReturns();

            verify(rentalRepository, times(2)).findOverdueRentals(any(LocalDate.class), any(Pageable.class));
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
                    .endDate(LocalDate.now().minusDays(1))
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
                    .endDate(LocalDate.now().minusDays(2))
                    .dailyPrice(new BigDecimal("600.00"))
                    .currency(CurrencyType.TRY)
                    .status(RentalStatus.IN_USE)
                    .lateReturnStatus(LateReturnStatus.ON_TIME)
                    .build();

            Page<Rental> page = new PageImpl<>(Arrays.asList(rental1, rental2));
            when(rentalRepository.findOverdueRentals(any(LocalDate.class), any(Pageable.class)))
                    .thenReturn(page);

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
            when(penaltyCalculationService.calculatePenalty(any(Rental.class), any(LocalDateTime.class)))
                    .thenReturn(penaltyResult);

            service.detectLateReturns();

            verify(rentalRepository, times(2)).save(any(Rental.class));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Validation")
    class EdgeCasesAndValidation {

        @Test
        @DisplayName("Should throw exception when rental is null")
        void shouldThrowExceptionWhenRentalIsNull() {
            LocalDateTime currentTime = LocalDateTime.now();

            assertThatThrownBy(() -> service.calculateLateStatus(null, currentTime))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Rental and end date cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when end date is null")
        void shouldThrowExceptionWhenEndDateIsNull() {
            testRental.setEndDate(null);
            LocalDateTime currentTime = LocalDateTime.now();

            assertThatThrownBy(() -> service.calculateLateStatus(testRental, currentTime))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Rental and end date cannot be null");
        }

        @Test
        @DisplayName("Should return zero late hours when rental is null")
        void shouldReturnZeroLateHoursWhenRentalIsNull() {
            LocalDateTime currentTime = LocalDateTime.now();

            long lateHours = service.calculateLateHours(null, currentTime);

            assertThat(lateHours).isEqualTo(0);
        }

        @Test
        @DisplayName("Should set late detected timestamp on first late detection")
        void shouldSetLateDetectedTimestampOnFirstLateDetection() {
            testRental.setEndDate(LocalDate.now().minusDays(3));
            testRental.setLateDetectedAt(null);
            testRental.setLateReturnStatus(LateReturnStatus.ON_TIME);

            Page<Rental> page = new PageImpl<>(Collections.singletonList(testRental));
            when(rentalRepository.findOverdueRentals(any(LocalDate.class), any(Pageable.class)))
                    .thenReturn(page);

            PenaltyResult penaltyResult = new PenaltyResult(
                    new BigDecimal("100.00"),
                    new BigDecimal("500.00"),
                    48,
                    2,
                    LateReturnStatus.SEVERELY_LATE,
                    "Test",
                    false
            );
            when(penaltyCalculationService.calculatePenalty(any(Rental.class), any(LocalDateTime.class)))
                    .thenReturn(penaltyResult);

            service.detectLateReturns();

            assertThat(testRental.getLateDetectedAt()).isNotNull();
            assertThat(testRental.getLateReturnStatus()).isEqualTo(LateReturnStatus.SEVERELY_LATE);
        }

        @Test
        @DisplayName("Should not update late detected timestamp if already set")
        void shouldNotUpdateLateDetectedTimestampIfAlreadySet() {
            LocalDateTime originalDetectionTime = LocalDateTime.now().minusHours(5);
            testRental.setLateDetectedAt(originalDetectionTime);
            testRental.setLateReturnStatus(LateReturnStatus.LATE);

            Page<Rental> page = new PageImpl<>(Collections.singletonList(testRental));
            when(rentalRepository.findOverdueRentals(any(LocalDate.class), any(Pageable.class)))
                    .thenReturn(page);

            PenaltyResult penaltyResult = new PenaltyResult(
                    new BigDecimal("200.00"),
                    new BigDecimal("500.00"),
                    4,
                    0,
                    LateReturnStatus.SEVERELY_LATE,
                    "Test",
                    false
            );
            when(penaltyCalculationService.calculatePenalty(any(Rental.class), any(LocalDateTime.class)))
                    .thenReturn(penaltyResult);

            service.detectLateReturns();

            assertThat(testRental.getLateDetectedAt()).isEqualTo(originalDetectionTime);
        }
    }
}
