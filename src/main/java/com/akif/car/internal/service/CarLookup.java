package com.akif.car.internal.service;

import com.akif.car.domain.Car;
import com.akif.car.internal.exception.CarNotFoundException;
import com.akif.car.internal.exception.CarValidationException;
import com.akif.car.internal.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class CarLookup {

    private final CarRepository carRepository;

    public Car requireActiveById(Long id) {
        validateId(id);
        return carRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new CarNotFoundException("Car not found with id: " + id));
    }

    public Car requireAnyById(Long id) {
        validateId(id);
        return carRepository.findById(id)
                .orElseThrow(() -> new CarNotFoundException("Car not found with id: " + id));
    }

    public Car requireActiveByLicensePlate(String licensePlate) {
        if (!StringUtils.hasText(licensePlate)) {
            throw new CarValidationException("License plate cannot be null or empty");
        }
        return carRepository.findByLicensePlateAndIsDeletedFalse(licensePlate)
                .orElseThrow(() -> new CarNotFoundException("Car not found with license plate: " + licensePlate));
    }

    public void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new CarValidationException("Invalid car ID: " + id);
        }
    }
}
