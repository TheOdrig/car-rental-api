package com.akif.car.internal.service;

import com.akif.car.api.CarDto;
import com.akif.car.api.CarService;
import com.akif.car.internal.dto.response.CarListResponse;
import com.akif.car.api.CarSummaryResponse;
import com.akif.car.internal.dto.request.CarRequest;
import com.akif.car.internal.exception.*;
import com.akif.car.internal.mapper.CarMapper;
import com.akif.car.internal.dto.pricing.CarPriceUpdateRequest;
import com.akif.car.internal.dto.request.CarSearchRequest;
import com.akif.car.internal.dto.request.CarStatusUpdateRequest;
import com.akif.car.api.CarResponse;
import com.akif.car.domain.enums.CarStatusType;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.shared.exception.InvalidStatusTransitionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CarServiceImpl implements CarService {

    private final CarRepository carRepository;
    private final CarMapper carMapper;


    @Override
    @Cacheable(value = "cars", key = "'dto:' + #id")
    public CarDto getCarDtoById(Long id) {
        log.debug("Getting minimal CarDto for cross-module usage, id: {}", id);
        validateCarId(id);

        Car car = findCarById(id);
        CarDto result = carMapper.toCarDto(car);

        log.debug("Successfully retrieved CarDto for id: {}", id);
        return result;
    }


    @Override
    @Cacheable(value = "cars", key = "#id")
    public CarResponse getCarById(Long id) {
        log.debug("Getting car by id: {}", id);
        validateCarId(id);

        Car car = findCarById(id);
        CarResponse result = carMapper.toDto(car);

        logCarRetrievalSuccess(result);
        return result;
    }

    @Override
    @Cacheable(value = "cars", key = "'licensePlate:' + #licensePlate")
    public CarResponse getCarByLicensePlate(String licensePlate) {
        log.debug("Getting car by license plate: {}", licensePlate);
        validateLicensePlate(licensePlate);

        Car car = findCarByLicensePlate(licensePlate);
        CarResponse result = carMapper.toDto(car);

        logCarRetrievalSuccess(result);
        return result;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "cars", allEntries = true),
        @CacheEvict(value = "car-status-counts", allEntries = true),
        @CacheEvict(value = "car-statistics", allEntries = true),
        @CacheEvict(value = "car-brand-counts", allEntries = true)
    })
    public CarResponse createCar(CarRequest carRequest) {
        log.debug("Creating new car with license plate: {}", carRequest.getLicensePlate());

        validateCarRequest(carRequest);
        checkLicensePlateUniqueness(carRequest.getLicensePlate());
        checkVinNumberUniqueness(carRequest.getVinNumber());

        Car car = carMapper.toEntity(carRequest);
        car.setCreateTime(LocalDateTime.now());
        car.setIsDeleted(false);
        car.setViewCount(0L);
        car.setLikeCount(0L);

        Car savedCar = carRepository.save(car);
        CarResponse result = carMapper.toDto(savedCar);

        logCarCreationSuccess(result);
        return result;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "cars", allEntries = true),
        @CacheEvict(value = "car-status-counts", allEntries = true),
        @CacheEvict(value = "car-statistics", allEntries = true)
    })
    public CarResponse updateCar(Long id, CarRequest carRequest) {
        log.debug("Updating car with id: {}", id);

        validateCarId(id);
        validateCarRequest(carRequest);

        Car existingCar = findCarById(id);

        if (!existingCar.getLicensePlate().equals(carRequest.getLicensePlate())) {
            checkLicensePlateUniqueness(carRequest.getLicensePlate());
        }
        
        if (!existingCar.getVinNumber().equals(carRequest.getVinNumber())) {
            checkVinNumberUniqueness(carRequest.getVinNumber());
        }

        carMapper.updateEntity(carRequest, existingCar);
        existingCar.setUpdateTime(LocalDateTime.now());

        Car updatedCar = carRepository.save(existingCar);
        CarResponse result = carMapper.toDto(updatedCar);

        logCarUpdateSuccess(result);
        return result;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "cars", allEntries = true),
        @CacheEvict(value = "car-status-counts", allEntries = true),
        @CacheEvict(value = "car-statistics", allEntries = true),
        @CacheEvict(value = "car-brand-counts", allEntries = true)
    })
    public void deleteCar(Long id) {
        log.debug("Hard deleting car with id: {}", id);
        validateCarId(id);

        Car car = findCarById(id);
        carRepository.delete(car);

        log.info("Successfully deleted car: ID={}", id);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "cars", allEntries = true),
        @CacheEvict(value = "car-status-counts", allEntries = true),
        @CacheEvict(value = "car-statistics", allEntries = true)
    })
    public void softDeleteCar(Long id) {
        log.debug("Soft deleting car with id: {}", id);
        validateCarId(id);

        Car car = findCarById(id);
        car.softDelete();
        car.setUpdateTime(LocalDateTime.now());

        carRepository.save(car);
        log.info("Successfully soft deleted car: ID={}", id);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "cars", allEntries = true),
        @CacheEvict(value = "car-status-counts", allEntries = true),
        @CacheEvict(value = "car-statistics", allEntries = true)
    })
    public void restoreCar(Long id) {
        log.debug("Restoring car with id: {}", id);
        validateCarId(id);

        Car car = findCarById(id);
        car.restore();
        car.setUpdateTime(LocalDateTime.now());

        carRepository.save(car);
        log.info("Successfully restored car: ID={}", id);
    }

    @Override
    @Cacheable(value = "cars", key = "'search:' + #searchRequest.hashCode()")
    public CarListResponse searchCars(CarSearchRequest searchRequest) {
        log.debug("Searching cars with criteria: {}", searchRequest);

        Pageable pageable = buildPageable(searchRequest);

        Page<Car> cars = carRepository.findCarsByCriteria(
                searchRequest.getSearchTerm(),
                searchRequest.getBrand(),
                searchRequest.getModel(),
                searchRequest.getTransmissionType(),
                searchRequest.getBodyType(),
                searchRequest.getFuelType(),
                searchRequest.getMinSeats(),
                searchRequest.getMinProductionYear(),
                searchRequest.getMaxProductionYear(),
                searchRequest.getMinPrice(),
                searchRequest.getMaxPrice(),
                searchRequest.getCurrencyType(),
                searchRequest.getCarStatusType(),
                pageable
        );

        List<CarResponse> carDtos = cars.getContent().stream()
                .map(carMapper::toDto)
                .collect(Collectors.toList());

        CarListResponse result = new CarListResponse(
                carDtos,
                cars.getTotalElements(),
                cars.getTotalPages(),
                cars.getNumber(),
                cars.getSize(),
                cars.isFirst(),
                cars.isLast(),
                cars.hasNext(),
                cars.hasPrevious(),
                cars.getNumberOfElements()
        );

        logSearchSuccess(result);
        return result;
    }

    @Override
    @Cacheable(value = "cars", key = "'all:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<CarResponse> getAllCars(Pageable pageable) {
        log.debug("Getting all cars with pageable: {}", pageable);

        Page<Car> cars = carRepository.findAll(pageable);
        Page<CarResponse> result = cars.map(carMapper::toDto);

        logPagedRetrievalSuccess("cars", result);
        return result;
    }

    @Override
    @Cacheable(value = "cars", key = "'status:' + #status + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<CarResponse> getCarsByStatus(String status, Pageable pageable) {
        log.debug("Getting cars by status: {}", status);

        CarStatusType statusType = CarStatusType.fromString(status);
        Page<Car> cars = carRepository.findByCarStatusTypeAndIsDeletedFalse(statusType, pageable);
        Page<CarResponse> result = cars.map(carMapper::toDto);

        logPagedRetrievalSuccess("cars", result);
        return result;
    }

    @Override
    public long getCarCount() {
        return carRepository.count();
    }

    @Override
    @Cacheable(value = "cars", key = "'brand:' + #brand + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<CarResponse> getCarsByBrand(String brand, Pageable pageable) {
        log.debug("Getting cars by brand: {}", brand);

        Page<Car> cars = carRepository.findByBrandIgnoreCaseAndIsDeletedFalse(brand, pageable);
        Page<CarResponse> result = cars.map(carMapper::toDto);

        log.info("Successfully retrieved {} cars with brand: {}", result.getTotalElements(), brand);
        return result;
    }

    @Override
    @Cacheable(value = "cars", key = "'price:' + #minPrice + ':' + #maxPrice + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<CarResponse> getCarsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        log.debug("Getting cars by price range: {} - {}", minPrice, maxPrice);

        Page<Car> cars = carRepository.findByPriceBetweenAndIsDeletedFalse(minPrice, maxPrice, pageable);
        Page<CarResponse> result = cars.map(carMapper::toDto);

        log.info("Successfully retrieved {} cars in price range: {} - {}", result.getTotalElements(), minPrice, maxPrice);
        return result;
    }

    @Override
    @Cacheable(value = "cars", key = "'new:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<CarResponse> getNewCars(Pageable pageable) {
        log.debug("Getting new cars");

        LocalDate oneYearAgo = LocalDate.now().minusYears(1);
        Page<Car> cars = carRepository.findByProductionYearGreaterThanEqualAndIsDeletedFalse(oneYearAgo.getYear(), pageable);
        Page<CarResponse> result = cars.map(carMapper::toDto);

        logPagedRetrievalSuccess("new cars", result);
        return result;
    }

    @Override
    @Cacheable(value = "cars", key = "'featured:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<CarResponse> getFeaturedCars(Pageable pageable) {
        log.debug("Getting featured cars");

        Page<Car> cars = carRepository.findByIsFeaturedTrueAndIsDeletedFalse(pageable);
        Page<CarResponse> result = cars.map(carMapper::toDto);

        logPagedRetrievalSuccess("featured cars", result);
        return result;
    }

    @Override
    @Cacheable(value = "cars", key = "'testdrive:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<CarResponse> getCarsAvailableForTestDrive(Pageable pageable) {
        log.debug("Getting cars available for test drive");

        Page<Car> cars = carRepository.findByIsTestDriveAvailableTrueAndIsDeletedFalse(pageable);
        Page<CarResponse> result = cars.map(carMapper::toDto);

        logPagedRetrievalSuccess("cars available for test drive", result);
        return result;
    }

    @Override
    @Cacheable(value = "cars", key = "'active:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<CarResponse> getAllActiveCars(Pageable pageable) {
        log.debug("Getting all active cars with pageable: {}", pageable);

        Page<Car> cars = carRepository.findByIsDeletedFalse(pageable);
        Page<CarResponse> result = cars.map(carMapper::toDto);

        logPagedRetrievalSuccess("active cars", result);
        return result;
    }

    @Override
    public long getActiveCarCount() {
        return carRepository.countByIsDeletedFalse();
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "cars", allEntries = true),
        @CacheEvict(value = "car-status-counts", allEntries = true),
        @CacheEvict(value = "car-statistics", allEntries = true)
    })
    public CarResponse sellCar(Long id) {
        log.debug("Selling car with id: {}", id);
        validateCarId(id);

        Car car = findCarById(id);

        if (!car.canBeSold()) {
            log.warn("Car {} cannot be sold, current status: {}", car.getId(), car.getCarStatusType());
            throw new CarCannotBeSoldException("Car cannot be sold, current status: " + car.getCarStatusType());
        }

        car.markAsSold();
        car.setUpdateTime(LocalDateTime.now());

        Car savedCar = carRepository.save(car);
        CarResponse result = carMapper.toDto(savedCar);

        log.info("Successfully sold car: ID={}, License Plate={}", result.getId(), result.getLicensePlate());
        return result;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "cars", allEntries = true),
        @CacheEvict(value = "car-status-counts", allEntries = true),
        @CacheEvict(value = "car-statistics", allEntries = true)
    })
    public CarResponse reserveCar(Long id) {
        log.debug("Reserving car with id: {}", id);
        validateCarId(id);

        Car car = findCarById(id);

        if (!car.canBeReserved()) {
            log.warn("Car {} cannot be reserved, current status: {}", car.getId(), car.getCarStatusType());
            throw new CarCannotBeReservedException("Car cannot be reserved, current status: " + car.getCarStatusType());
        }

        car.markAsReserved();
        car.setUpdateTime(LocalDateTime.now());

        Car savedCar = carRepository.save(car);
        CarResponse result = carMapper.toDto(savedCar);

        log.info("Successfully reserved car: ID={}, License Plate={}", result.getId(), result.getLicensePlate());
        return result;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "cars", allEntries = true),
        @CacheEvict(value = "car-status-counts", allEntries = true),
        @CacheEvict(value = "car-statistics", allEntries = true)
    })
    public CarResponse cancelReservation(Long id) {
        log.debug("Cancelling reservation for car with id: {}", id);
        validateCarId(id);

        Car car = findCarById(id);

        if (car.getCarStatusType() != CarStatusType.RESERVED) {
            log.warn("Car {} is not reserved, current status: {}", car.getId(), car.getCarStatusType());
            throw new InvalidStatusTransitionException("Car is not reserved, current status: " + car.getCarStatusType());
        }

        car.markAsAvailable();
        car.setUpdateTime(LocalDateTime.now());

        Car savedCar = carRepository.save(car);
        CarResponse result = carMapper.toDto(savedCar);

        log.info("Successfully cancelled reservation for car: ID={}, License Plate={}", result.getId(), result.getLicensePlate());
        return result;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "cars", allEntries = true),
        @CacheEvict(value = "car-status-counts", allEntries = true),
        @CacheEvict(value = "car-statistics", allEntries = true)
    })
    public CarResponse releaseCar(Long id) {
        log.debug("Releasing car after rental ends, id: {}", id);
        validateCarId(id);

        Car car = findCarById(id);

        if (car.getCarStatusType() != CarStatusType.RESERVED) {
            log.warn("Car {} is not reserved, current status: {}", car.getId(), car.getCarStatusType());
            throw new InvalidStatusTransitionException("Car is not in RESERVED status, current status: " + car.getCarStatusType());
        }

        car.markAsAvailable();
        car.setUpdateTime(LocalDateTime.now());

        Car savedCar = carRepository.save(car);
        CarResponse result = carMapper.toDto(savedCar);

        log.info("Successfully released car after rental: ID={}, License Plate={}", result.getId(), result.getLicensePlate());
        return result;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "cars", allEntries = true),
        @CacheEvict(value = "car-status-counts", allEntries = true),
        @CacheEvict(value = "car-statistics", allEntries = true)
    })
    public CarResponse markAsMaintenance(Long id) {
        log.debug("Marking car as maintenance with id: {}", id);
        validateCarId(id);

        Car car = findCarById(id);
        car.markAsMaintenance();
        car.setUpdateTime(LocalDateTime.now());

        Car savedCar = carRepository.save(car);
        CarResponse result = carMapper.toDto(savedCar);

        log.info("Successfully marked car as maintenance: ID={}, License Plate={}", result.getId(), result.getLicensePlate());
        return result;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "cars", allEntries = true),
        @CacheEvict(value = "car-status-counts", allEntries = true),
        @CacheEvict(value = "car-statistics", allEntries = true)
    })
    public CarResponse markAsAvailable(Long id) {
        log.debug("Marking car as available with id: {}", id);
        validateCarId(id);

        Car car = findCarById(id);
        car.markAsAvailable();
        car.setUpdateTime(LocalDateTime.now());

        Car savedCar = carRepository.save(car);
        CarResponse result = carMapper.toDto(savedCar);

        log.info("Successfully marked car as available: ID={}, License Plate={}", result.getId(), result.getLicensePlate());
        return result;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "cars", allEntries = true),
        @CacheEvict(value = "car-status-counts", allEntries = true),
        @CacheEvict(value = "car-statistics", allEntries = true)
    })
    public CarResponse updateCarStatus(Long id, CarStatusUpdateRequest statusUpdateRequest) {
        log.debug("Updating car status for id: {} to {}", id, statusUpdateRequest.carStatusType());
        validateCarId(id);

        Car car = findCarById(id);
        CarStatusType oldStatus = car.getCarStatusType();
        CarStatusType newStatus = statusUpdateRequest.carStatusType();

        validateStatusTransition(car.getCarStatusType(), newStatus);

        car.setCarStatusType(newStatus);
        car.setUpdateTime(LocalDateTime.now());

        Car savedCar = carRepository.save(car);
        CarResponse result = carMapper.toDto(savedCar);

        log.info("Successfully updated car status: ID={}, {} -> {}", result.getId(), oldStatus, newStatus);
        return result;
    }

    @Override
    @Transactional
    @CacheEvict(value = "cars", allEntries = true)
    public CarResponse updateCarPrice(Long id, CarPriceUpdateRequest priceUpdateRequest) {
        log.debug("Updating car price for id: {}", id);
        validateCarId(id);

        Car car = findCarById(id);
        BigDecimal oldPrice = car.getPrice();
        car.setPrice(priceUpdateRequest.price());
        car.setCurrencyType(priceUpdateRequest.currencyType());
        car.setDamagePrice(priceUpdateRequest.damagePrice());
        car.setUpdateTime(LocalDateTime.now());

        Car savedCar = carRepository.save(car);
        CarResponse result = carMapper.toDto(savedCar);

        log.info("Successfully updated car price: ID={}, {} -> {}", result.getId(), oldPrice, result.getPrice());
        return result;
    }

    @Override
    @Cacheable(value = "car-statistics", key = "'all'")
    public Map<String, Object> getCarStatistics() {
        log.debug("Getting car statistics");

        Map<String, Object> statistics = new HashMap<>();

        statistics.put("totalCars", carRepository.count());
        statistics.put("activeCars", carRepository.countByIsDeletedFalse());

        statistics.put("statusCounts", getCarsCountByStatus());

        statistics.put("brandCounts", getCarsCountByBrand());

        statistics.put("averagePrice", carRepository.getAveragePrice());
        statistics.put("minPrice", carRepository.getMinPrice());
        statistics.put("maxPrice", carRepository.getMaxPrice());

        log.info("Successfully retrieved car statistics");
        return statistics;
    }

    @Override
    @Cacheable(value = "car-status-counts", key = "'all'")
    public Map<String, Long> getCarsCountByStatus() {
        log.debug("Getting cars count by status");

        Map<String, Long> statusCounts = new HashMap<>();
        for (CarStatusType status : CarStatusType.values()) {
            long count = carRepository.countByCarStatusTypeAndIsDeletedFalse(status);
            statusCounts.put(status.name(), count);
        }

        log.info("Successfully retrieved cars count by status");
        return statusCounts;
    }

    @Override
    @Cacheable(value = "car-brand-counts", key = "'all'")
    public Map<String, Long> getCarsCountByBrand() {
        log.debug("Getting cars count by brand");

        List<Object[]> results = carRepository.getCarsCountByBrand();
        Map<String, Long> brandCounts = new HashMap<>();

        for (Object[] result : results) {
            String brand = (String) result[0];
            Long count = (Long) result[1];
            brandCounts.put(brand, count);
        }

        log.info("Successfully retrieved cars count by brand: {} brands found", brandCounts.size());
        return brandCounts;
    }

    @Override
    @Cacheable(value = "car-average-prices", key = "'all'")
    public Map<String, BigDecimal> getAveragePriceByBrand() {
        log.debug("Getting average price by brand");

        List<Object[]> results = carRepository.getAveragePriceByBrand();
        Map<String, BigDecimal> averagePrices = new HashMap<>();

        for (Object[] result : results) {
            String brand = (String) result[0];
            BigDecimal averagePrice = (BigDecimal) result[1];
            averagePrices.put(brand, averagePrice);
        }

        log.info("Successfully retrieved average price by brand: {} brands found", averagePrices.size());
        return averagePrices;
    }

    @Override
    @Cacheable(value = "most-viewed-cars", key = "#limit")
    public List<CarSummaryResponse> getMostViewedCars(int limit) {
        log.debug("Getting most viewed cars with limit: {}", limit);

        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "viewCount"));
        Page<Car> cars = carRepository.findByIsDeletedFalseOrderByViewCountDesc(pageable);

        List<CarSummaryResponse> result = cars.getContent().stream()
                .map(carMapper::toSummaryDto)
                .collect(Collectors.toList());

        log.info("Successfully retrieved {} most viewed cars", result.size());
        return result;
    }

    @Override
    @Cacheable(value = "most-liked-cars", key = "#limit")
    public List<CarSummaryResponse> getMostLikedCars(int limit) {
        log.debug("Getting most liked cars with limit: {}", limit);

        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "likeCount"));
        Page<Car> cars = carRepository.findByIsDeletedFalseOrderByLikeCountDesc(pageable);

        List<CarSummaryResponse> result = cars.getContent().stream()
                .map(carMapper::toSummaryDto)
                .collect(Collectors.toList());

        log.info("Successfully retrieved {} most liked cars", result.size());
        return result;
    }

    @Override
    public boolean existsByLicensePlate(String licensePlate) {
        return carRepository.existsByLicensePlate(licensePlate);
    }

    @Override
    @Transactional
    @CacheEvict(value = "cars", allEntries = true)
    public void incrementViewCount(Long id) {
        log.debug("Incrementing view count for car id: {}", id);
        validateCarId(id);

        Car car = findCarById(id);
        car.incrementViewCount();
        car.setUpdateTime(LocalDateTime.now());

        carRepository.save(car);
        log.info("Successfully incremented view count for car: ID={}", id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "cars", allEntries = true)
    public void incrementLikeCount(Long id) {
        log.debug("Incrementing like count for car id: {}", id);
        validateCarId(id);

        Car car = findCarById(id);
        car.incrementLikeCount();
        car.setUpdateTime(LocalDateTime.now());

        carRepository.save(car);
        log.info("Successfully incremented like count for car: ID={}", id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "cars", allEntries = true)
    public void decrementLikeCount(Long id) {
        log.debug("Decrementing like count for car id: {}", id);
        validateCarId(id);

        Car car = findCarById(id);
        car.decrementLikeCount();
        car.setUpdateTime(LocalDateTime.now());

        carRepository.save(car);
        log.info("Successfully decremented like count for car: ID={}", id);
    }

    @Override
    public List<String> validateCarData(CarRequest carRequest) {
        List<String> errors = new ArrayList<>();

        if (!StringUtils.hasText(carRequest.getLicensePlate())) {
            errors.add("License plate is required");
        }

        if (!StringUtils.hasText(carRequest.getBrand())) {
            errors.add("Brand is required");
        }

        if (!StringUtils.hasText(carRequest.getModel())) {
            errors.add("Model is required");
        }

        if (carRequest.getProductionYear() == null) {
            errors.add("Production year is required");
        }

        if (carRequest.getPrice() == null) {
            errors.add("Price is required");
        }

        if (carRequest.getCurrencyType() == null) {
            errors.add("Currency type is required");
        }

        if (carRequest.getCarStatusType() == null) {
            errors.add("Car status type is required");
        }

        if (carRequest.getProductionYear() != null && carRequest.getProductionYear() > LocalDate.now().getYear()) {
            errors.add("Production year cannot be in the future");
        }

        if (carRequest.getPrice() != null && carRequest.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Price must be positive");
        }

        return errors;
    }

    @Override
    public boolean canCarBeSold(Long id) {
        Car car = findCarById(id);
        return car.canBeSold();
    }

    @Override
    public boolean canCarBeReserved(Long id) {
        Car car = findCarById(id);
        return car.canBeReserved();
    }

    @Override
    @Cacheable(value = "cars", key = "'search:' + #searchTerm + ':' + #brand + ':' + #model + ':' + #pageable.pageNumber")
    public Page<CarResponse> searchCarsByCriteria(String searchTerm, String brand, String model,
                                                  BigDecimal minPrice, BigDecimal maxPrice,
                                                  CarStatusType status, Pageable pageable) {
        log.debug("Searching cars - term: {}, brand: {}, model: {}, price: {}-{}, status: {}",
                searchTerm, brand, model, minPrice, maxPrice, status);

        Page<Car> cars = carRepository.findCarsByCriteria(
                searchTerm, brand, model, null, null, null, null, null, null, minPrice, maxPrice, null, status, pageable
        );
        Page<CarResponse> result = cars.map(carMapper::toDto);

        log.info("Found {} cars matching search criteria", result.getTotalElements());
        return result;
    }


    @Override
    public int countByStatus(CarStatusType status) {
        return (int) carRepository.countByCarStatusTypeAndIsDeletedFalse(status);
    }

    @Override
    public int countTotalActiveCars() {
        return (int) carRepository.countByIsDeletedFalse();
    }


    private void logCarRetrievalSuccess(CarResponse result) {
        log.info("Successfully retrieved car (ID: {}, Plate: {})",
                result.getId(), result.getLicensePlate());
    }

    private void logCarCreationSuccess(CarResponse result) {
        log.info("Successfully created car: ID={}, Plate={}",
                result.getId(), result.getLicensePlate());
    }

    private void logCarUpdateSuccess(CarResponse result) {
        log.info("Successfully updated car: ID={}, Plate={}",
                result.getId(), result.getLicensePlate());
    }


    private void logPagedRetrievalSuccess(String entityType, Page<?> result) {
        log.info("Successfully retrieved {} {}. Page {}/{}",
                result.getNumberOfElements(), entityType,
                result.getNumber() + 1, result.getTotalPages());
    }

    private void logSearchSuccess(CarListResponse result) {
        log.info("Successfully found {} cars matching search criteria. Page {}/{}",
                result.numberOfElements(), result.currentPage() + 1, result.totalPages());
    }



    private void validateCarId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid car ID: " + id);
        }
    }

    private void validateLicensePlate(String licensePlate) {
        if (!StringUtils.hasText(licensePlate)) {
            throw new IllegalArgumentException("License plate cannot be null or empty");
        }
    }

    private void validateCarRequest(CarRequest carRequest) {
        if (carRequest == null) {
            throw new IllegalArgumentException("Car request cannot be null");
        }

        List<String> errors = validateCarData(carRequest);
        if (!errors.isEmpty()) {
            throw new CarValidationException("Car validation failed: " + String.join(", ", errors));
        }
    }

    private void checkLicensePlateUniqueness(String licensePlate) {
        if (carRepository.existsByLicensePlate(licensePlate)) {
            throw new CarAlreadyExistsException("Car already exists with license plate: " + licensePlate);
        }
    }

    private void checkVinNumberUniqueness(String vinNumber) {
        if (carRepository.existsByVinNumber(vinNumber)) {
            throw new CarAlreadyExistsException("Car already exists with vinNumber: " + vinNumber);
        }
    }

    private Car findCarById(Long id) {
        return carRepository.findById(id)
                .orElseThrow(() -> new CarNotFoundException("Car not found with id: " + id));
    }

    private Car findCarByLicensePlate(String licensePlate) {
        return carRepository.findByLicensePlate(licensePlate)
                .orElseThrow(() -> new CarNotFoundException("Car not found with license plate: " + licensePlate));
    }

    private void validateStatusTransition(CarStatusType currentStatus, CarStatusType newStatus) {

        Map<CarStatusType, Set<CarStatusType>> validTransitions = Map.of(
                CarStatusType.AVAILABLE, Set.of(CarStatusType.RESERVED, CarStatusType.MAINTENANCE, CarStatusType.DAMAGED, CarStatusType.INSPECTION, CarStatusType.SOLD),
                CarStatusType.RESERVED, Set.of(CarStatusType.AVAILABLE, CarStatusType.SOLD),
                CarStatusType.MAINTENANCE, Set.of(CarStatusType.AVAILABLE, CarStatusType.DAMAGED),
                CarStatusType.DAMAGED, Set.of(CarStatusType.AVAILABLE, CarStatusType.MAINTENANCE),
                CarStatusType.INSPECTION, Set.of(CarStatusType.AVAILABLE, CarStatusType.MAINTENANCE),
                CarStatusType.SOLD, Set.of()
        );

        Set<CarStatusType> allowedTransitions = validTransitions.get(currentStatus);
        if (allowedTransitions == null || !allowedTransitions.contains(newStatus)) {
            throw new InvalidStatusTransitionException(
                    String.format("Invalid status transition from %s to %s", currentStatus, newStatus)
            );
        }
    }

    private Pageable buildPageable(CarSearchRequest searchRequest) {
        Sort sort = Sort.by(Sort.Direction.fromString(searchRequest.getSortDirection()), searchRequest.getSortBy());
        return PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);
    }
}