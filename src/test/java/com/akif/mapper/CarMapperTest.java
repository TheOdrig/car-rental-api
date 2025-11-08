package com.akif.mapper;

import com.akif.dto.request.CarRequestDto;
import com.akif.dto.response.CarResponseDto;
import com.akif.dto.response.CarSummaryResponseDto;
import com.akif.enums.CarStatusType;
import com.akif.enums.CurrencyType;
import com.akif.model.Car;
import com.akif.starter.CarGalleryProjectApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.context.ContextConfiguration;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@ContextConfiguration(classes = CarGalleryProjectApplication.class)
@DisplayName("CarMapper Tests")
public class CarMapperTest {

    private CarMapper carMapper;

    private Car testCar1;
    private Car testCar2;
    private CarRequestDto testCarRequestDto;

    @BeforeEach
    void setUp() {
        carMapper = Mappers.getMapper(CarMapper.class);

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
                .build();

        testCarRequestDto = CarRequestDto.builder()
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
    }


    @Test
    @DisplayName("Should map Car entity to CarResponseDto correctly")
    void shouldMapCarEntityToCarResponseDtoCorrectly() {

        CarResponseDto result = carMapper.toDto(testCar1);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getLicensePlate()).isEqualTo("34ABC123");
        assertThat(result.getVinNumber()).isEqualTo("VIN123456789");
        assertThat(result.getBrand()).isEqualTo("Toyota");
        assertThat(result.getModel()).isEqualTo("Corolla");
        assertThat(result.getProductionYear()).isEqualTo(2020);
        assertThat(result.getPrice()).isEqualTo(new BigDecimal("250000"));
        assertThat(result.getCurrencyType()).isEqualTo(CurrencyType.TRY);
        assertThat(result.getDamagePrice()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getCarStatusType()).isEqualTo(CarStatusType.AVAILABLE);
        assertThat(result.getIsFeatured()).isTrue();
        assertThat(result.getIsTestDriveAvailable()).isTrue();
        assertThat(result.getViewCount()).isEqualTo(10L);
        assertThat(result.getLikeCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Should map Car entity to CarSummaryResponseDto correctly")
    void shouldMapCarEntityToCarSummaryResponseDtoCorrectly() {

        CarSummaryResponseDto result = carMapper.toSummaryDto(testCar1);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getLicensePlate()).isEqualTo("34ABC123");
        assertThat(result.getBrand()).isEqualTo("Toyota");
        assertThat(result.getModel()).isEqualTo("Corolla");
        assertThat(result.getProductionYear()).isEqualTo(2020);
        assertThat(result.getCurrencyType()).isEqualTo(CurrencyType.TRY);
        assertThat(result.getCarStatusType()).isEqualTo(CarStatusType.AVAILABLE);
        assertThat(result.getIsFeatured()).isTrue();
    }

    @Test
    @DisplayName("Should map CarRequestDto to Car entity correctly")
    void shouldMapCarRequestDtoToCarEntityCorrectly() {

        Car result = carMapper.toEntity(testCarRequestDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNull();
        assertThat(result.getLicensePlate()).isEqualTo("35DEF456");
        assertThat(result.getVinNumber()).isEqualTo("VIN456789123");
        assertThat(result.getBrand()).isEqualTo("BMW");
        assertThat(result.getModel()).isEqualTo("X5");
        assertThat(result.getProductionYear()).isEqualTo(2019);
        assertThat(result.getPrice()).isEqualTo(new BigDecimal("1500000"));
        assertThat(result.getCurrencyType()).isEqualTo(CurrencyType.TRY);
        assertThat(result.getDamagePrice()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getCarStatusType()).isEqualTo(CarStatusType.SOLD);
        assertThat(result.getIsFeatured()).isTrue();
        assertThat(result.getIsTestDriveAvailable()).isFalse();
        assertThat(result.getCreateTime()).isNull();
        assertThat(result.getUpdateTime()).isNull();
        assertThat(result.getVersion()).isNull();
        assertThat(result.getIsDeleted()).isFalse();
        assertThat(result.getViewCount()).isNull();
        assertThat(result.getLikeCount()).isNull();
    }

    @Test
    @DisplayName("Should update Car entity with CarRequestDto data")
    void shouldUpdateCarEntityWithCarRequestDtoData() {

        Car existingCar = Car.builder()
                .id(1L)
                .licensePlate("OLD123")
                .brand("OldBrand")
                .model("OldModel")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .version(1L)
                .viewCount(100L)
                .likeCount(50L)
                .build();

        carMapper.updateEntity(testCarRequestDto, existingCar);

        assertThat(existingCar.getId()).isEqualTo(1L);
        assertThat(existingCar.getLicensePlate()).isEqualTo("35DEF456");
        assertThat(existingCar.getBrand()).isEqualTo("BMW");
        assertThat(existingCar.getModel()).isEqualTo("X5");
        assertThat(existingCar.getCreateTime()).isNotNull();
        assertThat(existingCar.getUpdateTime()).isNotNull();
        assertThat(existingCar.getVersion()).isEqualTo(1L);
        assertThat(existingCar.getIsDeleted()).isFalse();
        assertThat(existingCar.getViewCount()).isEqualTo(100L);
        assertThat(existingCar.getLikeCount()).isEqualTo(50L);
    }

    @Test
    @DisplayName("Should map list of Car entities to list of CarResponseDto")
    void shouldMapListOfCarEntitiesToListOfCarResponseDto() {

        List<Car> cars = List.of(testCar1, testCar2);

        List<CarResponseDto> result = carMapper.toDtoList(cars);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getBrand()).isEqualTo("Toyota");
        assertThat(result.get(1).getBrand()).isEqualTo("Honda");
    }

    @Test
    @DisplayName("Should map list of Car entities to list of CarSummaryResponseDto")
    void shouldMapListOfCarEntitiesToListOfCarSummaryResponseDto() {

        List<Car> cars = List.of(testCar1, testCar2);

        List<CarSummaryResponseDto> result = carMapper.toSummaryDtoList(cars);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getBrand()).isEqualTo("Toyota");
        assertThat(result.get(1).getBrand()).isEqualTo("Honda");
    }

    @Test
    @DisplayName("Should handle null Car entity gracefully")
    void shouldHandleNullCarEntityGracefully() {

        CarResponseDto result = carMapper.toDto(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should handle null CarRequestDto gracefully")
    void shouldHandleNullCarRequestDtoGracefully() {
        Car result = carMapper.toEntity(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should handle null Car list gracefully")
    void shouldHandleNullCarListGracefully() {
        List<CarResponseDto> result = carMapper.toDtoList(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should map computed fields correctly")
    void shouldMapComputedFieldsCorrectly() {

        CarResponseDto result = carMapper.toDto(testCar1);

        assertThat(result.getFormattedPrice()).isNotNull();
        assertThat(result.getAge()).isNotNull();
        assertThat(result.getFullName()).isEqualTo("Toyota Corolla");
        assertThat(result.getDisplayName()).isEqualTo("Toyota Corolla (2020)");
        assertThat(result.getTotalPrice()).isNotNull();
        assertThat(result.getIsNew()).isNotNull();
        assertThat(result.getIsOld()).isNotNull();
        assertThat(result.getHasDamage()).isNotNull();
        assertThat(result.getNeedsService()).isNotNull();
        assertThat(result.getIsInsuranceExpired()).isNotNull();
        assertThat(result.getIsInspectionExpired()).isNotNull();
        assertThat(result.getHasExpiredDocuments()).isNotNull();
        assertThat(result.getIsAvailable()).isNotNull();
        assertThat(result.getCanBeSold()).isNotNull();
        assertThat(result.getCanBeReserved()).isNotNull();
        assertThat(result.getRequiresAttention()).isNotNull();
    }
}
