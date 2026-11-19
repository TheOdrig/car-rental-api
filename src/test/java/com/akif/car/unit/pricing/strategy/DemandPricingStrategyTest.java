package com.akif.car.unit.pricing.strategy;

import com.akif.car.api.CarBookingCalendar;
import com.akif.car.api.PriceModifier;
import com.akif.car.internal.config.PricingConfig;
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
import java.util.Set;
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

    private static final LocalDate START = LocalDate.now().plusDays(10);
    private static final LocalDate END = LocalDate.now().plusDays(15);

    @Mock
    private PricingConfig config;

    @Mock
    private CarBookingCalendar bookingCalendar;

    @Mock
    private CarRepository carRepository;

    @InjectMocks
    private DemandPricingStrategy strategy;

    private PricingConfig.StrategyConfig strategyConfig;

    @BeforeEach
    void setUp() {
        PricingConfig.DemandConfig demandConfig = new PricingConfig.DemandConfig();
        strategyConfig = new PricingConfig.StrategyConfig();

        PricingConfig.DemandTier highTier = new PricingConfig.DemandTier();
        highTier.setThreshold(80);
        highTier.setMultiplier(new BigDecimal("1.20"));

        PricingConfig.DemandTier moderateTier = new PricingConfig.DemandTier();
        moderateTier.setThreshold(50);
        moderateTier.setMultiplier(new BigDecimal("1.10"));

        demandConfig.setHigh(highTier);
        demandConfig.setModerate(moderateTier);

        lenient().when(config.getDemand()).thenReturn(demandConfig);
        lenient().when(config.getStrategy()).thenReturn(strategyConfig);
    }

    private PricingContext context() {
        return new PricingContext(1L, new BigDecimal("500"), START, END, LocalDate.now(), 5, 10, "SUV");
    }

    private void fleet(int size, int booked) {
        List<Long> ids = LongStream.rangeClosed(1, size).boxed().collect(Collectors.toList());
        when(carRepository.findBookableCarIds(any())).thenReturn(ids);
        Set<Long> bookedIds = LongStream.rangeClosed(1, booked).boxed().collect(Collectors.toSet());
        when(bookingCalendar.findBookedCarIds(eq(START), eq(END))).thenReturn(bookedIds);
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
            when(bookingCalendar.findBookedCarIds(eq(START), eq(END))).thenReturn(Set.of(1L, 98L, 99L));

            PriceModifier result = strategy.calculate(context());

            assertThat(result.multiplier()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(result.description()).contains("25%");
        }

        @Test
        @DisplayName("Empty fleet is neutral and does not query bookings")
        void emptyFleet() {
            when(carRepository.findBookableCarIds(any())).thenReturn(List.of());

            PriceModifier result = strategy.calculate(context());

            assertThat(result.multiplier()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(result.description()).contains("0%");
            verify(bookingCalendar, never()).findBookedCarIds(any(), any());
        }
    }

    @Nested
    @DisplayName("Strategy Metadata")
    class StrategyMetadata {

        @Test
        @DisplayName("Should return correct strategy name")
        void shouldReturnCorrectStrategyName() {
            assertThat(strategy.getStrategyName()).isEqualTo("Demand Pricing");
        }

        @Test
        @DisplayName("Should return correct order")
        void shouldReturnCorrectOrder() {
            assertThat(strategy.getOrder()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should be enabled when config says enabled")
        void shouldBeEnabledWhenConfigSaysEnabled() {
            strategyConfig.setDemandEnabled(true);
            assertThat(strategy.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should be disabled when config says disabled")
        void shouldBeDisabledWhenConfigSaysDisabled() {
            strategyConfig.setDemandEnabled(false);
            assertThat(strategy.isEnabled()).isFalse();
        }
    }
}
