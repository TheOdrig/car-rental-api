package com.akif.service.impl;

import com.akif.dto.request.CarPriceUpdateRequestDto;
import com.akif.dto.request.CarRequestDto;
import com.akif.dto.request.CarSearchRequestDto;
import com.akif.dto.request.CarStatusUpdateRequestDto;
import com.akif.dto.response.CarListResponseDto;
import com.akif.dto.response.CarResponseDto;
import com.akif.dto.response.CarSummaryResponseDto;
import com.akif.enums.CarStatusType;
import com.akif.exception.*;
import com.akif.mapper.CarMapper;
import com.akif.model.Car;
import com.akif.repository.CarRepository;
import com.akif.service.ICarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
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
public class CarServiceImpl implements ICarService {

    private final CarRepository carRepository;
    private final CarMapper carMapper;


    @Override
    @Cacheable(value = "cars", key = "#id")
    public CarResponseDto getCarById(Long id) {
        log.debug("Getting car by id: {}", id);
        validateCarId(id);

        Car car = findCarById(id);
        CarResponseDto result = carMapper.toDto(car);

        logCarRetrievalSuccess(result);
        return result;
    }

    @Override
    @Cacheable(value = "cars", key = "'licensePlate:' + #licensePlate")
    public CarResponseDto getCarByLicensePlate(String licensePlate) {
        log.debug("Getting car by license plate: {}", licensePlate);
        validateLicensePlate(licensePlate);

        Car car = findCarByLicensePlate(licensePlate);
        CarResponseDto result = carMapper.toDto(car);

        logCarRetrievalSuccess(result);
        return result;
    }

    @Override
    @Transactional
    @CacheEvict(value = "cars", allEntries = true)
    public CarResponseDto createCar(CarRequestDto carRequest) {
        log.debug("Creating new car with license plate: {}", carRequest.getLicensePlate());

        validateCarRequest(carRequest);
        checkLicensePlateUniqueness(carRequest.getLicensePlate());

        Car car = carMapper.toEntity(carRequest);
        car.setCreateTime(LocalDateTime.now());
        car.setIsDeleted(false);

        Car savedCar = carRepository.save(car);
        CarResponseDto result = carMapper.toDto(savedCar);

        logCarCreationSuccess(result);
        return result;
    }

    @Override
    @Transactional
    @CacheEvict(value = "cars", allEntries = true)
    public CarResponseDto updateCar(Long id, CarRequestDto carRequest) {
        log.debug("Updating car with id: {}", id);

        validateCarId(id);
        validateCarRequest(carRequest);

        Car existingCar = findCarById(id);

        if (!existingCar.getLicensePlate().equals(carRequest.getLicensePlate())) {
            checkLicensePlateUniqueness(carRequest.getLicensePlate());
        }

        carMapper.updateEntity(carRequest, existingCar);
        existingCar.setUpdateTime(LocalDateTime.now());

        Car updatedCar = carRepository.save(existingCar);
        CarResponseDto result = carMapper.toDto(updatedCar);

        logCarUpdateSuccess(result);
        return result;
    }

    @Override
    @Transactional
    @CacheEvict(value = "cars", allEntries = true)
    public void deleteCar(Long id) {
        log.debug("Hard deleting car with id: {}", id);
        validateCarId(id);

        Car car = findCarById(id);
        carRepository.delete(car);

        log.info("Successfully deleted car: ID={}", id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "cars", allEntries = true)
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
    @CacheEvict(value = "cars", allEntries = true)
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
    public CarListResponseDto searchCars(CarSearchRequestDto searchRequest) {
        log.debug("Searching cars with criteria: {}", searchRequest);

        Pageable pageable = buildPageable(searchRequest);

        Page<Car> cars = carRepository.findCarsByCriteria(
                searchRequest.getSearchTerm(),
                searchRequest.getBrand(),
                searchRequest.getModel(),
                searchRequest.getMinProductionYear(),
                searchRequest.getMaxProductionYear(),
                searchRequest.getMinPrice(),
                searchRequest.getMaxPrice(),
                searchRequest.getCurrencyType(),
                searchRequest.getCarStatusType(),
                pageable
        );

        List<CarResponseDto> carDtos = cars.getContent().stream()
                .map(carMapper::toDto)
                .collect(Collectors.toList());

        CarListResponseDto result = CarListResponseDto.builder()
                .cars(carDtos)
                .totalElements(cars.getTotalElements())
                .totalPages(cars.getTotalPages())
                .currentPage(cars.getNumber())
                .pageSize(cars.getSize())
                .isFirst(cars.isFirst())
                .isLast(cars.isLast())
                .hasNext(cars.hasNext())
                .hasPrevious(cars.hasPrevious())
                .numberOfElements(cars.getNumberOfElements())
                .build();

        logSearchSuccess(result);
        return result;
    }

    @Override
    @Cacheable(value = "cars", key = "'all:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<CarResponseDto> getAllCars(Pageable pageable) {
        log.debug("Getting all cars with pageable: {}", pageable);

        Page<Car> cars = carRepository.findAll(pageable);
        Page<CarResponseDto> result = cars.map(carMapper::toDto);

        logPagedRetrievalSuccess("cars", result);
        return result;
    }

    @Override
    @Cacheable(value = "cars", key = "'status:' + #status + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<CarResponseDto> getCarsByStatus(String status, Pageable pageable) {
        log.debug("Getting cars by status: {}", status);

        CarStatusType statusType = CarStatusType.fromString(status);
        Page<Car> cars = carRepository.findByCarStatusTypeAndIsDeletedFalse(statusType, pageable);
        Page<CarResponseDto> result = cars.map(carMapper::toDto);

        logPagedRetrievalSuccess("cars", result);
        return result;
    }

    @Override
    public long getCarCount() {
        return carRepository.count();
    }

    @Override
    @Cacheable(value = "cars", key = "'brand:' + #brand + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<CarResponseDto> getCarsByBrand(String brand, Pageable pageable) {
        log.debug("Getting cars by brand: {}", brand);

        Page<Car> cars = carRepository.findByBrandIgnoreCaseAndIsDeletedFalse(brand, pageable);
        Page<CarResponseDto> result = cars.map(carMapper::toDto);

        log.info("Successfully retrieved {} cars with brand: {}", result.getTotalElements(), brand);
        return result;
    }

    @Override
    @Cacheable(value = "cars", key = "'price:' + #minPrice + ':' + #maxPrice + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<CarResponseDto> getCarsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        log.debug("Getting cars by price range: {} - {}", minPrice, maxPrice);

        Page<Car> cars = carRepository.findByPriceBetweenAndIsDeletedFalse(minPrice, maxPrice, pageable);
        Page<CarResponseDto> result = cars.map(carMapper::toDto);

        log.info("Successfully retrieved {} cars in price range: {} - {}", result.getTotalElements(), minPrice, maxPrice);
        return result;
    }

    @Override
    @Cacheable(value = "cars", key = "'new:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<CarResponseDto> getNewCars(Pageable pageable) {
        log.debug("Getting new cars");

        LocalDate oneYearAgo = LocalDate.now().minusYears(1);
        Page<Car> cars = carRepository.findByProductionYearGreaterThanEqualAndIsDeletedFalse(oneYearAgo.getYear(), pageable);
        Page<CarResponseDto> result = cars.map(carMapper::toDto);

        logPagedRetrievalSuccess("new cars", result);
        return result;
    }

    @Override
    @Cacheable(value = "cars", key = "'featured:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<CarResponseDto> getFeaturedCars(Pageable pageable) {
        log.debug("Getting featured cars");

        Page<Car> cars = carRepository.findByIsFeaturedTrueAndIsDeletedFalse(pageable);
        Page<CarResponseDto> result = cars.map(carMapper::toDto);

        logPagedRetrievalSuccess("featured cars", result);
        return result;
    }

    @Override
    @Cacheable(value = "cars", key = "'testdrive:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<CarResponseDto> getCarsAvailableForTestDrive(Pageable pageable) {
        log.debug("Getting cars available for test drive");

        Page<Car> cars = carRepository.findByIsTestDriveAvailableTrueAndIsDeletedFalse(pageable);
        Page<CarResponseDto> result = cars.map(carMapper::toDto);

        logPagedRetrievalSuccess("cars available for test drive", result);
        return result;
    }

    @Override
    @Cacheable(value = "cars", key = "'active:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<CarResponseDto> getAllActiveCars(Pageable pageable) {
        log.debug("Getting all active cars with pageable: {}", pageable);

        Page<Car> cars = carRepository.findByIsDeletedFalse(pageable);
        Page<CarResponseDto> result = cars.map(carMapper::toDto);

        logPagedRetrievalSuccess("active cars", result);
        return result;
    }

    @Override
    public long getActiveCarCount() {
        return carRepository.countByIsDeletedFalse();
    }

    @Override
    @Transactional
    @CacheEvict(value = "cars", allEntries = true)
    public CarResponseDto sellCar(Long id) {
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
        CarResponseDto result = carMapper.toDto(savedCar);

        log.info("Successfully sold car: ID={}, License Plate={}", result.getId(), result.getLicensePlate());
        return result;
    }

    @Override
    @Transactional
    @CacheEvict(value = "cars", allEntries = true)
    public CarResponseDto reserveCar(Long id) {
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
        CarResponseDto result = carMapper.toDto(savedCar);

        log.info("Successfully reserved car: ID={}, License Plate={}", result.getId(), result.getLicensePlate());
        return result;
    }

    @Override
    @Transactional
    @CacheEvict(value = "cars", allEntries = true)
    public CarResponseDto cancelReservation(Long id) {
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
        CarResponseDto result = carMapper.toDto(savedCar);

        log.info("Successfully cancelled reservation for car: ID={}, License Plate={}", result.getId(), result.getLicensePlate());
        return result;
    }

    @Override
    @Transactional
    @CacheEvict(value = "cars", allEntries = true)
    public CarResponseDto markAsMaintenance(Long id) {
        log.debug("Marking car as maintenance with id: {}", id);
        validateCarId(id);

        Car car = findCarById(id);
        car.markAsMaintenance();
        car.setUpdateTime(LocalDateTime.now());

        Car savedCar = carRepository.save(car);
        CarResponseDto result = carMapper.toDto(savedCar);

        log.info("Successfully marked car as maintenance: ID={}, License Plate={}", result.getId(), result.getLicensePlate());
        return result;
    }

    @Override
    @Transactional
    @CacheEvict(value = "cars", allEntries = true)
    public CarResponseDto markAsAvailable(Long id) {
        log.debug("Marking car as available with id: {}", id);
        validateCarId(id);

        Car car = findCarById(id);
        car.markAsAvailable();
        car.setUpdateTime(LocalDateTime.now());

        Car savedCar = carRepository.save(car);
        CarResponseDto result = carMapper.toDto(savedCar);

        log.info("Successfully marked car as available: ID={}, License Plate={}", result.getId(), result.getLicensePlate());
        return result;
    }

    @Override
    @Transactional
    @CacheEvict(value = "cars", allEntries = true)
    public CarResponseDto updateCarStatus(Long id, CarStatusUpdateRequestDto statusUpdateRequest) {
        log.debug("Updating car status for id: {} to {}", id, statusUpdateRequest.getCarStatusType());
        validateCarId(id);

        Car car = findCarById(id);
        CarStatusType oldStatus = car.getCarStatusType();
        CarStatusType newStatus = statusUpdateRequest.getCarStatusType();

        validateStatusTransition(car.getCarStatusType(), newStatus);

        car.setCarStatusType(newStatus);
        car.setUpdateTime(LocalDateTime.now());

        Car savedCar = carRepository.save(car);
        CarResponseDto result = carMapper.toDto(savedCar);

        log.info("Successfully updated car status: ID={}, {} -> {}", result.getId(), oldStatus, newStatus);
        return result;
    }

    @Override
    @Transactional
    @CacheEvict(value = "cars", allEntries = true)
    public CarResponseDto updateCarPrice(Long id, CarPriceUpdateRequestDto priceUpdateRequest) {
        log.debug("Updating car price for id: {}", id);
        validateCarId(id);

        Car car = findCarById(id);
        BigDecimal oldPrice = car.getPrice();
        car.setPrice(priceUpdateRequest.getPrice());
        car.setCurrencyType(priceUpdateRequest.getCurrencyType());
        car.setDamagePrice(priceUpdateRequest.getDamagePrice());
        car.setUpdateTime(LocalDateTime.now());

        Car savedCar = carRepository.save(car);
        CarResponseDto result = carMapper.toDto(savedCar);

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
    public List<CarSummaryResponseDto> getMostViewedCars(int limit) {
        log.debug("Getting most viewed cars with limit: {}", limit);

        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "viewCount"));
        Page<Car> cars = carRepository.findByIsDeletedFalseOrderByViewCountDesc(pageable);

        List<CarSummaryResponseDto> result = cars.getContent().stream()
                .map(carMapper::toSummaryDto)
                .collect(Collectors.toList());

        log.info("Successfully retrieved {} most viewed cars", result.size());
        return result;
    }

    @Override
    @Cacheable(value = "most-liked-cars", key = "#limit")
    public List<CarSummaryResponseDto> getMostLikedCars(int limit) {
        log.debug("Getting most liked cars with limit: {}", limit);

        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "likeCount"));
        Page<Car> cars = carRepository.findByIsDeletedFalseOrderByLikeCountDesc(pageable);

        List<CarSummaryResponseDto> result = cars.getContent().stream()
                .map(carMapper::toSummaryDto)
                .collect(Collectors.toList());

        log.info("Successfully retrieved {} most liked cars", result.size());
        return result;
    }

    @Override
    public boolean existsById(Long id) {
        return carRepository.existsById(id);
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
    public List<String> validateCarData(CarRequestDto carRequest) {
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
    public Page<CarResponseDto> searchCarsByCriteria(String searchTerm, String brand, String model,
                                           BigDecimal minPrice, BigDecimal maxPrice,
                                           CarStatusType status, Pageable pageable) {
        log.debug("Searching cars - term: {}, brand: {}, model: {}, price: {}-{}, status: {}",
                searchTerm, brand, model, minPrice, maxPrice, status);

        Page<Car> cars = carRepository.findCarsByCriteria(
                searchTerm, brand, model, null, null, minPrice, maxPrice, null, status, pageable
        );
        Page<CarResponseDto> result = cars.map(carMapper::toDto);

        log.info("Found {} cars matching search criteria", result.getTotalElements());
        return result;
    }


    private void logCarRetrievalSuccess(CarResponseDto result) {
        log.info("Successfully retrieved car (ID: {}, Plate: {})",
                result.getId(), result.getLicensePlate());
    }

    private void logCarCreationSuccess(CarResponseDto result) {
        log.info("Successfully created car: ID={}, Plate={}",
                result.getId(), result.getLicensePlate());
    }

    private void logCarUpdateSuccess(CarResponseDto result) {
        log.info("Successfully updated car: ID={}, Plate={}",
                result.getId(), result.getLicensePlate());
    }


    private void logPagedRetrievalSuccess(String entityType, Page<?> result) {
        log.info("Successfully retrieved {} {}. Page {}/{}",
                result.getNumberOfElements(), entityType,
                result.getNumber() + 1, result.getTotalPages());
    }

    private void logSearchSuccess(CarListResponseDto result) {
        log.info("Successfully found {} cars matching search criteria. Page {}/{}",
                result.getNumberOfElements(), result.getCurrentPage() + 1, result.getTotalPages());
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

    private void validateCarRequest(CarRequestDto carRequest) {
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
            throw new CarAlreadyExistsException("Car with license plate " + licensePlate + " already exists");
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

    private Pageable buildPageable(CarSearchRequestDto searchRequest) {
        Sort sort = Sort.by(Sort.Direction.fromString(searchRequest.getSortDirection()), searchRequest.getSortBy());
        return PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);
    }
}