package com.akif.service.pricing.strategy;

import com.akif.config.PricingConfig;
import com.akif.repository.RentalRepository;
import com.akif.service.pricing.PriceModifier;
import com.akif.service.pricing.PricingContext;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DemandPricingStrategy Unit Tests")
class DemandPricingStrategyTest {

    @Mock
    private PricingConfig config;

    @Mock
    private RentalRepository rentalRepository;

    @InjectMocks
    private DemandPricingStrategy strategy;

    private PricingConfig.DemandConfig demandConfig;
    private PricingConfig.StrategyConfig strategyConfig;

    @BeforeEach
    void setUp() {
        demandConfig = new PricingConfig.DemandConfig();
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

    @Nested
    @DisplayName("High Demand Pricing")
    class HighDemandPricing {

        @Test
        @DisplayName("Should apply high demand surcharge when occupancy > 80%")
        void shouldApplyHighDemandSurchargeWhenOccupancyAbove80() {
            PricingContext context = new PricingContext(
                1L,
                new BigDecimal("500"),
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15),
                LocalDate.now(),
                5,
                10,
                "SUV"
            );

            when(rentalRepository.countOverlappingRentals(
                eq(1L),
                any(LocalDate.class),
                any(LocalDate.class)
            )).thenReturn(9L);

            PriceModifier result = strategy.calculate(context);

            assertThat(result.multiplier()).isEqualByComparingTo(new BigDecimal("1.20"));
            assertThat(result.isDiscount()).isFalse();
            assertThat(result.strategyName()).isEqualTo("Demand Pricing");
            assertThat(result.description()).contains("High demand");
            assertThat(result.description()).contains("90%");
        }

        @Test
        @DisplayName("Should apply high demand surcharge when occupancy exactly 81%")
        void shouldApplyHighDemandSurchargeWhenOccupancyExactly81() {
            PricingContext context = new PricingContext(
                1L,
                new BigDecimal("500"),
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15),
                LocalDate.now(),
                5,
                10,
                "SUV"
            );

            when(rentalRepository.countOverlappingRentals(
                eq(1L),
                any(LocalDate.class),
                any(LocalDate.class)
            )).thenReturn(9L);

            PriceModifier result = strategy.calculate(context);

            assertThat(result.multiplier()).isEqualByComparingTo(new BigDecimal("1.20"));
        }
    }

    @Nested
    @DisplayName("Moderate Demand Pricing")
    class ModerateDemandPricing {

        @Test
        @DisplayName("Should apply moderate demand surcharge when occupancy between 50-80%")
        void shouldApplyModerateDemandSurchargeWhenOccupancyBetween50And80() {
            PricingContext context = new PricingContext(
                1L,
                new BigDecimal("500"),
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15),
                LocalDate.now(),
                5,
                10,
                "SUV"
            );

            when(rentalRepository.countOverlappingRentals(
                eq(1L),
                any(LocalDate.class),
                any(LocalDate.class)
            )).thenReturn(6L);

            PriceModifier result = strategy.calculate(context);

            assertThat(result.multiplier()).isEqualByComparingTo(new BigDecimal("1.10"));
            assertThat(result.isDiscount()).isFalse();
            assertThat(result.description()).contains("Moderate demand");
            assertThat(result.description()).contains("60%");
        }

        @Test
        @DisplayName("Should apply moderate demand surcharge when occupancy exactly 50%")
        void shouldApplyModerateDemandSurchargeWhenOccupancyExactly50() {
            PricingContext context = new PricingContext(
                1L,
                new BigDecimal("500"),
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15),
                LocalDate.now(),
                5,
                10,
                "SUV"
            );

            when(rentalRepository.countOverlappingRentals(
                eq(1L),
                any(LocalDate.class),
                any(LocalDate.class)
            )).thenReturn(5L);

            PriceModifier result = strategy.calculate(context);

            assertThat(result.multiplier()).isEqualByComparingTo(new BigDecimal("1.10"));
        }

        @Test
        @DisplayName("Should apply moderate demand surcharge when occupancy exactly 80%")
        void shouldApplyModerateDemandSurchargeWhenOccupancyExactly80() {
            PricingContext context = new PricingContext(
                1L,
                new BigDecimal("500"),
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15),
                LocalDate.now(),
                5,
                10,
                "SUV"
            );

            when(rentalRepository.countOverlappingRentals(
                eq(1L),
                any(LocalDate.class),
                any(LocalDate.class)
            )).thenReturn(8L);

            PriceModifier result = strategy.calculate(context);

            assertThat(result.multiplier()).isEqualByComparingTo(new BigDecimal("1.10"));
        }
    }

    @Nested
    @DisplayName("Normal Demand Pricing")
    class NormalDemandPricing {

        @Test
        @DisplayName("Should apply no surcharge when occupancy < 50%")
        void shouldApplyNoSurchargeWhenOccupancyBelow50() {
            PricingContext context = new PricingContext(
                1L,
                new BigDecimal("500"),
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15),
                LocalDate.now(),
                5,
                10,
                "SUV"
            );

            when(rentalRepository.countOverlappingRentals(
                eq(1L),
                any(LocalDate.class),
                any(LocalDate.class)
            )).thenReturn(3L);

            PriceModifier result = strategy.calculate(context);

            assertThat(result.multiplier()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(result.isDiscount()).isFalse();
            assertThat(result.description()).contains("Normal demand");
            assertThat(result.description()).contains("30%");
        }

        @Test
        @DisplayName("Should apply no surcharge when no overlapping rentals")
        void shouldApplyNoSurchargeWhenNoOverlappingRentals() {
            PricingContext context = new PricingContext(
                1L,
                new BigDecimal("500"),
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15),
                LocalDate.now(),
                5,
                10,
                "SUV"
            );

            when(rentalRepository.countOverlappingRentals(
                eq(1L),
                any(LocalDate.class),
                any(LocalDate.class)
            )).thenReturn(0L);

            PriceModifier result = strategy.calculate(context);

            assertThat(result.multiplier()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(result.description()).contains("Normal demand");
            assertThat(result.description()).contains("0%");
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
