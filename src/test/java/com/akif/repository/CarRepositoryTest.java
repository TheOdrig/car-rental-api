package com.akif.repository;

import com.akif.enums.CarStatusType;
import com.akif.enums.CurrencyType;
import com.akif.model.Car;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.akif.starter.CarGalleryProjectApplication;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = CarGalleryProjectApplication.class)
@ActiveProfiles("test")
@DisplayName("CarRepository Tests")
public class CarRepositoryTest {

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Car testCar1;
    private Car testCar2;
    private Car testCar3;

    @BeforeEach
    void setUp() {
        testCar1 = Car.builder()
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

        testCar3 = Car.builder()
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
                .viewCount(25L)
                .likeCount(15L)
                .build();

        carRepository.saveAll(List.of(testCar1, testCar2, testCar3));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Should find car by licensePlate")
    void shouldFindCarByLicensePlate() {

        Optional<Car> foundCar = carRepository.findByLicensePlate("34ABC123");

        assertThat(foundCar).isPresent();
        assertThat(foundCar.get().getBrand()).isEqualTo("Toyota");
        assertThat(foundCar.get().getModel()).isEqualTo("Corolla");
    }

    @Test
    @DisplayName("Should find car by VIN number")
    void shouldFindCarByVinNumber() {

        Optional<Car> foundCar = carRepository.findByVinNumber("VIN123456789");

        assertThat(foundCar).isPresent();
        assertThat(foundCar.get().getLicensePlate()).isEqualTo("34ABC123");
        assertThat(foundCar.get().getBrand()).isEqualTo("Toyota");
    }


    @Test
    @DisplayName("Should check if car exists by licensePlate")
    void shouldCheckIfCarExistsByLicensePlate() {

        assertThat(carRepository.existsByLicensePlate("34ABC123")).isTrue();
        assertThat(carRepository.existsByLicensePlate("99NOTFOUND")).isFalse();
    }

    @Test
    @DisplayName("Should check if car exists by VIN number")
    void shouldCheckIfCarExistsByVinNumber() {

        assertThat(carRepository.existsByVinNumber("VIN123456789")).isTrue();
        assertThat(carRepository.existsByVinNumber("VINNOTFOUND")).isFalse();
    }


    @Test
    @DisplayName("Should find cars by status with pagination")
    void shouldFindCarsByStatus_WithPagination() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<Car> availableCars = carRepository.findByCarStatusTypeAndIsDeletedFalse(CarStatusType.AVAILABLE, pageable);

        assertThat(availableCars.getContent()).hasSize(2);
        assertThat(availableCars.getContent()).allMatch(car -> car.getCarStatusType() == CarStatusType.AVAILABLE);
        assertThat(availableCars.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should count cars by status type")
    void shouldCountCarsByStatusType() {

        long availableCount = carRepository.countByCarStatusTypeAndIsDeletedFalse(CarStatusType.AVAILABLE);
        assertThat(availableCount).isEqualTo(2L);

        long maintenanceCount = carRepository.countByCarStatusTypeAndIsDeletedFalse(CarStatusType.MAINTENANCE);
        assertThat(maintenanceCount).isEqualTo(0L);
    }


    @Test
    @DisplayName("Should find cars by brand (case insensitive)")
    void shouldFindCarsByBrandCaseInsensitive() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<Car> toyotaCars = carRepository.findByBrandIgnoreCaseAndIsDeletedFalse("toyota", pageable);

        assertThat(toyotaCars.getContent()).hasSize(1);
        assertThat(toyotaCars.getContent().get(0).getBrand()).isEqualTo("Toyota");
    }

    @Test
    @DisplayName("Should find cars by brand and model")
    void shouldFindCarsByBrandAndModel() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<Car> cars = carRepository.findByBrandIgnoreCaseAndModelIgnoreCaseAndIsDeletedFalse("honda", "civic", pageable);

        assertThat(cars.getContent()).hasSize(1);
        assertThat(cars.getContent().get(0).getBrand()).isEqualTo("Honda");
        assertThat(cars.getContent().get(0).getModel()).isEqualTo("Civic");
    }


    @Test
    @DisplayName("Should find cars by price range")
    void shouldFindCarsByPriceRange() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<Car> cars = carRepository.findByPriceBetweenAndIsDeletedFalse(
                new BigDecimal("200000"), new BigDecimal("400000"), pageable);

        assertThat(cars.getContent()).hasSize(2);
        assertThat(cars.getContent()).allMatch(car ->
                car.getPrice().compareTo(new BigDecimal("200000")) >= 0 &&
                        car.getPrice().compareTo(new BigDecimal("400000")) <= 0);
    }

    @Test
    @DisplayName("Should find cars by price range and currency")
    void shouldFindCarsByPriceRangeAndCurrency() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<Car> cars = carRepository.findByPriceBetweenAndCurrencyTypeAndIsDeletedFalse(
                new BigDecimal("200000"), new BigDecimal("400000"), CurrencyType.TRY, pageable);

        assertThat(cars.getContent()).hasSize(2);
        assertThat(cars.getContent()).allMatch(car ->
                car.getPrice().compareTo(new BigDecimal("200000")) >= 0 &&
                        car.getPrice().compareTo(new BigDecimal("400000")) <= 0 &&
                        car.getCurrencyType() == CurrencyType.TRY);
    }


    @Test
    @DisplayName("Should find cars by production year range")
    void shouldFindCarsByProductionYearRange() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<Car> cars = carRepository.findByProductionYearBetweenAndIsDeletedFalse(2020, 2021, pageable);

        assertThat(cars.getContent()).hasSize(2);
        assertThat(cars.getContent()).allMatch(car ->
                car.getProductionYear() >= 2020 && car.getProductionYear() <= 2021);
    }

    @Test
    @DisplayName("Should find new cars")
    void shouldFindNewCars() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<Car> cars = carRepository.findByProductionYearGreaterThanEqualAndIsDeletedFalse(2020, pageable);

        assertThat(cars.getContent()).hasSize(2);
        assertThat(cars.getContent()).allMatch(car -> car.getProductionYear() >= 2020);
    }


    @Test
    @DisplayName("Should find featured cars")
    void shouldFindFeaturedCars() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<Car> cars = carRepository.findByIsFeaturedTrueAndIsDeletedFalse(pageable);

        assertThat(cars.getContent()).hasSize(2);
        assertThat(cars.getContent()).allMatch(Car::getIsFeatured);
    }

    @Test
    @DisplayName("Should find cars available for test drive")
    void shouldFindCarsAvailableForTestDrive() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<Car> cars = carRepository.findByIsTestDriveAvailableTrueAndIsDeletedFalse(pageable);

        assertThat(cars.getContent()).hasSize(1);
        assertThat(cars.getContent()).allMatch(Car::getIsTestDriveAvailable);
    }


    @Test
    @DisplayName("Should find all active cars")
    void shouldFindAllActiveCars() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<Car> cars = carRepository.findByIsDeletedFalse(pageable);

        assertThat(cars.getContent()).hasSize(3);
        assertThat(cars.getContent()).allMatch(car -> !car.getIsDeleted());
    }

    @Test
    @DisplayName("Should count active cars")
    void shouldCountActiveCars() {

        long count = carRepository.countByIsDeletedFalse();

        assertThat(count).isEqualTo(3);
    }


    @Test
    @DisplayName("Should find cars ordered by view count")
    void shouldFindCarsOrderedByViewCount() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<Car> cars = carRepository.findByIsDeletedFalseOrderByViewCountDesc(pageable);

        assertThat(cars.getContent()).hasSize(3);
        List<Car> carList = cars.getContent();
        assertThat(carList.get(0).getViewCount()).isGreaterThanOrEqualTo(carList.get(1).getViewCount());
        assertThat(carList.get(1).getViewCount()).isGreaterThanOrEqualTo(carList.get(2).getViewCount());
    }

    @Test
    @DisplayName("Should find cars ordered by like count")
    void shouldFindCarsOrderedByLikeCount() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<Car> cars = carRepository.findByIsDeletedFalseOrderByLikeCountDesc(pageable);

        assertThat(cars.getContent()).hasSize(3);
        List<Car> carList = cars.getContent();
        assertThat(carList.get(0).getLikeCount()).isGreaterThanOrEqualTo(carList.get(1).getLikeCount());
        assertThat(carList.get(1).getLikeCount()).isGreaterThanOrEqualTo(carList.get(2).getLikeCount());
    }


    @Test
    @DisplayName("Should find cars by criteria with search term")
    void shouldFindCarsByCriteriaWithSearchTerm() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<Car> cars = carRepository.findCarsByCriteria(
                "toyota", null, null, null, null, null, null, null, null, pageable);

        assertThat(cars.getContent()).hasSize(1);
        assertThat(cars.getContent().get(0).getBrand()).isEqualTo("Toyota");
    }

    @Test
    @DisplayName("Should find cars by criteria with brand filter")
    void shouldFindCarsByCriteriaWithBrandFilter() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<Car> cars = carRepository.findCarsByCriteria(
                null, "Honda", null, null, null, null, null, null, null, pageable);

        assertThat(cars.getContent()).hasSize(1);
        assertThat(cars.getContent().get(0).getBrand()).isEqualTo("Honda");
    }

    @Test
    @DisplayName("Should find cars by criteria with price range")
    void shouldFindCarsByCriteriaWithPriceRange() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<Car> cars = carRepository.findCarsByCriteria(
                null, null, null, null, null, new BigDecimal("200000"), new BigDecimal("400000"), null, null, pageable);

        assertThat(cars.getContent()).hasSize(2);
        assertThat(cars.getContent()).allMatch(car ->
                car.getPrice().compareTo(new BigDecimal("200000")) >= 0 &&
                        car.getPrice().compareTo(new BigDecimal("400000")) <= 0);
    }


    @Test
    @DisplayName("Should get average price")
    void shouldGetAveragePrice() {

        BigDecimal averagePrice = carRepository.getAveragePrice();

        assertThat(averagePrice).isNotNull();
        assertThat(averagePrice).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should get minimum price")
    void shouldGetMinPrice() {

        BigDecimal minPrice = carRepository.getMinPrice();

        assertThat(minPrice).isNotNull();
        assertThat(minPrice).isEqualByComparingTo(new BigDecimal("250000"));
    }

    @Test
    @DisplayName("Should get maximum price")
    void shouldGetMaxPrice() {

        BigDecimal maxPrice = carRepository.getMaxPrice();

        assertThat(maxPrice).isNotNull();
        assertThat(maxPrice).isEqualByComparingTo(new BigDecimal("1500000"));
    }

    @Test
    @DisplayName("Should get cars count by brand")
    void shouldGetCarsCountByBrand() {

        List<Object[]> brandCounts = carRepository.getCarsCountByBrand();

        assertThat(brandCounts).hasSize(3);
        assertThat(brandCounts).anyMatch(result ->
                "Toyota".equals(result[0]) && ((Long) result[1]) == 1L);
        assertThat(brandCounts).anyMatch(result ->
                "Honda".equals(result[0]) && ((Long) result[1]) == 1L);
        assertThat(brandCounts).anyMatch(result ->
                "BMW".equals(result[0]) && ((Long) result[1]) == 1L);
    }

    @Test
    @DisplayName("Should get average price by brand")
    void shouldGetAveragePriceByBrand() {

        List<Object[]> brandPrices = carRepository.getAveragePriceByBrand();

        assertThat(brandPrices).hasSize(3);
        assertThat(brandPrices).anyMatch(result -> "Toyota".equals(result[0]));
        assertThat(brandPrices).anyMatch(result -> "Honda".equals(result[0]));
        assertThat(brandPrices).anyMatch(result -> "BMW".equals(result[0]));
    }

    @Test
    @DisplayName("Should get cars count by status")
    void shouldGetCarsCountByStatus() {

        List<Object[]> statusCounts = carRepository.getCarsCountByStatus();

        assertThat(statusCounts).hasSize(2);
        assertThat(statusCounts).anyMatch(result ->
                CarStatusType.AVAILABLE.equals(result[0]) && ((Long) result[1]) == 2L);
        assertThat(statusCounts).anyMatch(result ->
                CarStatusType.SOLD.equals(result[0]) && ((Long) result[1]) == 1L);
    }
}
