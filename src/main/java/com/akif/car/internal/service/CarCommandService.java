package com.akif.car.internal.service;

import com.akif.car.api.CarResponse;
import com.akif.car.domain.Car;
import com.akif.car.internal.cache.EvictCarCaches;
import com.akif.car.internal.dto.pricing.CarPriceUpdateRequest;
import com.akif.car.internal.dto.request.CarRequest;
import com.akif.car.internal.exception.CarAlreadyExistsException;
import com.akif.car.internal.exception.CarValidationException;
import com.akif.car.internal.mapper.CarMapper;
import com.akif.car.internal.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CarCommandService {

    private final Clock clock;
    private final CarRepository carRepository;
    private final CarMapper carMapper;
    private final CarLookup carLookup;

    @EvictCarCaches
    public CarResponse createCar(CarRequest submitted) {
        CarRequest carRequest = submitted == null ? null : submitted.normalized();
        log.debug("Creating new car");

        validateCarRequest(carRequest);
        checkLicensePlateUniqueness(carRequest.licensePlate());
        checkVinNumberUniqueness(carRequest.vinNumber());

        Car car = carMapper.toEntity(carRequest);
        car.setCreateTime(Instant.now(clock));
        car.setIsDeleted(false);

        CarResponse result = carMapper.toDto(carRepository.save(car));

        log.info("Successfully created car: ID={}, Plate={}", result.id(), result.licensePlate());
        return result;
    }

    @EvictCarCaches
    public CarResponse updateCar(Long id, CarRequest submitted) {
        CarRequest carRequest = submitted == null ? null : submitted.normalized();
        log.debug("Updating car with id: {}", id);

        carLookup.validateId(id);
        validateCarRequest(carRequest);

        Car existingCar = carLookup.requireActiveById(id);

        if (!existingCar.getLicensePlate().equals(carRequest.licensePlate())) {
            checkLicensePlateUniqueness(carRequest.licensePlate());
        }

        if (StringUtils.hasText(carRequest.vinNumber())
                && !Objects.equals(existingCar.getVinNumber(), carRequest.vinNumber())) {
            checkVinNumberUniqueness(carRequest.vinNumber());
        }

        carMapper.updateEntity(carRequest, existingCar);
        existingCar.setUpdateTime(Instant.now(clock));

        CarResponse result = carMapper.toDto(carRepository.save(existingCar));

        log.info("Successfully updated car: ID={}, Plate={}", result.id(), result.licensePlate());
        return result;
    }

    @EvictCarCaches
    public void deleteCar(Long id) {
        log.debug("Hard deleting car with id: {}", id);
        Car car = carLookup.requireAnyById(id);
        carRepository.delete(car);
        log.info("Successfully deleted car: ID={}", id);
    }

    @EvictCarCaches
    public void softDeleteCar(Long id) {
        log.debug("Soft deleting car with id: {}", id);
        Car car = carLookup.requireActiveById(id);
        car.softDelete();
        car.setUpdateTime(Instant.now(clock));
        carRepository.save(car);
        log.info("Successfully soft deleted car: ID={}", id);
    }

    @EvictCarCaches
    public CarResponse restoreCar(Long id) {
        log.debug("Restoring car with id: {}", id);
        Car car = carLookup.requireAnyById(id);
        car.restore();
        car.setUpdateTime(Instant.now(clock));

        CarResponse result = carMapper.toDto(carRepository.save(car));

        log.info("Successfully restored car: ID={}", id);
        return result;
    }

    @EvictCarCaches
    public CarResponse updateCarPrice(Long id, CarPriceUpdateRequest priceUpdateRequest) {
        log.debug("Updating car price for id: {}", id);

        Car car = carLookup.requireActiveById(id);
        BigDecimal oldPrice = car.getPrice();
        car.setPrice(priceUpdateRequest.price());
        car.setCurrencyType(priceUpdateRequest.currencyType());
        car.setUpdateTime(Instant.now(clock));

        CarResponse result = carMapper.toDto(carRepository.save(car));

        log.info("Successfully updated car price: ID={}, {} -> {}", result.id(), oldPrice, result.price());
        return result;
    }

    @Transactional(readOnly = true)
    public List<String> validateCarData(CarRequest carRequest) {
        List<String> errors = new ArrayList<>();

        if (!StringUtils.hasText(carRequest.licensePlate())) {
            errors.add("License plate is required");
        }

        if (!StringUtils.hasText(carRequest.brand())) {
            errors.add("Brand is required");
        }

        if (!StringUtils.hasText(carRequest.model())) {
            errors.add("Model is required");
        }

        if (carRequest.productionYear() == null) {
            errors.add("Production year is required");
        }

        if (carRequest.price() == null) {
            errors.add("Price is required");
        }

        if (carRequest.currencyType() == null) {
            errors.add("Currency type is required");
        }

        if (carRequest.carStatusType() == null) {
            errors.add("Car status type is required");
        }

        if (carRequest.productionYear() != null && carRequest.productionYear() > LocalDate.now(clock).getYear()) {
            errors.add("Production year cannot be in the future");
        }

        if (carRequest.price() != null && carRequest.price().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Price must be positive");
        }

        return errors;
    }

    private void validateCarRequest(CarRequest carRequest) {
        if (carRequest == null) {
            throw new CarValidationException("Car request cannot be null");
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
        if (!StringUtils.hasText(vinNumber)) {
            return;
        }
        if (carRepository.existsByVinNumber(vinNumber)) {
            throw new CarAlreadyExistsException("Car already exists with vinNumber: " + vinNumber);
        }
    }
}
