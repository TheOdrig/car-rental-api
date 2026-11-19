package com.akif.car.internal.service;

import com.akif.car.api.CarFleetOperations;
import com.akif.car.api.CarResponse;
import com.akif.car.api.CarStatusType;
import com.akif.car.domain.Car;
import com.akif.car.internal.cache.EvictCarCaches;
import com.akif.car.internal.dto.request.CarStatusUpdateRequest;
import com.akif.car.internal.mapper.CarMapper;
import com.akif.car.internal.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CarStatusService implements CarFleetOperations {

    private final Clock clock;
    private final CarRepository carRepository;
    private final CarMapper carMapper;
    private final CarLookup carLookup;

    @Override
    @EvictCarCaches
    public CarResponse markAsMaintenance(Long id) {
        log.debug("Marking car as maintenance with id: {}", id);

        Car car = carLookup.requireActiveById(id);
        car.markAsMaintenance();
        CarResponse result = saveAndMap(car);

        log.info("Successfully marked car as maintenance: ID={}, License Plate={}", result.id(),
                result.licensePlate());
        return result;
    }

    @EvictCarCaches
    public CarResponse markAsAvailable(Long id) {
        log.debug("Marking car as available with id: {}", id);

        Car car = carLookup.requireActiveById(id);
        car.markAsAvailable();
        CarResponse result = saveAndMap(car);

        log.info("Successfully marked car as available: ID={}, License Plate={}", result.id(),
                result.licensePlate());
        return result;
    }

    @EvictCarCaches
    public CarResponse updateCarStatus(Long id, CarStatusUpdateRequest statusUpdateRequest) {
        log.debug("Updating car status for id: {} to {}", id, statusUpdateRequest.carStatusType());

        Car car = carLookup.requireActiveById(id);
        CarStatusType oldStatus = car.getCarStatusType();
        CarStatusType newStatus = statusUpdateRequest.carStatusType();

        car.changeFleetStatus(newStatus);
        CarResponse result = saveAndMap(car);

        log.info("Successfully updated car status: ID={}, {} -> {}", result.id(), oldStatus, newStatus);
        return result;
    }

    private CarResponse saveAndMap(Car car) {
        car.setUpdateTime(Instant.now(clock));
        return carMapper.toDto(carRepository.save(car));
    }
}
