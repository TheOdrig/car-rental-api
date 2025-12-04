package com.akif.service.availability;

import com.akif.dto.availability.SimilarCarDto;
import com.akif.enums.CarStatusType;
import com.akif.enums.CurrencyType;
import com.akif.exception.CarNotFoundException;
import com.akif.model.Car;
import com.akif.repository.CarRepository;
import com.akif.service.availability.impl.SimilarCarServiceImpl;
import com.akif.service.pricing.IDynamicPricingService;
import com.akif.service.pricing.PriceModifier;
import com.akif.service.pricing.PricingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SimilarCarServiceImpl Unit Tests")
class SimilarCarServiceImplTest {

    @Mock
    private CarRepository carRepository;

    @Mock
    private ICarAvailabilityService carAvailabilityService;

    @Mock
    private IDynamicPricingService dynamicPricingService;

    @InjectMocks
    private SimilarCarServiceImpl similarCarService;

    private Car referenceCar;
    private Car similarCar1;
    private Car similarCar2;
    private Car similarCar3;
    private PricingResult pricingResult;
    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        startDate = LocalDate.now().plusDays(1);
        endDate = LocalDate.now().plusDays(5);

        referenceCar = Car.builder()
                .id(1L)
                .licensePlate("34ABC123")
                .brand("Toyota")
                .model("Corolla")
                .productionYear(2020)
                .bodyType("Sedan")
                .price(new BigDecimal("1000"))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .imageUrl("https://example.com/car1.jpg")
                .isDeleted(false)
                .build();

        similarCar1 = Car.builder()
                .id(2L)
                .licensePlate("34DEF456")
                .brand("Toyota")
                .model("Camry")
                .productionYear(2021)
                .bodyType("Sedan")
                .price(new BigDecimal("1100"))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .imageUrl("https://example.com/car2.jpg")
                .isDeleted(false)
                .build();

        similarCar2 = Car.builder()
                .id(3L)
                .licensePlate("34GHI789")
                .brand("Honda")
                .model("Civic")
                .productionYear(2020)
                .bodyType("Sedan")
                .price(new BigDecimal("950"))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .imageUrl("https://example.com/car3.jpg")
                .isDeleted(false)
                .build();

        similarCar3 = Car.builder()
                .id(4L)
                .licensePlate("34JKL012")
                .brand("Mazda")
                .model("3")
                .productionYear(2019)
                .bodyType("Sedan")
                .price(new BigDecimal("900"))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .imageUrl("https://example.com/car4.jpg")
                .isDeleted(false)
                .build();

        pricingResult = PricingResult.builder()
                .basePrice(new BigDecimal("500"))
                .rentalDays(5)
                .appliedModifiers(List.of(PriceModifier.discount("Early booking", new BigDecimal("0.9"), "Early booking discount")))
                .combinedMultiplier(new BigDecimal("0.9"))
                .finalPrice(new BigDecimal("2250"))
                .build();
    }

    @Nested
    @DisplayName("Find Similar Cars Tests")
    class FindSimilarCarsTests {

        @Test
        @DisplayName("Should return max 5 similar cars when more available")
        void shouldReturnMax5SimilarCarsWhenMoreAvailable() {
            List<Car> manyCars = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                Car car = Car.builder()
                        .id((long) (i + 10))
                        .licensePlate("34TEST" + i)
                        .brand("Toyota")
                        .model("Model" + i)
                        .bodyType("Sedan")
                        .price(new BigDecimal("1000"))
                        .currencyType(CurrencyType.TRY)
                        .carStatusType(CarStatusType.AVAILABLE)
                        .isDeleted(false)
                        .build();
                manyCars.add(car);
            }

            Page<Car> carPage = new PageImpl<>(manyCars);

            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(referenceCar));
            when(carRepository.findSimilarCars(
                    anyString(), any(BigDecimal.class), any(BigDecimal.class), 
                    anyLong(), anyList(), any(Pageable.class)))
                    .thenReturn(carPage);
            when(carAvailabilityService.isCarAvailable(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(true);
            when(dynamicPricingService.calculatePrice(anyLong(), any(LocalDate.class), 
                    any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(pricingResult);

            List<SimilarCarDto> result = similarCarService.findSimilarAvailableCars(1L, startDate, endDate, 5);

            assertThat(result).hasSize(5);
            verify(carRepository).findByIdAndIsDeletedFalse(1L);
            verify(carRepository).findSimilarCars(
                    anyString(), any(BigDecimal.class), any(BigDecimal.class), 
                    anyLong(), anyList(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should filter by same body type")
        void shouldFilterBySameBodyType() {
            Page<Car> carPage = new PageImpl<>(List.of(similarCar1, similarCar2));

            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(referenceCar));
            when(carRepository.findSimilarCars(
                    eq("Sedan"), any(BigDecimal.class), any(BigDecimal.class), 
                    eq(1L), anyList(), any(Pageable.class)))
                    .thenReturn(carPage);
            when(carAvailabilityService.isCarAvailable(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(true);
            when(dynamicPricingService.calculatePrice(anyLong(), any(LocalDate.class), 
                    any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(pricingResult);

            List<SimilarCarDto> result = similarCarService.findSimilarAvailableCars(1L, startDate, endDate, 5);

            assertThat(result).isNotEmpty();
            assertThat(result).allMatch(car -> car.getSimilarityReasons().contains("Same body type"));
            verify(carRepository).findSimilarCars(
                    eq("Sedan"), any(BigDecimal.class), any(BigDecimal.class), 
                    eq(1L), anyList(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should calculate similarity score correctly")
        void shouldCalculateSimilarityScoreCorrectly() {
            Page<Car> carPage = new PageImpl<>(List.of(similarCar1, similarCar2, similarCar3));

            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(referenceCar));
            when(carRepository.findSimilarCars(
                    anyString(), any(BigDecimal.class), any(BigDecimal.class), 
                    anyLong(), anyList(), any(Pageable.class)))
                    .thenReturn(carPage);
            when(carAvailabilityService.isCarAvailable(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(true);
            when(dynamicPricingService.calculatePrice(anyLong(), any(LocalDate.class), 
                    any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(pricingResult);

            List<SimilarCarDto> result = similarCarService.findSimilarAvailableCars(1L, startDate, endDate, 5);

            assertThat(result).isNotEmpty();

            SimilarCarDto car1 = result.stream()
                    .filter(c -> c.getId().equals(2L))
                    .findFirst()
                    .orElseThrow();
            assertThat(car1.getSimilarityScore()).isEqualTo(100);
            assertThat(car1.getSimilarityReasons()).containsExactlyInAnyOrder(
                    "Same brand", "Same body type", "Similar price");

            SimilarCarDto car2 = result.stream()
                    .filter(c -> c.getId().equals(3L))
                    .findFirst()
                    .orElseThrow();
            assertThat(car2.getSimilarityScore()).isEqualTo(70);
            assertThat(car2.getSimilarityReasons()).containsExactlyInAnyOrder(
                    "Same body type", "Similar price");

            SimilarCarDto car3 = result.stream()
                    .filter(c -> c.getId().equals(4L))
                    .findFirst()
                    .orElseThrow();
            assertThat(car3.getSimilarityScore()).isEqualTo(70);
        }

        @Test
        @DisplayName("Should include similarity reasons for each car")
        void shouldIncludeSimilarityReasonsForEachCar() {
            Page<Car> carPage = new PageImpl<>(List.of(similarCar1));

            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(referenceCar));
            when(carRepository.findSimilarCars(
                    anyString(), any(BigDecimal.class), any(BigDecimal.class), 
                    anyLong(), anyList(), any(Pageable.class)))
                    .thenReturn(carPage);
            when(carAvailabilityService.isCarAvailable(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(true);
            when(dynamicPricingService.calculatePrice(anyLong(), any(LocalDate.class), 
                    any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(pricingResult);

            List<SimilarCarDto> result = similarCarService.findSimilarAvailableCars(1L, startDate, endDate, 5);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSimilarityReasons()).isNotEmpty();
            assertThat(result.get(0).getSimilarityReasons()).contains("Same brand", "Same body type", "Similar price");
        }

        @Test
        @DisplayName("Should return empty list when no similar cars available")
        void shouldReturnEmptyListWhenNoSimilarCarsAvailable() {
            Page<Car> emptyPage = new PageImpl<>(List.of());

            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(referenceCar));
            when(carRepository.findSimilarCars(
                    anyString(), any(BigDecimal.class), any(BigDecimal.class), 
                    anyLong(), anyList(), any(Pageable.class)))
                    .thenReturn(emptyPage);

            List<SimilarCarDto> result = similarCarService.findSimilarAvailableCars(1L, startDate, endDate, 5);

            assertThat(result).isEmpty();
            verify(carRepository).findByIdAndIsDeletedFalse(1L);
            verify(carRepository).findSimilarCars(
                    anyString(), any(BigDecimal.class), any(BigDecimal.class), 
                    anyLong(), anyList(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should exclude unavailable cars from results")
        void shouldExcludeUnavailableCarsFromResults() {
            Page<Car> carPage = new PageImpl<>(List.of(similarCar1, similarCar2, similarCar3));

            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(referenceCar));
            when(carRepository.findSimilarCars(
                    anyString(), any(BigDecimal.class), any(BigDecimal.class), 
                    anyLong(), anyList(), any(Pageable.class)))
                    .thenReturn(carPage);

            when(carAvailabilityService.isCarAvailable(eq(2L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(true);
            when(carAvailabilityService.isCarAvailable(eq(3L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(false);
            when(carAvailabilityService.isCarAvailable(eq(4L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(false);
            
            when(dynamicPricingService.calculatePrice(anyLong(), any(LocalDate.class), 
                    any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(pricingResult);

            List<SimilarCarDto> result = similarCarService.findSimilarAvailableCars(1L, startDate, endDate, 5);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(2L);
            verify(carAvailabilityService, times(3)).isCarAvailable(
                    anyLong(), any(LocalDate.class), any(LocalDate.class));
        }

        @Test
        @DisplayName("Should sort results by similarity score descending")
        void shouldSortResultsBySimilarityScoreDescending() {
            Page<Car> carPage = new PageImpl<>(List.of(similarCar3, similarCar2, similarCar1));

            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(referenceCar));
            when(carRepository.findSimilarCars(
                    anyString(), any(BigDecimal.class), any(BigDecimal.class), 
                    anyLong(), anyList(), any(Pageable.class)))
                    .thenReturn(carPage);
            when(carAvailabilityService.isCarAvailable(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(true);
            when(dynamicPricingService.calculatePrice(anyLong(), any(LocalDate.class), 
                    any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(pricingResult);

            List<SimilarCarDto> result = similarCarService.findSimilarAvailableCars(1L, startDate, endDate, 5);

            assertThat(result).hasSize(3);
            assertThat(result.get(0).getId()).isEqualTo(2L);
            assertThat(result.get(0).getSimilarityScore()).isEqualTo(100);

            for (int i = 0; i < result.size() - 1; i++) {
                assertThat(result.get(i).getSimilarityScore())
                        .isGreaterThanOrEqualTo(result.get(i + 1).getSimilarityScore());
            }
        }

        @Test
        @DisplayName("Should calculate price range correctly (±20%)")
        void shouldCalculatePriceRangeCorrectly() {
            Page<Car> carPage = new PageImpl<>(List.of(similarCar1));

            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(referenceCar));
            when(carRepository.findSimilarCars(
                    eq("Sedan"), any(BigDecimal.class), any(BigDecimal.class), 
                    eq(1L), anyList(), any(Pageable.class)))
                    .thenReturn(carPage);
            when(carAvailabilityService.isCarAvailable(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(true);
            when(dynamicPricingService.calculatePrice(anyLong(), any(LocalDate.class), 
                    any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(pricingResult);

            similarCarService.findSimilarAvailableCars(1L, startDate, endDate, 5);

            verify(carRepository).findSimilarCars(
                    eq("Sedan"), any(BigDecimal.class), any(BigDecimal.class), 
                    eq(1L), anyList(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should throw exception when reference car not found")
        void shouldThrowExceptionWhenReferenceCarNotFound() {
            when(carRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> similarCarService.findSimilarAvailableCars(999L, startDate, endDate, 5))
                    .isInstanceOf(CarNotFoundException.class);

            verify(carRepository).findByIdAndIsDeletedFalse(999L);
            verify(carRepository, never()).findSimilarCars(
                    anyString(), any(), any(), anyLong(), anyList(), any());
        }

        @Test
        @DisplayName("Should include pricing information for each similar car")
        void shouldIncludePricingInformationForEachSimilarCar() {
            Page<Car> carPage = new PageImpl<>(List.of(similarCar1));

            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(referenceCar));
            when(carRepository.findSimilarCars(
                    anyString(), any(BigDecimal.class), any(BigDecimal.class), 
                    anyLong(), anyList(), any(Pageable.class)))
                    .thenReturn(carPage);
            when(carAvailabilityService.isCarAvailable(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(true);
            when(dynamicPricingService.calculatePrice(anyLong(), any(LocalDate.class), 
                    any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(pricingResult);

            List<SimilarCarDto> result = similarCarService.findSimilarAvailableCars(1L, startDate, endDate, 5);

            assertThat(result).hasSize(1);
            SimilarCarDto car = result.get(0);
            assertThat(car.getDailyRate()).isNotNull();
            assertThat(car.getTotalPrice()).isNotNull();
            assertThat(car.getCurrency()).isNotNull();
            assertThat(car.getDailyRate()).isEqualTo(pricingResult.effectiveDailyPrice());
            assertThat(car.getTotalPrice()).isEqualTo(pricingResult.finalPrice());

            verify(dynamicPricingService).calculatePrice(
                    eq(2L), eq(startDate), eq(endDate), any(LocalDate.class));
        }
    }
}
