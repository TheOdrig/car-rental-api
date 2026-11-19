package com.akif.car.unit;

import com.akif.car.api.CarResponse;
import com.akif.car.domain.Car;
import com.akif.car.api.CarStatusType;
import com.akif.car.internal.dto.request.CarRequest;
import com.akif.car.internal.mapper.CarMapper;
import com.akif.car.internal.repository.CarRepository;
import com.akif.car.internal.service.CarCommandService;
import com.akif.car.internal.service.CarLookup;
import com.akif.car.internal.service.CarQueryService;
import com.akif.car.internal.service.CarStatusService;
import com.akif.shared.enums.CurrencyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.time.Clock;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Car update with missing VINs and restore")
class CarNullVinAndRestoreTest {

    @Mock
    private CarRepository carRepository;

    @Mock
    private CarMapper carMapper;

    @Spy
    private Clock clock = Clock.systemDefaultZone();

    private CarQueryService queryService;
    private CarCommandService commandService;
    private CarStatusService statusService;

    @BeforeEach
    void wireService() {
        CarLookup carLookup = new CarLookup(carRepository);
        queryService = new CarQueryService(clock, carRepository, carMapper, carLookup);
        commandService = new CarCommandService(clock, carRepository, carMapper, carLookup);
        statusService = new CarStatusService(clock, carRepository, carMapper, carLookup);
    }

    private Car testCar;
    private CarResponse testCarResponse;

    @BeforeEach
    void setUp() {
        testCar = Car.builder()
                .id(1L)
                .licensePlate("34ABC123")
                .vinNumber("VIN123456789")
                .brand("Toyota")
                .model("Corolla")
                .productionYear(2020)
                .price(new BigDecimal("250000"))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .isDeleted(false)
                .createTime(Instant.now())
                .updateTime(Instant.now())
                .build();

        testCarResponse = CarResponse.builder()
                .id(1L)
                .licensePlate("34ABC123")
                .vinNumber("VIN123456789")
                .brand("Toyota")
                .model("Corolla")
                .productionYear(2020)
                .price(new BigDecimal("250000"))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .build();
    }

    @Nested
    @DisplayName("NPE Fix - VIN Comparison")
    class VinComparisonNpeFix {

        @Test
        @DisplayName("Should not throw NPE when existing car VIN is null")
        void shouldNotThrowNpeWhenExistingCarVinIsNull() {
            Car carWithNullVin = Car.builder()
                    .id(1L)
                    .licensePlate("34ABC123")
                    .vinNumber(null)
                    .brand("Toyota")
                    .model("Corolla")
                    .productionYear(2020)
                    .price(new BigDecimal("250000"))
                    .currencyType(CurrencyType.TRY)
                    .carStatusType(CarStatusType.AVAILABLE)
                    .build();

            CarRequest updateRequest = CarRequest.builder()
                    .licensePlate("34ABC123")
                    .vinNumber("NEWVIN123")
                    .brand("Toyota")
                    .model("Corolla")
                    .productionYear(2020)
                    .price(new BigDecimal("250000"))
                    .currencyType(CurrencyType.TRY)
                    .carStatusType(CarStatusType.AVAILABLE)
                    .build();

            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(carWithNullVin));
            when(carRepository.existsByVinNumber("NEWVIN123")).thenReturn(false);
            when(carRepository.save(any(Car.class))).thenReturn(carWithNullVin);
            when(carMapper.toDto(any(Car.class))).thenReturn(testCarResponse);

            assertThatCode(() -> commandService.updateCar(1L, updateRequest))
                    .doesNotThrowAnyException();

            verify(carRepository).existsByVinNumber("NEWVIN123");
        }

        @Test
        @DisplayName("Should not throw NPE when request VIN is null")
        void shouldNotThrowNpeWhenRequestVinIsNull() {
            CarRequest updateRequest = CarRequest.builder()
                    .licensePlate("34ABC123")
                    .vinNumber(null)
                    .brand("Toyota")
                    .model("Corolla")
                    .productionYear(2020)
                    .price(new BigDecimal("250000"))
                    .currencyType(CurrencyType.TRY)
                    .carStatusType(CarStatusType.AVAILABLE)
                    .build();

            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testCar));
            when(carRepository.save(any(Car.class))).thenReturn(testCar);
            when(carMapper.toDto(any(Car.class))).thenReturn(testCarResponse);

            assertThatCode(() -> commandService.updateCar(1L, updateRequest))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should not throw NPE when both VINs are null")
        void shouldNotThrowNpeWhenBothVinsAreNull() {
            Car carWithNullVin = Car.builder()
                    .id(1L)
                    .licensePlate("34ABC123")
                    .vinNumber(null)
                    .brand("Toyota")
                    .model("Corolla")
                    .productionYear(2020)
                    .price(new BigDecimal("250000"))
                    .currencyType(CurrencyType.TRY)
                    .carStatusType(CarStatusType.AVAILABLE)
                    .build();

            CarRequest updateRequest = CarRequest.builder()
                    .licensePlate("34ABC123")
                    .vinNumber(null)
                    .brand("Toyota")
                    .model("Corolla")
                    .productionYear(2020)
                    .price(new BigDecimal("250000"))
                    .currencyType(CurrencyType.TRY)
                    .carStatusType(CarStatusType.AVAILABLE)
                    .build();

            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(carWithNullVin));
            when(carRepository.save(any(Car.class))).thenReturn(carWithNullVin);
            when(carMapper.toDto(any(Car.class))).thenReturn(testCarResponse);

            assertThatCode(() -> commandService.updateCar(1L, updateRequest))
                    .doesNotThrowAnyException();

            verify(carRepository, never()).existsByVinNumber(any());
        }
    }

    @Nested
    @DisplayName("RestoreCar Return Type Fix")
    class RestoreCarReturnTypeFix {

        @Test
        @DisplayName("Should return CarResponse when restoring car")
        void shouldReturnCarResponseWhenRestoringCar() {
            Car deletedCar = Car.builder()
                    .id(1L)
                    .licensePlate("34ABC123")
                    .vinNumber("VIN123456789")
                    .brand("Toyota")
                    .model("Corolla")
                    .productionYear(2020)
                    .price(new BigDecimal("250000"))
                    .currencyType(CurrencyType.TRY)
                    .carStatusType(CarStatusType.AVAILABLE)
                    .isDeleted(true)
                    .build();

            when(carRepository.findById(1L)).thenReturn(Optional.of(deletedCar));
            when(carRepository.save(any(Car.class))).thenReturn(deletedCar);
            when(carMapper.toDto(deletedCar)).thenReturn(testCarResponse);

            CarResponse result = commandService.restoreCar(1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.licensePlate()).isEqualTo("34ABC123");
            assertThat(result.brand()).isEqualTo("Toyota");

            verify(carRepository).findById(1L);
            verify(carRepository).save(deletedCar);
            verify(carMapper).toDto(deletedCar);
        }

        @Test
        @DisplayName("Should restore car and set isDeleted to false")
        void shouldRestoreCarAndSetIsDeletedToFalse() {
            Car deletedCar = Car.builder()
                    .id(1L)
                    .licensePlate("34ABC123")
                    .brand("Toyota")
                    .model("Corolla")
                    .productionYear(2020)
                    .price(new BigDecimal("250000"))
                    .currencyType(CurrencyType.TRY)
                    .carStatusType(CarStatusType.AVAILABLE)
                    .isDeleted(true)
                    .build();

            when(carRepository.findById(1L)).thenReturn(Optional.of(deletedCar));
            when(carRepository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(carMapper.toDto(any(Car.class))).thenReturn(testCarResponse);

            commandService.restoreCar(1L);

            assertThat(deletedCar.getIsDeleted()).isFalse();
            verify(carRepository).save(deletedCar);
        }
    }
}
