package com.akif.rental.unit.penalty;

import com.akif.car.domain.Car;
import com.akif.rental.domain.enums.LateReturnStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.config.PenaltyConfig;
import com.akif.rental.internal.dto.penalty.PenaltyResult;
import com.akif.rental.internal.service.penalty.impl.PenaltyCalculationServiceImpl;
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
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PenaltyCalculationServiceImpl Unit Tests")
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class PenaltyCalculationServiceImplTest {

    @Mock
    private PenaltyConfig penaltyConfig;

    @InjectMocks
    private PenaltyCalculationServiceImpl service;

    private Rental testRental;
    private Car testCar;

    @BeforeEach
    void setUp() {
        when(penaltyConfig.getGracePeriodMinutes()).thenReturn(60);
        when(penaltyConfig.getHourlyPenaltyRate()).thenReturn(new BigDecimal("0.10"));
        when(penaltyConfig.getDailyPenaltyRate()).thenReturn(new BigDecimal("1.50"));
        when(penaltyConfig.getPenaltyCapMultiplier()).thenReturn(new BigDecimal("5.0"));
        when(penaltyConfig.getSeverelyLateThresholdHours()).thenReturn(24);

        testCar = Car.builder()
                .id(1L)
                .brand("Toyota")
                .model("Corolla")
                .licensePlate("34ABC123")
                .build();

        testRental = Rental.builder()
                .id(1L)
                .carId(testCar.getId())
                .carBrand(testCar.getBrand())
                .carModel(testCar.getModel())
                .carLicensePlate(testCar.getLicensePlate())
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().minusDays(2))
                .dailyPrice(new BigDecimal("500.00"))
                .currency(CurrencyType.TRY)
                .build();
    }

    @Nested
    @DisplayName("Hourly Penalty Calculation (1-6 hours)")
    class HourlyPenaltyCalculation {

        @Test
        @DisplayName("Should calculate penalty for 1 hour late")
        void shouldCalculatePenaltyFor1HourLate() {
            LocalDateTime returnTime = testRental.getEndDate().atTime(23, 59, 59).plusHours(2);

            PenaltyResult result = service.calculatePenalty(testRental, returnTime);

            assertThat(result).isNotNull();
            assertThat(result.lateHours()).isEqualTo(1);
            assertThat(result.penaltyAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(result.status()).isEqualTo(LateReturnStatus.LATE);
            assertThat(result.cappedAtMax()).isFalse();
        }

        @Test
        @DisplayName("Should calculate penalty for 3 hours late")
        void shouldCalculatePenaltyFor3HoursLate() {
            LocalDateTime returnTime = testRental.getEndDate().atTime(23, 59, 59).plusHours(4); // 3 hours after grace

            PenaltyResult result = service.calculatePenalty(testRental, returnTime);

            assertThat(result.lateHours()).isEqualTo(3);
            assertThat(result.penaltyAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(result.status()).isEqualTo(LateReturnStatus.LATE);
        }

        @Test
        @DisplayName("Should calculate penalty for 6 hours late")
        void shouldCalculatePenaltyFor6HoursLate() {
            LocalDateTime returnTime = testRental.getEndDate().atTime(23, 59, 59).plusHours(7);

            PenaltyResult result = service.calculatePenalty(testRental, returnTime);

            assertThat(result.lateHours()).isEqualTo(6);
            assertThat(result.penaltyAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
            assertThat(result.status()).isEqualTo(LateReturnStatus.LATE);
        }

        @Test
        @DisplayName("Should throw exception for invalid hourly penalty range")
        void shouldThrowExceptionForInvalidHourlyPenaltyRange() {
            BigDecimal dailyRate = new BigDecimal("500.00");

            assertThatThrownBy(() -> service.calculateHourlyPenalty(dailyRate, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Hourly penalty applies only for 1-6 hours late");

            assertThatThrownBy(() -> service.calculateHourlyPenalty(dailyRate, 7))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Hourly penalty applies only for 1-6 hours late");
        }
    }

    @Nested
    @DisplayName("Daily Penalty Calculation (7-24 hours, multi-day)")
    class DailyPenaltyCalculation {

        @Test
        @DisplayName("Should calculate penalty for 7-24 hours late (1 day)")
        void shouldCalculatePenaltyFor7To24HoursLate() {
            LocalDateTime returnTime = testRental.getEndDate().atTime(23, 59, 59).plusHours(10);

            PenaltyResult result = service.calculatePenalty(testRental, returnTime);

            assertThat(result.lateHours()).isEqualTo(9);
            assertThat(result.lateDays()).isEqualTo(0);
            assertThat(result.penaltyAmount()).isEqualByComparingTo(new BigDecimal("750.00"));
            assertThat(result.status()).isEqualTo(LateReturnStatus.LATE);
        }

        @Test
        @DisplayName("Should calculate penalty for exactly 24 hours late")
        void shouldCalculatePenaltyForExactly24HoursLate() {
            LocalDateTime returnTime = testRental.getEndDate().atTime(23, 59, 59).plusHours(25);

            PenaltyResult result = service.calculatePenalty(testRental, returnTime);

            assertThat(result.lateHours()).isEqualTo(24);
            assertThat(result.status()).isEqualTo(LateReturnStatus.SEVERELY_LATE);
            assertThat(result.penaltyAmount()).isEqualByComparingTo(new BigDecimal("750.00"));
        }

        @Test
        @DisplayName("Should calculate penalty for 2 days late")
        void shouldCalculatePenaltyFor2DaysLate() {
            LocalDateTime returnTime = testRental.getEndDate().atTime(23, 59, 59).plusHours(49);

            PenaltyResult result = service.calculatePenalty(testRental, returnTime);

            assertThat(result.lateHours()).isEqualTo(48);
            assertThat(result.lateDays()).isEqualTo(2);
            assertThat(result.penaltyAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(result.status()).isEqualTo(LateReturnStatus.SEVERELY_LATE);
        }

        @Test
        @DisplayName("Should calculate penalty for 5 days late")
        void shouldCalculatePenaltyFor5DaysLate() {
            LocalDateTime returnTime = testRental.getEndDate().atTime(23, 59, 59).plusDays(5).plusHours(1);

            PenaltyResult result = service.calculatePenalty(testRental, returnTime);

            assertThat(result.lateDays()).isEqualTo(5);
            assertThat(result.penaltyAmount()).isEqualByComparingTo(new BigDecimal("2500.00"));
            assertThat(result.status()).isEqualTo(LateReturnStatus.SEVERELY_LATE);
            assertThat(result.cappedAtMax()).isTrue();
        }

        @Test
        @DisplayName("Should throw exception for invalid daily penalty range")
        void shouldThrowExceptionForInvalidDailyPenaltyRange() {
            BigDecimal dailyRate = new BigDecimal("500.00");

            assertThatThrownBy(() -> service.calculateDailyPenalty(dailyRate, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Daily penalty applies only for 1+ days late");

            assertThatThrownBy(() -> service.calculateDailyPenalty(dailyRate, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Daily penalty applies only for 1+ days late");
        }
    }

    @Nested
    @DisplayName("Penalty Cap Enforcement")
    class PenaltyCapEnforcement {

        @Test
        @DisplayName("Should enforce penalty cap at 5x daily rate")
        void shouldEnforcePenaltyCapAt5xDailyRate() {
            LocalDateTime returnTime = testRental.getEndDate().atTime(23, 59, 59).plusDays(10);

            PenaltyResult result = service.calculatePenalty(testRental, returnTime);

            BigDecimal expectedCap = new BigDecimal("2500.00"); // 500 * 5
            assertThat(result.penaltyAmount()).isEqualByComparingTo(expectedCap);
            assertThat(result.cappedAtMax()).isTrue();
            assertThat(result.breakdown()).contains("capped at 5x daily rate");
        }

        @Test
        @DisplayName("Should not apply cap when penalty is below maximum")
        void shouldNotApplyCapWhenPenaltyIsBelowMaximum() {
            LocalDateTime returnTime = testRental.getEndDate().atTime(23, 59, 59).plusHours(3);

            PenaltyResult result = service.calculatePenalty(testRental, returnTime);

            assertThat(result.penaltyAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(result.cappedAtMax()).isFalse();
            assertThat(result.breakdown()).doesNotContain("capped");
        }

        @Test
        @DisplayName("Should apply penalty cap correctly with applyPenaltyCap method")
        void shouldApplyPenaltyCapCorrectly() {
            BigDecimal dailyRate = new BigDecimal("500.00");
            BigDecimal excessivePenalty = new BigDecimal("3000.00");
            BigDecimal normalPenalty = new BigDecimal("200.00");

            BigDecimal cappedExcessive = service.applyPenaltyCap(excessivePenalty, dailyRate);
            BigDecimal cappedNormal = service.applyPenaltyCap(normalPenalty, dailyRate);

            assertThat(cappedExcessive).isEqualByComparingTo(new BigDecimal("2500.00"));
            assertThat(cappedNormal).isEqualByComparingTo(new BigDecimal("200.00"));
        }
    }

    @Nested
    @DisplayName("Grace Period Zero Penalty")
    class GracePeriodZeroPenalty {

        @Test
        @DisplayName("Should apply zero penalty within grace period")
        void shouldApplyZeroPenaltyWithinGracePeriod() {
            LocalDateTime returnTime = testRental.getEndDate().atTime(23, 59, 59).plusMinutes(30); // 30 min late

            PenaltyResult result = service.calculatePenalty(testRental, returnTime);

            assertThat(result.penaltyAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.status()).isEqualTo(LateReturnStatus.GRACE_PERIOD);
            assertThat(result.lateHours()).isEqualTo(0);
            assertThat(result.lateDays()).isEqualTo(0);
            assertThat(result.breakdown()).contains("Within grace period");
        }

        @Test
        @DisplayName("Should apply zero penalty at exactly grace period limit")
        void shouldApplyZeroPenaltyAtExactlyGracePeriodLimit() {
            LocalDateTime returnTime = testRental.getEndDate().atTime(23, 59, 59).plusMinutes(60); // Exactly 60 min

            PenaltyResult result = service.calculatePenalty(testRental, returnTime);

            assertThat(result.penaltyAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.status()).isEqualTo(LateReturnStatus.GRACE_PERIOD);
        }

        @Test
        @DisplayName("Should apply penalty immediately after grace period")
        void shouldApplyPenaltyImmediatelyAfterGracePeriod() {
            LocalDateTime returnTime = testRental.getEndDate().atTime(23, 59, 59).plusMinutes(61);

            PenaltyResult result = service.calculatePenalty(testRental, returnTime);

            assertThat(result.penaltyAmount()).isGreaterThan(BigDecimal.ZERO);
            assertThat(result.status()).isEqualTo(LateReturnStatus.LATE);
            assertThat(result.lateHours()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle fractional hours correctly with ceiling")
        void shouldHandleFractionalHoursCorrectlyWithCeiling() {
            LocalDateTime returnTime = testRental.getEndDate().atTime(23, 59, 59).plusMinutes(150);

            PenaltyResult result = service.calculatePenalty(testRental, returnTime);

            assertThat(result.lateHours()).isEqualTo(2);
            assertThat(result.penaltyAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("Should include breakdown information in result")
        void shouldIncludeBreakdownInformationInResult() {
            LocalDateTime returnTime = testRental.getEndDate().atTime(23, 59, 59).plusHours(3);

            PenaltyResult result = service.calculatePenalty(testRental, returnTime);

            assertThat(result.breakdown()).isNotNull();
            assertThat(result.breakdown()).contains("Hourly penalty");
            assertThat(result.breakdown()).contains("hours");
            assertThat(result.breakdown()).contains("500");
        }

        @Test
        @DisplayName("Should handle different daily rates correctly")
        void shouldHandleDifferentDailyRatesCorrectly() {
            testRental.setDailyPrice(new BigDecimal("1000.00"));
            LocalDateTime returnTime = testRental.getEndDate().atTime(23, 59, 59).plusHours(2);

            PenaltyResult result = service.calculatePenalty(testRental, returnTime);

            assertThat(result.penaltyAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(result.dailyRate()).isEqualByComparingTo(new BigDecimal("1000.00"));
        }
    }
}
