package com.akif.car.internal.service;

import com.akif.car.api.CarResponse;
import com.akif.car.api.CarStatusType;
import com.akif.car.domain.Car;
import com.akif.car.internal.cache.EvictCarCaches;
import com.akif.car.internal.dto.request.CarStatusUpdateRequest;
import com.akif.car.internal.exception.CarCannotBeReservedException;
import com.akif.car.internal.mapper.CarMapper;
import com.akif.car.internal.repository.CarRepository;
import com.akif.shared.exception.InvalidStatusTransitionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CarStatusService {

    private static final Map<CarStatusType, Set<CarStatusType>> VALID_TRANSITIONS = Map.of(
            CarStatusType.AVAILABLE,
            Set.of(CarStatusType.RESERVED, CarStatusType.RENTED, CarStatusType.MAINTENANCE,
                    CarStatusType.DAMAGED, CarStatusType.INSPECTION),
            CarStatusType.RESERVED,
            Set.of(CarStatusType.AVAILABLE, CarStatusType.RENTED, CarStatusType.MAINTENANCE,
                    CarStatusType.DAMAGED),
            CarStatusType.RENTED,
            Set.of(CarStatusType.AVAILABLE, CarStatusType.MAINTENANCE, CarStatusType.DAMAGED,
                    CarStatusType.INSPECTION),
            CarStatusType.MAINTENANCE,
            Set.of(CarStatusType.AVAILABLE, CarStatusType.DAMAGED, CarStatusType.INSPECTION),
            CarStatusType.DAMAGED,
            Set.of(CarStatusType.AVAILABLE, CarStatusType.MAINTENANCE, CarStatusType.INSPECTION),
            CarStatusType.INSPECTION,
            Set.of(CarStatusType.AVAILABLE, CarStatusType.MAINTENANCE, CarStatusType.DAMAGED));

    private final Clock clock;
    private final CarRepository carRepository;
    private final CarMapper carMapper;
    private final CarLookup carLookup;

    @EvictCarCaches
    public CarResponse reserveCar(Long id) {
        log.debug("Reserving car with id: {}", id);

        Car car = carLookup.requireById(id);

        if (!car.canBeReserved()) {
            log.warn("Car {} cannot be reserved, current status: {}", car.getId(), car.getCarStatusType());
            throw new CarCannotBeReservedException("Car cannot be reserved, current status: " + car.getCarStatusType());
        }

        car.markAsReserved();
        CarResponse result = saveAndMap(car);

        log.info("Successfully reserved car: ID={}, License Plate={}", result.getId(), result.getLicensePlate());
        return result;
    }

    @EvictCarCaches
    public CarResponse cancelReservation(Long id) {
        log.debug("Cancelling reservation for car with id: {}", id);

        Car car = carLookup.requireById(id);

        if (car.getCarStatusType() != CarStatusType.RESERVED) {
            log.warn("Car {} is not reserved, current status: {}", car.getId(), car.getCarStatusType());
            throw new InvalidStatusTransitionException(
                    "Car is not reserved, current status: " + car.getCarStatusType());
        }

        car.markAsAvailable();
        CarResponse result = saveAndMap(car);

        log.info("Successfully cancelled reservation for car: ID={}, License Plate={}", result.getId(),
                result.getLicensePlate());
        return result;
    }

    @EvictCarCaches
    public CarResponse releaseCar(Long id) {
        log.debug("Releasing car after rental ends, id: {}", id);

        Car car = carLookup.requireById(id);

        if (car.getCarStatusType() != CarStatusType.RESERVED) {
            log.warn("Car {} is not reserved, current status: {}", car.getId(), car.getCarStatusType());
            throw new InvalidStatusTransitionException(
                    "Car is not in RESERVED status, current status: " + car.getCarStatusType());
        }

        car.markAsAvailable();
        CarResponse result = saveAndMap(car);

        log.info("Successfully released car after rental: ID={}, License Plate={}", result.getId(),
                result.getLicensePlate());
        return result;
    }

    @EvictCarCaches
    public CarResponse markAsMaintenance(Long id) {
        log.debug("Marking car as maintenance with id: {}", id);

        Car car = carLookup.requireById(id);
        car.markAsMaintenance();
        CarResponse result = saveAndMap(car);

        log.info("Successfully marked car as maintenance: ID={}, License Plate={}", result.getId(),
                result.getLicensePlate());
        return result;
    }

    @EvictCarCaches
    public CarResponse markAsAvailable(Long id) {
        log.debug("Marking car as available with id: {}", id);

        Car car = carLookup.requireById(id);
        car.markAsAvailable();
        CarResponse result = saveAndMap(car);

        log.info("Successfully marked car as available: ID={}, License Plate={}", result.getId(),
                result.getLicensePlate());
        return result;
    }

    @EvictCarCaches
    public CarResponse updateCarStatus(Long id, CarStatusUpdateRequest statusUpdateRequest) {
        log.debug("Updating car status for id: {} to {}", id, statusUpdateRequest.carStatusType());

        Car car = carLookup.requireById(id);
        CarStatusType oldStatus = car.getCarStatusType();
        CarStatusType newStatus = statusUpdateRequest.carStatusType();

        validateStatusTransition(oldStatus, newStatus);

        car.setCarStatusType(newStatus);
        CarResponse result = saveAndMap(car);

        log.info("Successfully updated car status: ID={}, {} -> {}", result.getId(), oldStatus, newStatus);
        return result;
    }

    @Transactional(readOnly = true)
    public boolean canCarBeReserved(Long id) {
        return carLookup.requireById(id).canBeReserved();
    }

    private CarResponse saveAndMap(Car car) {
        car.setUpdateTime(LocalDateTime.now(clock));
        return carMapper.toDto(carRepository.save(car));
    }

    private void validateStatusTransition(CarStatusType currentStatus, CarStatusType newStatus) {
        Set<CarStatusType> allowedTransitions = VALID_TRANSITIONS.get(currentStatus);
        if (allowedTransitions == null || !allowedTransitions.contains(newStatus)) {
            throw new InvalidStatusTransitionException(
                    String.format("Invalid status transition from %s to %s", currentStatus, newStatus));
        }
    }
}
