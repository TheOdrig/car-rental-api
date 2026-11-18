package com.akif.car.internal.service;

import com.akif.car.api.CarDto;
import com.akif.car.api.CarResponse;
import com.akif.car.api.CarStatusType;
import com.akif.car.domain.Car;
import com.akif.car.internal.dto.request.CarSearchRequest;
import com.akif.car.internal.dto.response.CarListResponse;
import com.akif.car.internal.mapper.CarMapper;
import com.akif.car.internal.repository.CarRepository;
import com.akif.shared.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CarQueryService {

    private final Clock clock;
    private final CarRepository carRepository;
    private final CarMapper carMapper;
    private final CarLookup carLookup;

    @Cacheable(value = CacheConfig.CARS_CACHE, key = "'dto:' + #id")
    public CarDto getCarDtoById(Long id) {
        log.debug("Getting minimal CarDto for cross-module usage, id: {}", id);
        CarDto result = carMapper.toCarDto(carLookup.requireById(id));
        log.debug("Successfully retrieved CarDto for id: {}", id);
        return result;
    }

    public CarResponse getCarForBooking(Long id) {
        log.debug("Reading car {} for booking straight from the database", id);
        return carMapper.toDto(carLookup.requireById(id));
    }

    @Cacheable(value = CacheConfig.CARS_CACHE, key = "#id")
    public CarResponse getCarById(Long id) {
        log.debug("Getting car by id: {}", id);
        CarResponse result = carMapper.toDto(carLookup.requireById(id));
        logCarRetrievalSuccess(result);
        return result;
    }

    @Cacheable(value = CacheConfig.CARS_CACHE, key = "'licensePlate:' + #licensePlate")
    public CarResponse getCarByLicensePlate(String licensePlate) {
        log.debug("Getting car by license plate: {}", licensePlate);
        CarResponse result = carMapper.toDto(carLookup.requireByLicensePlate(licensePlate));
        logCarRetrievalSuccess(result);
        return result;
    }

    @Cacheable(value = CacheConfig.CARS_CACHE, key = "'search:' + #searchRequest.toString()")
    public CarListResponse searchCars(CarSearchRequest searchRequest) {
        log.debug("Searching cars with criteria: {}", searchRequest);

        Pageable pageable = PageRequest.of(searchRequest.getPage(), searchRequest.getSize());

        String searchTerm = searchRequest.getSearchTerm() != null
                ? "%" + searchRequest.getSearchTerm().trim().toLowerCase() + "%"
                : null;
        String brand = lowerCaseOrNull(searchRequest.getBrand());
        String model = lowerCaseOrNull(searchRequest.getModel());
        String transmissionType = lowerCaseOrNull(searchRequest.getTransmissionType());
        String bodyType = lowerCaseOrNull(searchRequest.getBodyType());
        String fuelType = lowerCaseOrNull(searchRequest.getFuelType());
        String currencyType = searchRequest.getCurrencyType() != null ? searchRequest.getCurrencyType().name() : null;
        String carStatusType = searchRequest.getCarStatusType() != null ? searchRequest.getCarStatusType().name()
                : null;

        Page<Car> cars = carRepository.findCarsByCriteria(
                searchTerm,
                brand,
                model,
                transmissionType,
                bodyType,
                fuelType,
                searchRequest.getMinSeats(),
                searchRequest.getMinProductionYear(),
                searchRequest.getMaxProductionYear(),
                searchRequest.getMinPrice(),
                searchRequest.getMaxPrice(),
                currencyType,
                carStatusType,
                pageable);

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
                cars.getNumberOfElements());

        log.info("Successfully found {} cars matching search criteria. Page {}/{}",
                result.numberOfElements(), result.currentPage() + 1, result.totalPages());
        return result;
    }

    @Cacheable(value = CacheConfig.CARS_CACHE, key = "'all:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<CarResponse> getAllCars(Pageable pageable) {
        log.debug("Getting all cars with pageable: {}", pageable);
        Page<CarResponse> result = carRepository.findAll(pageable).map(carMapper::toDto);
        logPagedRetrievalSuccess("cars", result);
        return result;
    }

    @Cacheable(value = CacheConfig.CARS_CACHE, key = "'status:' + #status + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<CarResponse> getCarsByStatus(String status, Pageable pageable) {
        log.debug("Getting cars by status: {}", status);
        CarStatusType statusType = CarStatusType.fromString(status);
        Page<CarResponse> result = carRepository.findByCarStatusTypeAndIsDeletedFalse(statusType, pageable)
                .map(carMapper::toDto);
        logPagedRetrievalSuccess("cars", result);
        return result;
    }

    public long getCarCount() {
        return carRepository.count();
    }

    @Cacheable(value = CacheConfig.CARS_CACHE, key = "'brand:' + #brand + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<CarResponse> getCarsByBrand(String brand, Pageable pageable) {
        log.debug("Getting cars by brand: {}", brand);
        Page<CarResponse> result = carRepository.findByBrandIgnoreCaseAndIsDeletedFalse(brand, pageable)
                .map(carMapper::toDto);
        log.info("Successfully retrieved {} cars with brand: {}", result.getTotalElements(), brand);
        return result;
    }

    @Cacheable(value = CacheConfig.CARS_CACHE, key = "'price:' + #minPrice + ':' + #maxPrice + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<CarResponse> getCarsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        log.debug("Getting cars by price range: {} - {}", minPrice, maxPrice);
        Page<CarResponse> result = carRepository.findByPriceBetweenAndIsDeletedFalse(minPrice, maxPrice, pageable)
                .map(carMapper::toDto);
        log.info("Successfully retrieved {} cars in price range: {} - {}", result.getTotalElements(), minPrice,
                maxPrice);
        return result;
    }

    @Cacheable(value = CacheConfig.CARS_CACHE, key = "'new:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<CarResponse> getNewCars(Pageable pageable) {
        log.debug("Getting new cars");
        LocalDate oneYearAgo = LocalDate.now(clock).minusYears(1);
        Page<CarResponse> result = carRepository
                .findByProductionYearGreaterThanEqualAndIsDeletedFalse(oneYearAgo.getYear(), pageable)
                .map(carMapper::toDto);
        logPagedRetrievalSuccess("new cars", result);
        return result;
    }

    @Cacheable(value = CacheConfig.CARS_CACHE, key = "'active:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<CarResponse> getAllActiveCars(Pageable pageable) {
        log.debug("Getting all active cars with pageable: {}", pageable);
        Page<CarResponse> result = carRepository.findByIsDeletedFalse(pageable).map(carMapper::toDto);
        logPagedRetrievalSuccess("active cars", result);
        return result;
    }

    public long getActiveCarCount() {
        return carRepository.countByIsDeletedFalse();
    }

    public boolean existsByLicensePlate(String licensePlate) {
        return carRepository.existsByLicensePlate(licensePlate);
    }

    @Cacheable(value = CacheConfig.CARS_CACHE, key = "'search:' + #searchTerm + ':' + #brand + ':' + #model + ':' + #pageable.pageNumber")
    public Page<CarResponse> searchCarsByCriteria(String searchTerm, String brand, String model,
                                                  BigDecimal minPrice, BigDecimal maxPrice,
                                                  CarStatusType status, Pageable pageable) {
        log.debug("Searching cars - term: {}, brand: {}, model: {}, price: {}-{}, status: {}",
                searchTerm, brand, model, minPrice, maxPrice, status);

        String statusStr = status != null ? status.name() : null;
        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<CarResponse> result = carRepository.findCarsByCriteria(
                searchTerm, brand, model, null, null, null, null, null, null, minPrice, maxPrice, null, statusStr,
                unsortedPageable).map(carMapper::toDto);

        log.info("Found {} cars matching search criteria", result.getTotalElements());
        return result;
    }

    public int countByStatus(CarStatusType status) {
        return (int) carRepository.countByCarStatusTypeAndIsDeletedFalse(status);
    }

    public int countTotalActiveCars() {
        return (int) carRepository.countByIsDeletedFalse();
    }

    public java.util.Map<Long, CarStatusType> findActiveCarStatuses() {
        java.util.Map<Long, CarStatusType> statuses = new java.util.HashMap<>();
        for (Object[] row : carRepository.findActiveCarStatusRows()) {
            statuses.put((Long) row[0], (CarStatusType) row[1]);
        }
        return statuses;
    }

    private String lowerCaseOrNull(String value) {
        return value != null ? value.trim().toLowerCase() : null;
    }

    private void logCarRetrievalSuccess(CarResponse result) {
        log.info("Successfully retrieved car (ID: {}, Plate: {})", result.getId(), result.getLicensePlate());
    }

    private void logPagedRetrievalSuccess(String entityType, Page<?> result) {
        log.info("Successfully retrieved {} {}. Page {}/{}",
                result.getNumberOfElements(), entityType, result.getNumber() + 1, result.getTotalPages());
    }
}
