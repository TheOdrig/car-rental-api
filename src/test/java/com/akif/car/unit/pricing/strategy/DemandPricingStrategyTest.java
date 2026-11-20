package com.akif.car.unit.pricing.strategy;

import com.akif.car.api.CarBooking;
import com.akif.car.api.CarBookingCalendar;
import com.akif.car.api.PriceModifier;
import com.akif.car.internal.config.PricingConfig;
import com.akif.testsupport.ConfigFixtures;
import com.akif.car.internal.dto.pricing.PricingContext;
import com.akif.car.internal.repository.CarRepository;
import com.akif.car.internal.service.pricing.strategy.DemandPricingStrategy;
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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DemandPricingStrategy Unit Tests")
class DemandPricingStrategyTest {

    private static final LocalDate START = LocalDate.of(2031, 3, 10);
    private static final LocalDate END = LocalDate.of(2031, 3, 15);

    @Mock
    private PricingConfig config;

    @Mock
    private CarBookingCalendar bookingCalendar;

    @Mock
    private CarRepository carRepository;

    @InjectMocks
    private DemandPricingStrategy strategy;


    @BeforeEach
    void setUp() {
        lenient().when(config.demand()).thenReturn(ConfigFixtures.demand());
        lenient().when(config.strategy()).thenReturn(ConfigFixtures.strategies(true));
    }

    private PricingContext context() {
        return new PricingContext(1L, new BigDecimal("500"), START, END, START.minusDays(10), 5, 10, "SUV");
    }

    private void fleet(int size, int booked) {
        List<Long> ids = LongStream.rangeClosed(1, size).boxed().collect(Collectors.toList());
        when(carRepository.findBookableCarIds(any())).thenReturn(ids);
        List<CarBooking> bookings = LongStream.rangeClosed(1, booked)
                .mapToObj(id -> new CarBooking(id, START, END)).collect(Collectors.toList());
        when(bookingCalendar.findCommittedBookings(eq(START), eq(END))).thenReturn(bookings);
    }

    @Nested
    @DisplayName("Fleet occupancy tiers")
    class FleetOccupancy {

        @Test
        @DisplayName("Applies high surcharge above 80% fleet occupancy")
        void high() {
            fleet(10, 9);

            PriceModifier result = strategy.calculate(context());

            assertThat(result.multiplier()).isEqualByComparingTo(new BigDecimal("1.20"));
            assertThat(result.isDiscount()).isFalse();
            assertThat(result.description()).contains("High demand").contains("90%");
        }

        @Test
        @DisplayName("Exactly 80% is moderate, not high")
        void exactlyEighty() {
            fleet(10, 8);

            assertThat(strategy.calculate(context()).multiplier()).isEqualByComparingTo(new BigDecimal("1.10"));
        }

        @Test
        @DisplayName("Exactly 50% is moderate")
        void exactlyFifty() {
            fleet(10, 5);

            PriceModifier result = strategy.calculate(context());

            assertThat(result.multiplier()).isEqualByComparingTo(new BigDecimal("1.10"));
            assertThat(result.description()).contains("Moderate demand").contains("50%");
        }

        @Test
        @DisplayName("Below 50% is neutral")
        void normal() {
            fleet(10, 4);

            PriceModifier result = strategy.calculate(context());

            assertThat(result.multiplier()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(result.description()).contains("Normal demand").contains("40%");
        }

        @Test
        @DisplayName("No bookings in the period is neutral")
        void noBookings() {
            fleet(10, 0);

            PriceModifier result = strategy.calculate(context());

            assertThat(result.multiplier()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(result.description()).contains("0%");
        }

        @Test
        @DisplayName("Fully booked fleet caps at 100%")
        void fullyBooked() {
            fleet(4, 4);

            assertThat(strategy.calculate(context()).description()).contains("100%");
        }

        @Test
        @DisplayName("One booking on a two-car fleet is 50%, which a single-car count would miss")
        void singleBookingOnSmallFleetTriggersSurcharge() {
            fleet(2, 1);

            PriceModifier result = strategy.calculate(context());

            assertThat(result.multiplier()).isEqualByComparingTo(new BigDecimal("1.10"));
            verify(bookingCalendar, never()).countActiveBookings(any(), any(), any());
        }

        @Test
        @DisplayName("Booked cars outside the bookable fleet do not count")
        void bookedCarsOutsideFleetIgnored() {
            when(carRepository.findBookableCarIds(any())).thenReturn(List.of(1L, 2L, 3L, 4L));
            when(bookingCalendar.findCommittedBookings(eq(START), eq(END))).thenReturn(List.of(
                    new CarBooking(1L, START, END), new CarBooking(98L, START, END), new CarBooking(99L, START, END)));

            PriceModifier result = strategy.calculate(context());

            assertThat(result.multiplier()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(result.description()).contains("25%");
        }

        @Test
        @DisplayName("A short booking inside a long window is averaged per day, not counted as full occupancy")
        void shortBookingInsideLongWindowIsAveragedPerDay() {
            LocalDate longEnd = START.plusDays(29);
            when(carRepository.findBookableCarIds(any())).thenReturn(List.of(1L, 2L, 3L, 4L, 5L));
            when(bookingCalendar.findCommittedBookings(eq(START), eq(longEnd))).thenReturn(List.of(
                    new CarBooking(1L, START, START.plusDays(2)),
                    new CarBooking(2L, START, START.plusDays(2))));
            PricingContext longContext =
                    new PricingContext(1L, new BigDecimal("500"), START, longEnd, START.minusDays(10), 30, 10, "SUV");

            PriceModifier result = strategy.calculate(longContext);

            assertThat(result.multiplier()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(result.description()).contains("4%");
        }

        @Test
        @DisplayName("The occupancy of each day is averaged over the window")
        void occupancyIsAveragedPerDay() {
            when(carRepository.findBookableCarIds(any())).thenReturn(List.of(1L, 2L));
            when(bookingCalendar.findCommittedBookings(eq(START), eq(END))).thenReturn(List.of(
                    new CarBooking(1L, START, START.plusDays(2)),
                    new CarBooking(2L, START, START.plusDays(2))));

            PriceModifier result = strategy.calculate(context());

            assertThat(result.description()).contains("50%");
            assertThat(result.multiplier()).isEqualByComparingTo(new BigDecimal("1.10"));
        }

        @Test
        @DisplayName("The booking being repriced does not count as demand for itself")
        void excludesTheBookingBeingRepriced() {
            when(carRepository.findBookableCarIds(any())).thenReturn(List.of(1L));
            when(bookingCalendar.findCommittedBookings(eq(START), eq(END)))
                    .thenReturn(List.of(new CarBooking(77L, 1L, START, END)));
            PricingContext repricing = new PricingContext(
                    1L, new BigDecimal("500"), START, END, START.minusDays(10), 6, 10, "SUV", 77L);

            PriceModifier result = strategy.calculate(repricing);

            assertThat(result.multiplier()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(result.description()).contains("0%");
            assertThat(strategy.calculate(context()).description()).contains("100%");
        }

        @Test
        @DisplayName("Empty fleet is neutral and does not query bookings")
        void emptyFleet() {
            when(carRepository.findBookableCarIds(any())).thenReturn(List.of());

            PriceModifier result = strategy.calculate(context());

            assertThat(result.multiplier()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(result.description()).contains("0%");
            verify(bookingCalendar, never()).findCommittedBookings(any(), any());
        }
    }

    @Nested
    @DisplayName("Strategy Metadata")
    class StrategyMetadata {

        @Test
        @DisplayName("Should return correct strategy name")
        void shouldReturnCorrectStrategyName() {
            assertThat(strategy.getStrategyName()).isEqualTo("Demand Pricing");
            assertThat(strategy.getCode()).isEqualTo(com.akif.car.api.PricingStrategyCode.DEMAND);
        }

        @Test
        @DisplayName("Should return correct order")
        void shouldReturnCorrectOrder() {
            assertThat(strategy.getOrder()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should be enabled when config says enabled")
        void shouldBeEnabledWhenConfigSaysEnabled() {
            when(config.strategy()).thenReturn(ConfigFixtures.strategies(true));
            assertThat(strategy.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should be disabled when config says disabled")
        void shouldBeDisabledWhenConfigSaysDisabled() {
            when(config.strategy()).thenReturn(ConfigFixtures.strategies(false));
            assertThat(strategy.isEnabled()).isFalse();
        }
    }
}
