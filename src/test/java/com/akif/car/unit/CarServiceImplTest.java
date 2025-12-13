package com.akif.car.unit;

import com.akif.car.api.CarResponse;
import com.akif.car.domain.Car;
import com.akif.car.domain.enums.CarStatusType;
import com.akif.car.internal.dto.request.CarRequest;
import com.akif.car.internal.dto.request.CarStatusUpdateRequest;
import com.akif.car.internal.exception.CarAlreadyExistsException;
import com.akif.car.internal.exception.CarNotFoundException;
import com.akif.car.internal.mapper.CarMapper;
import com.akif.car.internal.repository.CarRepository;
import com.akif.car.internal.service.CarServiceImpl;
import com.akif.shared.enums.CurrencyType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("CarServiceImpl Unit Tests")
class CarServiceImplTest {

    @Mock
    private CarRepository carRepository;

    @Mock
    private CarMapper carMapper;

    @InjectMocks
    private CarServiceImpl carService;

    private Car testCar1;
    private Car testCar2;
    private CarRequest testCarRequest;
    private CarResponse testCarResponse;


    @BeforeEach
    void setUp() {

        testCar1 = Car.builder()
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
                .isFeatured(true)
                .isTestDriveAvailable(true)
                .viewCount(10L)
                .likeCount(5L)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        testCar2 = Car.builder()
                .id(2L)
                .licensePlate("06XYZ789")
                .vinNumber("VIN987654321")
                .brand("Honda")
                .model("Civic")
                .productionYear(2021)
                .price(new BigDecimal("300000"))
                .currencyType(CurrencyType.TRY)
                .damagePrice(new BigDecimal("5000"))
                .carStatusType(CarStatusType.AVAILABLE)
                .isFeatured(false)
                .isTestDriveAvailable(false)
                .viewCount(5L)
                .likeCount(2L)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        testCarRequest = CarRequest.builder()
                .licensePlate("35DEF456")
                .vinNumber("VIN456789123")
                .brand("BMW")
                .model("X5")
                .productionYear(2019)
                .price(new BigDecimal("1500000"))
                .currencyType(CurrencyType.TRY)
                .damagePrice(BigDecimal.ZERO)
                .carStatusType(CarStatusType.SOLD)
                .isFeatured(true)
                .isTestDriveAvailable(false)
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
                .damagePrice(BigDecimal.ZERO)
                .carStatusType(CarStatusType.AVAILABLE)
                .isFeatured(true)
                .isTestDriveAvailable(true)
                .viewCount(10L)
                .likeCount(5L)
                .build();
    }


    @Nested
    @DisplayName("Create Car Operations")
    class CreateCarOperations {

        @Test
        @DisplayName("Should create car successfully when car does not exist")
        void shouldCreateCarSuccessfullyWhenCarDoesNotExist() {

            when(carRepository.existsByLicensePlate(testCarRequest.getLicensePlate())).thenReturn(false);
            when(carRepository.existsByVinNumber(testCarRequest.getVinNumber())).thenReturn(false);
            when(carMapper.toEntity(testCarRequest)).thenReturn(testCar1);
            when(carRepository.save(testCar1)).thenReturn(testCar1);
            when(carMapper.toDto(testCar1)).thenReturn(testCarResponse);

            CarResponse result = carService.createCar(testCarRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getLicensePlate()).isEqualTo("34ABC123");
            assertThat(result.getBrand()).isEqualTo("Toyota");

            verify(carRepository).existsByLicensePlate(testCarRequest.getLicensePlate());
            verify(carRepository).existsByVinNumber(testCarRequest.getVinNumber());
            verify(carMapper).toEntity(testCarRequest);
            verify(carRepository).save(testCar1);
            verify(carMapper).toDto(testCar1);
        }

        @Test
        @DisplayName("Should throw exception when car with same license plate already exists")
        void shouldThrowExceptionWhenCarWithSameLicensePlateAlreadyExists() {

            when(carRepository.existsByLicensePlate(testCarRequest.getLicensePlate())).thenReturn(true);

            assertThatThrownBy(() -> carService.createCar(testCarRequest))
                    .isInstanceOf(CarAlreadyExistsException.class)
                    .hasMessageContaining("Car already exists with license plate: " + testCarRequest.getLicensePlate());

            verify(carRepository).existsByLicensePlate(testCarRequest.getLicensePlate());
            verify(carRepository, never()).existsByVinNumber(any());
            verify(carMapper, never()).toEntity(any());
            verify(carRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when car with same VIN already exists")
        void shouldThrowExceptionWhenCarWithSameVinAlreadyExists() {

            when(carRepository.existsByLicensePlate(testCarRequest.getLicensePlate())).thenReturn(false);
            when(carRepository.existsByVinNumber(testCarRequest.getVinNumber())).thenReturn(true);

            assertThatThrownBy(() -> carService.createCar(testCarRequest))
                    .isInstanceOf(CarAlreadyExistsException.class)
                    .hasMessageContaining("Car already exists with vinNumber: " + testCarRequest.getVinNumber());

            verify(carRepository).existsByLicensePlate(testCarRequest.getLicensePlate());
            verify(carRepository).existsByVinNumber(testCarRequest.getVinNumber());
            verify(carMapper, never()).toEntity(any());
            verify(carRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Find Car Operations")
    class FindCarOperations {

        @Test
        @DisplayName("Should find car by ID when car exists")
        void shouldFindCarByIdWhenCarExists() {

            when(carRepository.findById(1L)).thenReturn(Optional.of(testCar1));
            when(carMapper.toDto(testCar1)).thenReturn(testCarResponse);

            CarResponse result = carService.getCarById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getLicensePlate()).isEqualTo("34ABC123");

            verify(carRepository).findById(1L);
            verify(carMapper).toDto(testCar1);
        }

        @Test
        @DisplayName("Should throw exception when car ID does not exist")
        void shouldThrowExceptionWhenCarIdDoesNotExist() {

            when(carRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> carService.getCarById(999L))
                    .isInstanceOf(CarNotFoundException.class)
                    .hasMessageContaining("Car not found with id: 999");

            verify(carRepository).findById(999L);
            verify(carMapper, never()).toDto(any());
        }

        @Test
        @DisplayName("Should find car by license plate when car exists")
        void shouldFindCarByLicensePlateWhenCarExists() {

            when(carRepository.findByLicensePlate("34ABC123")).thenReturn(Optional.of(testCar1));
            when(carMapper.toDto(testCar1)).thenReturn(testCarResponse);

            CarResponse result = carService.getCarByLicensePlate("34ABC123");

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getLicensePlate()).isEqualTo("34ABC123");

            verify(carRepository).findByLicensePlate("34ABC123");
            verify(carMapper).toDto(testCar1);
        }

        @Test
        @DisplayName("Should throw exception when car license plate does not exist")
        void shouldThrowExceptionWhenCarLicensePlateDoesNotExist() {
            when(carRepository.findByLicensePlate("NONEXISTENT")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> carService.getCarByLicensePlate("NONEXISTENT"))
                    .isInstanceOf(CarNotFoundException.class)
                    .hasMessageContaining("Car not found with license plate: NONEXISTENT");

            verify(carRepository).findByLicensePlate("NONEXISTENT");
            verify(carMapper, never()).toDto(any());
        }
    }

    @Nested
    @DisplayName("Update Car Operations")
    class UpdateCarOperations {

        @Test
        @DisplayName("Should update car successfully when car exists")
        void shouldUpdateCarSuccessfullyWhenCarExists() {

            when(carRepository.findById(1L)).thenReturn(Optional.of(testCar1));
            when(carRepository.existsByLicensePlate(testCarRequest.getLicensePlate())).thenReturn(false);
            when(carRepository.existsByVinNumber(testCarRequest.getVinNumber())).thenReturn(false);
            when(carRepository.save(testCar1)).thenReturn(testCar1);
            when(carMapper.toDto(testCar1)).thenReturn(testCarResponse);

            CarResponse result = carService.updateCar(1L, testCarRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);

            verify(carRepository).findById(1L);
            verify(carRepository).existsByLicensePlate(testCarRequest.getLicensePlate());
            verify(carRepository).existsByVinNumber(testCarRequest.getVinNumber());
            verify(carMapper).updateEntity(testCarRequest, testCar1);
            verify(carRepository).save(testCar1);
            verify(carMapper).toDto(testCar1);
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent car")
        void shouldThrowExceptionWhenUpdatingNonExistentCar() {

            when(carRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> carService.updateCar(999L, testCarRequest))
                    .isInstanceOf(CarNotFoundException.class)
                    .hasMessageContaining("Car not found with id: 999");

            verify(carRepository).findById(999L);
            verify(carRepository, never()).existsByLicensePlate(any());
            verify(carMapper, never()).updateEntity(any(), any());
            verify(carRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Car Operations")
    class DeleteCarOperations {

        @Test
        @DisplayName("Should delete car successfully when car exists")
        void shouldDeleteCarSuccessfullyWhenCarExists() {

            when(carRepository.findById(1L)).thenReturn(Optional.of(testCar1));

            carService.deleteCar(1L);

            verify(carRepository).findById(1L);
            verify(carRepository).delete(testCar1);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent car")
        void shouldThrowExceptionWhenDeletingNonExistentCar() {

            when(carRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> carService.deleteCar(999L))
                    .isInstanceOf(CarNotFoundException.class)
                    .hasMessageContaining("Car not found with id: 999");

            verify(carRepository).findById(999L);
            verify(carRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("List Car Operations")
    class ListCarOperations {

        @Test
        @DisplayName("Should get all cars with pagination")
        void shouldGetAllCarsWithPagination() {

            Pageable pageable = PageRequest.of(0, 10);
            Page<Car> carPage = new PageImpl<>(List.of(testCar1, testCar2));
            when(carRepository.findAll(pageable)).thenReturn(carPage);
            when(carMapper.toDto(testCar1)).thenReturn(testCarResponse);
            when(carMapper.toDto(testCar2)).thenReturn(testCarResponse);

            Page<CarResponse> result = carService.getAllCars(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);

            verify(carRepository).findAll(pageable);
            verify(carMapper, times(2)).toDto(any(Car.class));
        }

        @Test
        @DisplayName("Should get cars by status with pagination")
        void shouldGetCarsByStatusWithPagination() {

            Pageable pageable = PageRequest.of(0, 10);
            Page<Car> carPage = new PageImpl<>(List.of(testCar1, testCar2));
            when(carRepository.findByCarStatusTypeAndIsDeletedFalse(CarStatusType.AVAILABLE, pageable)).thenReturn(carPage);
            when(carMapper.toDto(testCar1)).thenReturn(testCarResponse);
            when(carMapper.toDto(testCar2)).thenReturn(testCarResponse);

            Page<CarResponse> result = carService.getCarsByStatus(String.valueOf(CarStatusType.AVAILABLE), pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);

            verify(carRepository).findByCarStatusTypeAndIsDeletedFalse(CarStatusType.AVAILABLE, pageable);
            verify(carMapper, times(2)).toDto(any(Car.class));
        }
    }

    @Nested
    @DisplayName("Business Logic Operations")
    class BusinessLogicOperations {

        @Test
        @DisplayName("Should increment view count when viewing car")
        void shouldIncrementViewCountWhenViewingCar() {

            when(carRepository.findById(1L)).thenReturn(Optional.of(testCar1));
            when(carRepository.save(testCar1)).thenReturn(testCar1);

            carService.incrementViewCount(1L);

            assertThat(testCar1.getViewCount()).isEqualTo(11L);

            verify(carRepository).findById(1L);
            verify(carRepository).save(testCar1);
        }

        @Test
        @DisplayName("Should increment like count when liking car")
        void shouldIncrementLikeCountWhenLikingCar() {

            when(carRepository.findById(1L)).thenReturn(Optional.of(testCar1));
            when(carRepository.save(testCar1)).thenReturn(testCar1);

            carService.incrementLikeCount(1L);

            assertThat(testCar1.getLikeCount()).isEqualTo(6L);

            verify(carRepository).findById(1L);
            verify(carRepository).save(testCar1);
        }

        @Test
        @DisplayName("Should change car status when updating status")
        void shouldChangeCarStatusWhenUpdatingStatus() {

            CarStatusUpdateRequest statusUpdateRequest = new CarStatusUpdateRequest(
                    CarStatusType.SOLD,
                    "Car sold to customer",
                    "Payment completed"
            );

            when(carRepository.findById(1L)).thenReturn(Optional.of(testCar1));
            when(carRepository.save(testCar1)).thenReturn(testCar1);
            when(carMapper.toDto(testCar1)).thenReturn(testCarResponse);

            CarResponse result = carService.updateCarStatus(1L, statusUpdateRequest);

            assertThat(result).isNotNull();
            assertThat(testCar1.getCarStatusType()).isEqualTo(CarStatusType.SOLD);

            verify(carRepository).findById(1L);
            verify(carRepository).save(testCar1);
            verify(carMapper).toDto(testCar1);
        }
    }
}
