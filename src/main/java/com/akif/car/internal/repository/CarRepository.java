package com.akif.car.internal.repository;

import com.akif.car.domain.enums.CarStatusType;
import com.akif.shared.enums.CurrencyType;
import com.akif.car.domain.Car;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {

    Optional<Car> findByLicensePlate(String licensePlate);
    Optional<Car> findByVinNumber(String vinNumber);

    boolean existsByLicensePlate(String licensePlate);
    boolean existsByVinNumber(String vinNumber);

    Page<Car> findByCarStatusTypeAndIsDeletedFalse(CarStatusType carStatusType, Pageable pageable);
    long countByCarStatusTypeAndIsDeletedFalse(CarStatusType carStatusType);

    Page<Car> findByBrandIgnoreCaseAndIsDeletedFalse(String brand, Pageable pageable);
    Page<Car> findByBrandIgnoreCaseAndModelIgnoreCaseAndIsDeletedFalse(String brand, String model, Pageable pageable);

    Page<Car> findByPriceBetweenAndIsDeletedFalse(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    Page<Car> findByPriceBetweenAndCurrencyTypeAndIsDeletedFalse(BigDecimal minPrice, BigDecimal maxPrice, CurrencyType currencyType, Pageable pageable);

    Page<Car> findByProductionYearBetweenAndIsDeletedFalse(Integer minYear, Integer maxYear, Pageable pageable);
    Page<Car> findByProductionYearGreaterThanEqualAndIsDeletedFalse(Integer yearThreshold, Pageable pageable);

    Page<Car> findByIsFeaturedTrueAndIsDeletedFalse(Pageable pageable);
    Page<Car> findByIsTestDriveAvailableTrueAndIsDeletedFalse(Pageable pageable);

    Page<Car> findByIsDeletedFalse(Pageable pageable);
    long countByIsDeletedFalse();

    Optional<Car> findByIdAndIsDeletedFalse(Long id);

    Page<Car> findByIsDeletedFalseOrderByViewCountDesc(Pageable pageable);
    Page<Car> findByIsDeletedFalseOrderByLikeCountDesc(Pageable pageable);


    @Query("SELECT c FROM Car c WHERE " +
            "(:searchTerm IS NULL OR " +
            "LOWER(c.licensePlate) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.brand) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.model) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
            "(:brand IS NULL OR LOWER(c.brand) = LOWER(:brand)) AND " +
            "(:model IS NULL OR LOWER(c.model) = LOWER(:model)) AND " +
            "(:transmissionType IS NULL OR LOWER(c.transmissionType) = LOWER(:transmissionType)) AND " +
            "(:bodyType IS NULL OR LOWER(c.bodyType) = LOWER(:bodyType)) AND " +
            "(:fuelType IS NULL OR LOWER(c.fuelType) = LOWER(:fuelType)) AND " +
            "(:minSeats IS NULL OR c.seats >= :minSeats) AND " +
            "(:minProductionYear IS NULL OR c.productionYear >= :minProductionYear) AND " +
            "(:maxProductionYear IS NULL OR c.productionYear <= :maxProductionYear) AND " +
            "(:minPrice IS NULL OR c.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR c.price <= :maxPrice) AND " +
            "(:currencyType IS NULL OR c.currencyType = :currencyType) AND " +
            "(:carStatusType IS NULL OR c.carStatusType = :carStatusType) AND " +
            "c.isDeleted = false")
    Page<Car> findCarsByCriteria(@Param("searchTerm") String searchTerm,
                                 @Param("brand") String brand,
                                 @Param("model") String model,
                                 @Param("transmissionType") String transmissionType,
                                 @Param("bodyType") String bodyType,
                                 @Param("fuelType") String fuelType,
                                 @Param("minSeats") Integer minSeats,
                                 @Param("minProductionYear") Integer minProductionYear,
                                 @Param("maxProductionYear") Integer maxProductionYear,
                                 @Param("minPrice") BigDecimal minPrice,
                                 @Param("maxPrice") BigDecimal maxPrice,
                                 @Param("currencyType") CurrencyType currencyType,
                                 @Param("carStatusType") CarStatusType carStatusType,
                                 Pageable pageable);

    @Query("SELECT AVG(c.price) FROM Car c WHERE c.isDeleted = false")
    BigDecimal getAveragePrice();

    @Query("SELECT MIN(c.price) FROM Car c WHERE c.isDeleted = false")
    BigDecimal getMinPrice();

    @Query("SELECT MAX(c.price) FROM Car c WHERE c.isDeleted = false")
    BigDecimal getMaxPrice();

    @Query("SELECT c.brand, COUNT(c) FROM Car c WHERE c.isDeleted = false GROUP BY c.brand ORDER BY COUNT(c) DESC")
    List<Object[]> getCarsCountByBrand();

    @Query("SELECT c.brand, CAST(AVG(c.price) AS BigDecimal) FROM Car c WHERE c.isDeleted = false GROUP BY c.brand ORDER BY AVG(c.price) DESC")
    List<Object[]> getAveragePriceByBrand();

    @Query("SELECT c.carStatusType, COUNT(c) FROM Car c WHERE c.isDeleted = false GROUP BY c.carStatusType")
    List<Object[]> getCarsCountByStatus();


    @Query("SELECT c FROM Car c WHERE " +
            "c.isDeleted = false AND " +
            "c.carStatusType NOT IN :blockingStatuses AND " +
            "(:brand IS NULL OR LOWER(c.brand) = LOWER(:brand)) AND " +
            "(:model IS NULL OR LOWER(c.model) = LOWER(:model)) AND " +
            "(:fuelType IS NULL OR LOWER(c.fuelType) = LOWER(:fuelType)) AND " +
            "(:transmissionType IS NULL OR LOWER(c.transmissionType) = LOWER(:transmissionType)) AND " +
            "(:bodyType IS NULL OR LOWER(c.bodyType) = LOWER(:bodyType)) AND " +
            "(:minSeats IS NULL OR c.seats >= :minSeats) AND " +
            "(:minPrice IS NULL OR c.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR c.price <= :maxPrice) AND " +
            "(:minProductionYear IS NULL OR c.productionYear >= :minProductionYear) AND " +
            "(:maxProductionYear IS NULL OR c.productionYear <= :maxProductionYear) AND " +
            "NOT EXISTS (" +
            "   SELECT r FROM Rental r WHERE " +
            "   r.carId = c.id AND " +
            "   r.status IN (com.akif.rental.domain.enums.RentalStatus.CONFIRMED, com.akif.rental.domain.enums.RentalStatus.IN_USE) AND " +
            "   r.isDeleted = false AND " +
            "   r.startDate <= :endDate AND r.endDate >= :startDate" +
            ")")
    Page<Car> findAvailableCarsForDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("blockingStatuses") List<CarStatusType> blockingStatuses,
            @Param("brand") String brand,
            @Param("model") String model,
            @Param("fuelType") String fuelType,
            @Param("transmissionType") String transmissionType,
            @Param("bodyType") String bodyType,
            @Param("minSeats") Integer minSeats,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minProductionYear") Integer minProductionYear,
            @Param("maxProductionYear") Integer maxProductionYear,
            Pageable pageable);

    @Query("SELECT c FROM Car c WHERE " +
            "c.isDeleted = false AND " +
            "c.id != :excludeCarId AND " +
            "c.carStatusType NOT IN :blockingStatuses AND " +
            "(LOWER(c.bodyType) = LOWER(:bodyType) OR " +
            "(c.price >= :minPrice AND c.price <= :maxPrice))")
    Page<Car> findSimilarCars(
            @Param("bodyType") String bodyType,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("excludeCarId") Long excludeCarId,
            @Param("blockingStatuses") List<CarStatusType> blockingStatuses,
            Pageable pageable);
}
