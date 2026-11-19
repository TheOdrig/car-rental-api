package com.akif.rental.internal.service;

import java.time.Clock;
import com.akif.car.api.CarDto;
import com.akif.car.api.CarStatusType;
import com.akif.car.api.CarDatesUnavailableException;
import com.akif.car.api.CarNotAvailableException;
import com.akif.rental.api.RentalResponse;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.internal.config.RentalProperties;
import com.akif.rental.internal.exception.OpenRentalRequestLimitException;
import com.akif.rental.api.RentalRequest;
import com.akif.rental.internal.mapper.RentalMapper;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.domain.model.RentalDatePolicy;
import com.akif.auth.api.UserDto;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.car.api.DynamicPricingService;
import com.akif.car.api.PriceModifier;
import com.akif.car.api.PricingResult;
import com.akif.shared.enums.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RentalBookingService {

    private static final String EXCLUSION_VIOLATION = "23P01";

    private final Clock clock;
    private final RentalRepository rentalRepository;
    private final RentalMapper rentalMapper;
    private final DynamicPricingService dynamicPricingService;
    private final RentalProperties rentalProperties;
    private final RentalLookups lookups;

    @Transactional
    public RentalResponse requestRental(RentalRequest request, Long userId) {
        log.info("Creating rental request for user: {}, car: {}", userId, request.carId());

        UserDto user = lookups.findUserById(userId);
        CarDto car = lookups.findCarById(request.carId());

        if (!car.status().equals(CarStatusType.AVAILABLE)) {
            throw new CarNotAvailableException(
                    car.id(),
                    "Car status is: " + car.status().getDisplayName());
        }

        validateRentalDates(request.startDate(), request.endDate());
        checkOpenRequestLimit(user.id());
        checkDateOverlap(car.id(), request.startDate(), request.endDate());

        PricingResult pricingResult = dynamicPricingService.calculatePrice(
                request.carId(),
                request.startDate(),
                request.endDate(),
                LocalDate.now(clock));

        int days = pricingResult.rentalDays();
        BigDecimal dailyPrice = pricingResult.effectiveDailyPrice();
        BigDecimal totalPrice = pricingResult.finalPrice();
        CurrencyType currency = car.currency();

        log.info("Dynamic pricing applied: base={}, final={}, modifiers={}",
                pricingResult.baseTotalPrice(), pricingResult.finalPrice(), pricingResult.appliedModifiers().size());

        Rental rental = Rental.builder()
                .userId(user.id())
                .carId(car.id())
                .carBrand(car.brand())
                .carModel(car.model())
                .carLicensePlate(car.licensePlate())
                .carThumbnailUrl(car.thumbnailUrl())
                .userEmail(user.email())
                .userFullName(user.firstName() + " " + user.lastName())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .days(days)
                .dailyPrice(dailyPrice)
                .totalPrice(totalPrice)
                .currency(currency)
                .status(RentalStatus.REQUESTED)
                .build();

        Rental savedRental = saveBooking(rental);
        RentalResponse result = rentalMapper.toDto(savedRental);

        result = new RentalResponse(
                result.id(),
                result.carSummary(),
                result.userSummary(),
                result.startDate(),
                result.endDate(),
                result.days(),
                result.dailyPrice(),
                result.totalPrice(),
                result.currency(),
                result.status(),
                pricingResult.baseTotalPrice(),
                pricingResult.finalPrice(),
                pricingResult.totalSavings(),
                pricingResult.appliedModifiers().stream()
                        .map(PriceModifier::description)
                        .toList(),
                result.convertedTotalPrice(),
                result.displayCurrency(),
                result.exchangeRate(),
                result.rateSource(),
                result.pickupNotes(),
                result.returnNotes(),
                result.approvalNotes(),
                result.cancellationReason(),
                result.createTime(),
                result.updateTime());

        lookups.logRentalOperationSuccess("created", result);
        return result;
    }

    private void validateRentalDates(LocalDate startDate, LocalDate endDate) {
        new RentalDatePolicy(rentalProperties.getMaxLeadDays(), rentalProperties.getMaxRentalDays())
                .validate(startDate, endDate, LocalDate.now(clock));
    }

    private void checkOpenRequestLimit(Long userId) {
        rentalRepository.acquireAdvisoryTransactionLock("rental-open-requests:" + userId);
        long open = rentalRepository.countByUserIdAndStatusAndIsDeletedFalse(userId, RentalStatus.REQUESTED);
        if (open >= rentalProperties.getMaxOpenRequestsPerUser()) {
            throw new OpenRentalRequestLimitException(rentalProperties.getMaxOpenRequestsPerUser());
        }
    }

    private void checkDateOverlap(Long carId, LocalDate startDate, LocalDate endDate) {
        long overlappingCount = rentalRepository.countOverlappingRentals(carId, startDate, endDate);
        if (overlappingCount > 0) {
            throw CarDatesUnavailableException.bookedForDates(carId, startDate, endDate);
        }
    }

    private Rental saveBooking(Rental rental) {
        try {
            return rentalRepository.saveAndFlush(rental);
        } catch (DataIntegrityViolationException e) {
            if (isExclusionViolation(e)) {
                throw CarDatesUnavailableException.bookedForDates(
                        rental.getCarId(), rental.getStartDate(), rental.getEndDate());
            }
            throw e;
        }
    }

    private static boolean isExclusionViolation(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException && EXCLUSION_VIOLATION.equals(sqlException.getSQLState())) {
                return true;
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        return false;
    }
}
