package com.akif.car.internal.repository;

import com.akif.car.domain.enums.CarStatusType;
import com.akif.shared.enums.CurrencyType;
import com.akif.car.domain.Car;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    Page<Car> findByBrandIgnoreCaseAndModelIgnoreCaseAndIsDeletedFalse(String brand, String model,
                                                                       Pageable pageable);

    Page<Car> findByPriceBetweenAndIsDeletedFalse(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    Page<Car> findByPriceBetweenAndCurrencyTypeAndIsDeletedFalse(BigDecimal minPrice, BigDecimal maxPrice,
                                                                 CurrencyType currencyType, Pageable pageable);

    Page<Car> findByProductionYearBetweenAndIsDeletedFalse(Integer minYear, Integer maxYear, Pageable pageable);

    Page<Car> findByProductionYearGreaterThanEqualAndIsDeletedFalse(Integer yearThreshold, Pageable pageable);

    Page<Car> findByIsFeaturedTrueAndCarStatusTypeAndIsDeletedFalse(CarStatusType carStatusType, Pageable pageable);

    Page<Car> findByIsTestDriveAvailableTrueAndIsDeletedFalse(Pageable pageable);

    @EntityGraph(attributePaths = {})
    Page<Car> findByIsDeletedFalse(Pageable pageable);

    long countByIsDeletedFalse();

    Optional<Car> findByIdAndIsDeletedFalse(Long id);

    Page<Car> findByIsDeletedFalseOrderByViewCountDesc(Pageable pageable);

    Page<Car> findByIsDeletedFalseOrderByLikeCountDesc(Pageable pageable);

    @Query(value = "SELECT * FROM gallery.car c WHERE " +
            "(CAST(:searchTerm AS VARCHAR) IS NULL OR " +
            "LOWER(c.license_plate) LIKE CAST(:searchTerm AS VARCHAR) OR " +
            "LOWER(c.brand) LIKE CAST(:searchTerm AS VARCHAR) OR " +
            "LOWER(c.model) LIKE CAST(:searchTerm AS VARCHAR)) AND " +
            "(CAST(:brand AS VARCHAR) IS NULL OR LOWER(c.brand) = CAST(:brand AS VARCHAR)) AND " +
            "(CAST(:model AS VARCHAR) IS NULL OR LOWER(c.model) = CAST(:model AS VARCHAR)) AND " +
            "(CAST(:transmissionType AS VARCHAR) IS NULL OR LOWER(c.transmission_type) = CAST(:transmissionType AS VARCHAR)) AND "
            +
            "(CAST(:bodyType AS VARCHAR) IS NULL OR LOWER(c.body_type) = CAST(:bodyType AS VARCHAR)) AND " +
            "(CAST(:fuelType AS VARCHAR) IS NULL OR LOWER(c.fuel_type) = CAST(:fuelType AS VARCHAR)) AND " +
            "(CAST(:minSeats AS INTEGER) IS NULL OR c.seats >= CAST(:minSeats AS INTEGER)) AND " +
            "(CAST(:minProductionYear AS INTEGER) IS NULL OR c.production_year >= CAST(:minProductionYear AS INTEGER)) AND "
            +
            "(CAST(:maxProductionYear AS INTEGER) IS NULL OR c.production_year <= CAST(:maxProductionYear AS INTEGER)) AND "
            +
            "(CAST(:minPrice AS DECIMAL) IS NULL OR c.price >= CAST(:minPrice AS DECIMAL)) AND " +
            "(CAST(:maxPrice AS DECIMAL) IS NULL OR c.price <= CAST(:maxPrice AS DECIMAL)) AND " +
            "(CAST(:currencyType AS VARCHAR) IS NULL OR c.currency_type = CAST(:currencyType AS VARCHAR)) AND "
            +
            "(CAST(:carStatusType AS VARCHAR) IS NULL OR c.car_status_type = CAST(:carStatusType AS VARCHAR)) AND "
            +
            "c.is_deleted = false " +
            "ORDER BY c.create_time DESC", countQuery = "SELECT COUNT(*) FROM gallery.car c WHERE " +
            "(CAST(:searchTerm AS VARCHAR) IS NULL OR " +
            "LOWER(c.license_plate) LIKE CAST(:searchTerm AS VARCHAR) OR " +
            "LOWER(c.brand) LIKE CAST(:searchTerm AS VARCHAR) OR " +
            "LOWER(c.model) LIKE CAST(:searchTerm AS VARCHAR)) AND " +
            "(CAST(:brand AS VARCHAR) IS NULL OR LOWER(c.brand) = CAST(:brand AS VARCHAR)) AND "
            +
            "(CAST(:model AS VARCHAR) IS NULL OR LOWER(c.model) = CAST(:model AS VARCHAR)) AND "
            +
            "(CAST(:transmissionType AS VARCHAR) IS NULL OR LOWER(c.transmission_type) = CAST(:transmissionType AS VARCHAR)) AND "
            +
            "(CAST(:bodyType AS VARCHAR) IS NULL OR LOWER(c.body_type) = CAST(:bodyType AS VARCHAR)) AND "
            +
            "(CAST(:fuelType AS VARCHAR) IS NULL OR LOWER(c.fuel_type) = CAST(:fuelType AS VARCHAR)) AND "
            +
            "(CAST(:minSeats AS INTEGER) IS NULL OR c.seats >= CAST(:minSeats AS INTEGER)) AND "
            +
            "(CAST(:minProductionYear AS INTEGER) IS NULL OR c.production_year >= CAST(:minProductionYear AS INTEGER)) AND "
            +
            "(CAST(:maxProductionYear AS INTEGER) IS NULL OR c.production_year <= CAST(:maxProductionYear AS INTEGER)) AND "
            +
            "(CAST(:minPrice AS DECIMAL) IS NULL OR c.price >= CAST(:minPrice AS DECIMAL)) AND "
            +
            "(CAST(:maxPrice AS DECIMAL) IS NULL OR c.price <= CAST(:maxPrice AS DECIMAL)) AND "
            +
            "(CAST(:currencyType AS VARCHAR) IS NULL OR c.currency_type = CAST(:currencyType AS VARCHAR)) AND "
            +
            "(CAST(:carStatusType AS VARCHAR) IS NULL OR c.car_status_type = CAST(:carStatusType AS VARCHAR)) AND "
            +
            "c.is_deleted = false", nativeQuery = true)
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
                                 @Param("currencyType") String currencyType,
                                 @Param("carStatusType") String carStatusType,
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
            "(COALESCE(:brand, '') = '' OR LOWER(c.brand) = :brand) AND " +
            "(COALESCE(:model, '') = '' OR LOWER(c.model) = :model) AND " +
            "(COALESCE(:fuelType, '') = '' OR LOWER(c.fuelType) = :fuelType) AND " +
            "(COALESCE(:transmissionType, '') = '' OR LOWER(c.transmissionType) = :transmissionType) AND " +
            "(COALESCE(:bodyType, '') = '' OR LOWER(c.bodyType) = :bodyType) AND " +
            "(:minSeats IS NULL OR c.seats >= :minSeats) AND " +
            "(:minPrice IS NULL OR c.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR c.price <= :maxPrice) AND " +
            "(:minProductionYear IS NULL OR c.productionYear >= :minProductionYear) AND " +
            "(:maxProductionYear IS NULL OR c.productionYear <= :maxProductionYear) AND " +
            "NOT EXISTS (" +
            "   SELECT r FROM Rental r WHERE " +
            "   r.carId = c.id AND " +
            "   r.status IN (com.akif.rental.domain.enums.RentalStatus.CONFIRMED, com.akif.rental.domain.enums.RentalStatus.IN_USE) AND "
            +
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
            "(LOWER(c.bodyType) = :bodyType OR " +
            "(c.price >= :minPrice AND c.price <= :maxPrice))")
    Page<Car> findSimilarCars(
            @Param("bodyType") String bodyType,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("excludeCarId") Long excludeCarId,
            @Param("blockingStatuses") List<CarStatusType> blockingStatuses,
            Pageable pageable);

    @Modifying
    @Query("UPDATE Car c SET c.viewCount = c.viewCount + 1, c.updateTime = CURRENT_TIMESTAMP WHERE c.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Car c SET c.likeCount = c.likeCount + 1, c.updateTime = CURRENT_TIMESTAMP WHERE c.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Car c SET c.likeCount = CASE WHEN c.likeCount > 0 THEN c.likeCount - 1 ELSE 0 END, c.updateTime = CURRENT_TIMESTAMP WHERE c.id = :id")
    void decrementLikeCount(@Param("id") Long id);

    @Query("SELECT DISTINCT c.brand FROM Car c WHERE c.isDeleted = false AND c.brand IS NOT NULL ORDER BY c.brand")
    List<String> findDistinctBrands();

    @Query("SELECT DISTINCT c.transmissionType FROM Car c WHERE c.isDeleted = false AND c.transmissionType IS NOT NULL ORDER BY c.transmissionType")
    List<String> findDistinctTransmissionTypes();

    @Query("SELECT DISTINCT c.fuelType FROM Car c WHERE c.isDeleted = false AND c.fuelType IS NOT NULL ORDER BY c.fuelType")
    List<String> findDistinctFuelTypes();

    @Query("SELECT DISTINCT c.bodyType FROM Car c WHERE c.isDeleted = false AND c.bodyType IS NOT NULL ORDER BY c.bodyType")
    List<String> findDistinctBodyTypes();
}
