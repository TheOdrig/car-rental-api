package com.akif.car.internal.service;

import com.akif.car.api.CarCatalog;
import com.akif.car.api.CarDto;
import com.akif.car.api.CarResponse;
import com.akif.car.api.CarStatusType;
import com.akif.car.domain.Car;
import com.akif.car.internal.dto.request.CarSearchRequest;
import com.akif.shared.web.PageResponse;
import com.akif.car.internal.exception.CarValidationException;
import com.akif.car.internal.mapper.CarMapper;
import com.akif.car.internal.repository.CarRepository;
import com.akif.shared.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CarQueryService implements CarCatalog {

    private final Clock clock;
    private final CarRepository carRepository;
    private final CarMapper carMapper;
    private final CarLookup carLookup;

    @Override
    @Cacheable(value = CacheConfig.CARS_CACHE, key = "'dto:' + #id")
    public CarDto getCarDtoById(Long id) {
        log.debug("Getting minimal CarDto for cross-module usage, id: {}", id);
        CarDto result = carMapper.toCarDto(carLookup.requireAnyById(id));
        log.debug("Successfully retrieved CarDto for id: {}", id);
        return result;
    }

    @Override
    public CarDto getCarForBooking(Long id) {
        log.debug("Reading car {} for booking straight from the database", id);
        return carMapper.toCarDto(carLookup.requireActiveById(id));
    }

    @Cacheable(value = CacheConfig.CARS_CACHE, key = "#id")
    public CarResponse getCarById(Long id) {
        log.debug("Getting car by id: {}", id);
        CarResponse result = carMapper.toDto(carLookup.requireActiveById(id));
        logCarRetrievalSuccess(result);
        return result;
    }

    @Cacheable(value = CacheConfig.CARS_CACHE, key = "'licensePlate:' + #licensePlate")
    public CarResponse getCarByLicensePlate(String licensePlate) {
        log.debug("Getting car by license plate: {}", licensePlate);
        CarResponse result = carMapper.toDto(carLookup.requireActiveByLicensePlate(licensePlate));
        logCarRetrievalSuccess(result);
        return result;
    }

    private static final Map<String, String> SEARCH_SORT_COLUMNS = Map.of(
            "id", "id",
            "licensePlate", "license_plate",
            "brand", "brand",
            "model", "model",
            "productionYear", "production_year",
            "price", "price",
            "createTime", "create_time",
            "rating", "rating");

    private static Sort defaultSearchSort() {
        return Sort.by(new Sort.Order(Sort.Direction.DESC, "create_time").nullsLast(),
                new Sort.Order(Sort.Direction.DESC, "id"));
    }

    private static Sort searchSort(CarSearchRequest request) {
        String requested = request.getSortBy();
        if (requested == null) {
            return defaultSearchSort();
        }
        String column = SEARCH_SORT_COLUMNS.get(requested);
        if (column == null) {
            throw new CarValidationException("Invalid sort field: " + requested);
        }
        Sort.Direction direction = "desc".equalsIgnoreCase(request.getSortDirection())
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort.Order primary = new Sort.Order(direction, column).nullsLast();
        if ("id".equals(column)) {
            return Sort.by(primary);
        }
        return Sort.by(primary, new Sort.Order(direction, "id"));
    }

    @Cacheable(value = CacheConfig.CARS_CACHE, key = "'search:' + #searchRequest.toString()")
    public PageResponse<CarResponse> searchCars(CarSearchRequest searchRequest) {
        log.debug("Searching cars with criteria: {}", searchRequest);

        int pageNumber = searchRequest.getPage() != null ? searchRequest.getPage() : 0;
        int pageSize = searchRequest.getSize() != null ? searchRequest.getSize() : 20;
        Pageable pageable = PageRequest.of(pageNumber, pageSize, searchSort(searchRequest));

        String searchTerm = containsPattern(searchRequest.getSearchTerm());
        String brand = exactPattern(searchRequest.getBrand());
        String model = exactPattern(searchRequest.getModel());
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
                searchRequest.getMaxSeats(),
                searchRequest.getMinDoors(),
                searchRequest.getMaxKilometer(),
                searchRequest.getMinRating(),
                lowerCaseOrNull(searchRequest.getEngineType()),
                lowerCaseOrNull(searchRequest.getColor()),
                searchRequest.getMinProductionYear(),
                searchRequest.getMaxProductionYear(),
                searchRequest.getMinPrice(),
                searchRequest.getMaxPrice(),
                currencyType,
                carStatusType,
                pageable);

        PageResponse<CarResponse> result = PageResponse.of(cars.map(carMapper::toDto));

        log.info("Successfully found {} cars matching search criteria. Page {}/{}",
                result.totalElements(), result.page() + 1, result.totalPages());
        return result;
    }

    @Cacheable(value = CacheConfig.CARS_CACHE, key = "'all:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<CarResponse> getAllCars(Pageable pageable) {
        log.debug("Getting all cars with pageable: {}", pageable);
        Page<CarResponse> result = carRepository.findByIsDeletedFalse(pageable).map(carMapper::toDto);
        logPagedRetrievalSuccess("cars", result);
        return result;
    }

    @Cacheable(value = CacheConfig.CARS_CACHE, key = "'status:' + #status + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<CarResponse> getCarsByStatus(String status, Pageable pageable) {
        log.debug("Getting cars by status: {}", status);
        CarStatusType statusType = parseStatus(status);
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

    @Cacheable(value = CacheConfig.CARS_CACHE,
            key = "T(java.util.Arrays).asList('criteria', #searchTerm, #brand, #model, #minPrice, #maxPrice, #status, #page, #size)")
    public Page<CarResponse> searchCarsByCriteria(String searchTerm, String brand, String model,
                                                  BigDecimal minPrice, BigDecimal maxPrice,
                                                  CarStatusType status, int page, int size) {
        log.debug("Searching cars - term: {}, brand: {}, model: {}, price: {}-{}, status: {}",
                searchTerm, brand, model, minPrice, maxPrice, status);

        Pageable pageable = PageRequest.of(page, size, defaultSearchSort());
        Page<CarResponse> result = carRepository.findCarsByCriteria(
                containsPattern(searchTerm), containsPattern(brand), containsPattern(model),
                null, null, null, null, null, null, null, null, null, null, null, null,
                minPrice, maxPrice, null, status != null ? status.name() : null,
                pageable).map(carMapper::toDto);

        log.info("Found {} cars matching search criteria", result.getTotalElements());
        return result;
    }

    @Override
    public Map<Long, CarStatusType> findActiveCarStatuses() {
        Map<Long, CarStatusType> statuses = new HashMap<>();
        for (Object[] row : carRepository.findActiveCarStatusRows()) {
            statuses.put((Long) row[0], (CarStatusType) row[1]);
        }
        return statuses;
    }

    private static String lowerCaseOrNull(String value) {
        return value != null ? value.trim().toLowerCase() : null;
    }

    private static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private static String exactPattern(String value) {
        String normalized = lowerCaseOrNull(value);
        return normalized == null || normalized.isEmpty() ? null : escapeLike(normalized);
    }

    private static String containsPattern(String value) {
        String normalized = lowerCaseOrNull(value);
        return normalized == null || normalized.isEmpty() ? null : "%" + escapeLike(normalized) + "%";
    }

    private static CarStatusType parseStatus(String status) {
        try {
            return CarStatusType.fromString(status);
        } catch (IllegalArgumentException e) {
            throw new CarValidationException(e.getMessage());
        }
    }

    private void logCarRetrievalSuccess(CarResponse result) {
        log.info("Successfully retrieved car (ID: {}, Plate: {})", result.getId(), result.getLicensePlate());
    }

    private void logPagedRetrievalSuccess(String entityType, Page<?> result) {
        log.info("Successfully retrieved {} {}. Page {}/{}",
                result.getNumberOfElements(), entityType, result.getNumber() + 1, result.getTotalPages());
    }
}
