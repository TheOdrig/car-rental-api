package com.akif.car.unit.pricing.impl;

import com.akif.car.domain.Car;
import com.akif.car.internal.dto.pricing.PriceModifier;
import com.akif.car.internal.dto.pricing.PricingContext;
import com.akif.car.internal.dto.pricing.PricingResult;
import com.akif.car.internal.exception.CarNotFoundException;
import com.akif.car.internal.repository.CarRepository;
import com.akif.car.internal.service.pricing.PricingStrategy;
import com.akif.car.internal.service.pricing.impl.DynamicPricingServiceImpl;
import com.akif.rental.internal.config.PricingConfig;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DynamicPricingServiceImpl Unit Tests")
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class DynamicPricingServiceImplTest {

    @Mock
    private CarRepository carRepository;

    @Mock
    private PricingConfig config;

    @Mock
    private PricingStrategy strategy1;

    @Mock
    private PricingStrategy strategy2;

    @InjectMocks
    private DynamicPricingServiceImpl service;

    private Car testCar;

    @BeforeEach
    void setUp() {
        testCar = Car.builder()
            .id(1L)
            .price(new BigDecimal("500"))
            .bodyType("SUV")
            .build();
    }

    @Nested
    @DisplayName("Price Calculation")
    class PriceCalculation {

        @Test
        @DisplayName("Should calculate price with single strategy")
        void shouldCalculatePriceWithSingleStrategy() {
            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testCar));
            when(config.getMinDailyPrice()).thenReturn(new BigDecimal("100"));
            when(config.getMaxDailyPrice()).thenReturn(new BigDecimal("10000"));
            when(strategy1.isEnabled()).thenReturn(true);
            when(strategy1.getOrder()).thenReturn(1);
            when(strategy1.calculate(any(PricingContext.class)))
                .thenReturn(PriceModifier.discount("Test", new BigDecimal("0.90"), "10% discount"));

            service = new DynamicPricingServiceImpl(List.of(strategy1), carRepository, config);

            PricingResult result = service.calculatePrice(
                1L,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15),
                LocalDate.now()
            );

            assertThat(result).isNotNull();
            assertThat(result.basePrice()).isEqualByComparingTo(new BigDecimal("500"));
            assertThat(result.rentalDays()).isEqualTo(6);
            assertThat(result.combinedMultiplier()).isEqualByComparingTo(new BigDecimal("0.90"));
            assertThat(result.finalPrice()).isEqualByComparingTo(new BigDecimal("2700.00"));
        }

        @Test
        @DisplayName("Should combine multiple strategy modifiers by multiplication")
        void shouldCombineMultipleStrategyModifiers() {
            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testCar));
            when(config.getMinDailyPrice()).thenReturn(new BigDecimal("100"));
            when(config.getMaxDailyPrice()).thenReturn(new BigDecimal("10000"));
            when(strategy1.isEnabled()).thenReturn(true);
            when(strategy1.getOrder()).thenReturn(1);
            when(strategy1.calculate(any(PricingContext.class)))
                .thenReturn(PriceModifier.discount("Strategy1", new BigDecimal("0.90"), "10% discount"));

            when(strategy2.isEnabled()).thenReturn(true);
            when(strategy2.getOrder()).thenReturn(2);
            when(strategy2.calculate(any(PricingContext.class)))
                .thenReturn(PriceModifier.surcharge("Strategy2", new BigDecimal("1.20"), "20% surcharge"));

            service = new DynamicPricingServiceImpl(List.of(strategy1, strategy2), carRepository, config);

            PricingResult result = service.calculatePrice(
                1L,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15),
                LocalDate.now()
            );

            assertThat(result.combinedMultiplier()).isEqualByComparingTo(new BigDecimal("1.08"));
            assertThat(result.finalPrice()).isEqualByComparingTo(new BigDecimal("3240.00"));
        }

        @Test
        @DisplayName("Should skip disabled strategies")
        void shouldSkipDisabledStrategies() {
            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testCar));
            when(config.getMinDailyPrice()).thenReturn(new BigDecimal("100"));
            when(config.getMaxDailyPrice()).thenReturn(new BigDecimal("10000"));
            when(strategy1.isEnabled()).thenReturn(false);
            when(strategy2.isEnabled()).thenReturn(true);
            when(strategy2.getOrder()).thenReturn(2);
            when(strategy2.calculate(any(PricingContext.class)))
                .thenReturn(PriceModifier.discount("Strategy2", new BigDecimal("0.85"), "15% discount"));

            service = new DynamicPricingServiceImpl(List.of(strategy1, strategy2), carRepository, config);

            PricingResult result = service.calculatePrice(
                1L,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15),
                LocalDate.now()
            );

            assertThat(result.appliedModifiers()).hasSize(1);
            assertThat(result.combinedMultiplier()).isEqualByComparingTo(new BigDecimal("0.85"));
        }

        @Test
        @DisplayName("Should throw exception when car not found")
        void shouldThrowExceptionWhenCarNotFound() {
            when(carRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.calculatePrice(
                999L,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15),
                LocalDate.now()
            )).isInstanceOf(CarNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Price Cap Enforcement")
    class PriceCapEnforcement {

        @Test
        @DisplayName("Should enforce minimum daily price cap")
        void shouldEnforceMinimumDailyPriceCap() {
            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testCar));
            when(config.getMinDailyPrice()).thenReturn(new BigDecimal("100"));
            when(config.getMaxDailyPrice()).thenReturn(new BigDecimal("10000"));

            when(strategy1.isEnabled()).thenReturn(true);
            when(strategy1.getOrder()).thenReturn(1);
            when(strategy1.calculate(any(PricingContext.class)))
                .thenReturn(PriceModifier.discount("Test", new BigDecimal("0.10"), "90% discount"));

            service = new DynamicPricingServiceImpl(List.of(strategy1), carRepository, config);

            PricingResult result = service.calculatePrice(
                1L,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15),
                LocalDate.now()
            );

            assertThat(result.finalPrice()).isEqualByComparingTo(new BigDecimal("600.00"));
        }
    }

    @Nested
    @DisplayName("Preview Price")
    class PreviewPrice {

        @Test
        @DisplayName("Should calculate preview price using current date as booking date")
        void shouldCalculatePreviewPriceUsingCurrentDate() {
            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testCar));
            when(config.getMinDailyPrice()).thenReturn(new BigDecimal("100"));
            when(config.getMaxDailyPrice()).thenReturn(new BigDecimal("10000"));
            when(strategy1.isEnabled()).thenReturn(true);
            when(strategy1.getOrder()).thenReturn(1);
            when(strategy1.calculate(any(PricingContext.class)))
                .thenReturn(PriceModifier.neutral("Test", "No discount"));

            service = new DynamicPricingServiceImpl(List.of(strategy1), carRepository, config);

            PricingResult result = service.previewPrice(
                1L,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15)
            );

            assertThat(result).isNotNull();
            assertThat(result.basePrice()).isEqualByComparingTo(new BigDecimal("500"));
        }
    }
}
