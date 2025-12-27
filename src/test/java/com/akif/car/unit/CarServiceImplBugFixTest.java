package com.akif.car.unit;

import com.akif.car.api.CarResponse;
import com.akif.car.domain.Car;
import com.akif.car.domain.enums.CarStatusType;
import com.akif.car.internal.dto.request.CarRequest;
import com.akif.car.internal.mapper.CarMapper;
import com.akif.car.internal.repository.CarRepository;
import com.akif.car.internal.service.CarServiceImpl;
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
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CarServiceImpl Bug Fix Tests")
class CarServiceImplBugFixTest {

    @Mock
    private CarRepository carRepository;

    @Mock
    private CarMapper carMapper;

    @InjectMocks
    private CarServiceImpl carService;

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
                .damagePrice(BigDecimal.ZERO)
                .carStatusType(CarStatusType.AVAILABLE)
                .isDeleted(false)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
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
    @DisplayName("NPE Fix - VIN Comparison (BUG-008)")
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

            when(carRepository.findById(1L)).thenReturn(Optional.of(carWithNullVin));
            when(carRepository.existsByVinNumber("NEWVIN123")).thenReturn(false);
            when(carRepository.save(any(Car.class))).thenReturn(carWithNullVin);
            when(carMapper.toDto(any(Car.class))).thenReturn(testCarResponse);

            assertThatCode(() -> carService.updateCar(1L, updateRequest))
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

            when(carRepository.findById(1L)).thenReturn(Optional.of(testCar));
            when(carRepository.existsByVinNumber(null)).thenReturn(false);
            when(carRepository.save(any(Car.class))).thenReturn(testCar);
            when(carMapper.toDto(any(Car.class))).thenReturn(testCarResponse);

            assertThatCode(() -> carService.updateCar(1L, updateRequest))
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

            when(carRepository.findById(1L)).thenReturn(Optional.of(carWithNullVin));
            when(carRepository.save(any(Car.class))).thenReturn(carWithNullVin);
            when(carMapper.toDto(any(Car.class))).thenReturn(testCarResponse);

            assertThatCode(() -> carService.updateCar(1L, updateRequest))
                    .doesNotThrowAnyException();

            verify(carRepository, never()).existsByVinNumber(any());
        }
    }

    @Nested
    @DisplayName("RestoreCar Return Type Fix (BUG-004)")
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

            CarResponse result = carService.restoreCar(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getLicensePlate()).isEqualTo("34ABC123");
            assertThat(result.getBrand()).isEqualTo("Toyota");

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

            carService.restoreCar(1L);

            assertThat(deletedCar.getIsDeleted()).isFalse();
            verify(carRepository).save(deletedCar);
        }
    }
}
