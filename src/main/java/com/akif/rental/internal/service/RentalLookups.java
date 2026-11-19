package com.akif.rental.internal.service;

import com.akif.car.api.CarDto;
import com.akif.car.api.CarStatusType;
import com.akif.car.api.CarNotAvailableException;
import com.akif.rental.api.RentalResponse;
import com.akif.payment.api.PaymentDto;
import com.akif.rental.internal.exception.RentalPaymentNotFoundException;
import com.akif.rental.internal.exception.RentalNotFoundException;
import com.akif.rental.domain.model.Rental;
import com.akif.car.api.CarCatalog;
import com.akif.auth.api.UserLookup;
import com.akif.auth.api.UserDto;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.payment.api.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RentalLookups {

    private final RentalRepository rentalRepository;
    private final CarCatalog carCatalog;
    private final UserLookup userLookup;
    private final PaymentService paymentService;

    void requireCarAvailable(Long carId) {
        CarDto car = findCarById(carId);
        if (car.status() != CarStatusType.AVAILABLE) {
            throw new CarNotAvailableException(
                    carId,
                    "Car status is: " + car.status().getDisplayName());
        }
    }

    Rental findRentalForUpdate(Long id) {
        return rentalRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RentalNotFoundException(id));
    }

    Rental findRentalById(Long id) {
        return rentalRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RentalNotFoundException(id));
    }

    CarDto findCarById(Long id) {
        return carCatalog.getCarForBooking(id);
    }

    UserDto findUserById(Long userId) {
        return userLookup.getUserById(userId);
    }

    PaymentDto findPaymentByRentalId(Long rentalId) {
        return paymentService.getPaymentByRentalId(rentalId)
                .orElseThrow(() -> new RentalPaymentNotFoundException(rentalId));
    }

    void logRentalOperationSuccess(String operation, RentalResponse result) {
        log.info("Successfully {} rental: ID={}, Status={}",
                operation, result.id(), result.status());
    }

    void logRentalOperationSuccess(String operation, RentalResponse result, String extraInfo) {
        log.info("Successfully {} rental: ID={}, Status={}, {}",
                operation, result.id(), result.status(), extraInfo);
    }
}
