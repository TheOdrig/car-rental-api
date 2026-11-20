package com.akif.car.internal.service.availability.impl;

import java.time.Clock;

import com.akif.car.internal.dto.availability.AvailabilitySearchResponse;
import com.akif.car.internal.dto.availability.AvailableCarDto;
import com.akif.car.internal.dto.availability.CarAvailabilityCalendarDto;
import com.akif.car.api.DayAvailabilityDto;
import com.akif.car.internal.dto.availability.*;
import com.akif.car.internal.dto.availability.AvailabilitySearchRequest;
import com.akif.currency.api.ConversionResult;
import com.akif.car.api.AvailabilityStatus;
import com.akif.car.api.CarStatusType;
import com.akif.shared.enums.CurrencyType;
import com.akif.car.internal.exception.CarNotFoundException;
import com.akif.car.internal.exception.AvailabilityValidationException;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.car.api.BookedPeriod;
import com.akif.car.internal.config.AvailabilityProperties;
import com.akif.car.api.CarBookingCalendar;
import com.akif.car.internal.service.availability.CarAvailabilityService;
import com.akif.car.internal.service.availability.RentalWindow;
import com.akif.currency.api.CurrencyConversionService;
import com.akif.car.internal.service.pricing.BatchPricing;
import com.akif.car.api.PriceModifier;
import com.akif.car.api.PricingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import com.akif.shared.time.RentalDays;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CarAvailabilityServiceImpl implements CarAvailabilityService {

    private final Clock clock;
    private final AvailabilityProperties availabilityProperties;

    private final CarRepository carRepository;
    private final CarBookingCalendar bookingCalendar;
    private final BatchPricing batchPricing;
    private final CurrencyConversionService currencyConversionService;

    @Override
    public AvailabilitySearchResponse searchAvailableCars(AvailabilitySearchRequest request) {
        log.debug("Searching available cars for date range: {} to {}", request.startDate(), request.endDate());

        validateRentalDates(request.startDate(), request.endDate());

        validatePageSize(request.size());

        Pageable pageable = createPageable(request);

        List<CarStatusType> blockingStatuses = Arrays.asList(CarStatusType.getUnavailableStatuses());

        Set<Long> bookedCarIds = bookingCalendar.findBookedCarIds(request.startDate(), request.endDate());

        Page<Car> availableCars = carRepository.findAvailableCarsForDateRange(
                bookedCarIds,
                blockingStatuses,
                request.brand() != null ? request.brand().toLowerCase() : null,
                request.model() != null ? request.model().toLowerCase() : null,
                request.fuelType() != null ? request.fuelType().toLowerCase() : null,
                request.transmissionType() != null ? request.transmissionType().toLowerCase() : null,
                request.bodyType() != null ? request.bodyType().toLowerCase() : null,
                request.minSeats(),
                request.minPrice(),
                request.maxPrice(),
                request.minProductionYear(),
                request.maxProductionYear(),
                pageable);

        int rentalDays = RentalDays.inclusiveDays(request.startDate(), request.endDate());

        Map<Long, PricingResult> prices = batchPricing.priceCars(availableCars.getContent(), request.startDate(),
                request.endDate(), LocalDate.now(clock));

        List<AvailableCarDto> carDtos = availableCars.getContent().stream()
                .map(car -> convertToAvailableCarDto(car, prices.get(car.getId()), request.targetCurrency()))
                .toList();

        AvailabilitySearchResponse response = new AvailabilitySearchResponse(
                carDtos,
                availableCars.getTotalElements(),
                availableCars.getTotalPages(),
                availableCars.getNumber(),
                availableCars.getSize(),
                request.startDate(),
                request.endDate(),
                rentalDays);

        log.info("Found {} available cars for date range {} to {}",
                response.totalElements(), request.startDate(), request.endDate());

        return response;
    }

    @Override
    public boolean isCarAvailable(Long carId, LocalDate startDate, LocalDate endDate) {
        log.debug("Checking availability for car: {} from {} to {}", carId, startDate, endDate);

        Car car = carRepository.findByIdAndIsDeletedFalse(carId)
                .orElseThrow(() -> new CarNotFoundException(carId));

        if (Arrays.asList(CarStatusType.getUnavailableStatuses()).contains(car.getCarStatusType())) {
            log.debug("Car {} is unavailable due to status: {}", carId, car.getCarStatusType());
            return false;
        }

        long overlappingCount = bookingCalendar.countActiveBookings(carId, startDate, endDate);
        boolean available = overlappingCount == 0;

        log.debug("Car {} availability: {} (overlapping rentals: {})", carId, available, overlappingCount);
        return available;
    }

    @Override
    public CarAvailabilityCalendarDto getCarAvailabilityCalendar(Long carId, YearMonth month) {
        log.debug("Getting availability calendar for car: {} for month: {}", carId, month);

        validateCalendarMonth(month);

        Car car = carRepository.findByIdAndIsDeletedFalse(carId)
                .orElseThrow(() -> new CarNotFoundException(carId));

        boolean carBlocked = Arrays.asList(CarStatusType.getUnavailableStatuses()).contains(car.getCarStatusType());
        String blockReason = carBlocked ? car.getCarStatusType().name() : null;

        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        List<BookedPeriod> rentals = bookingCalendar.findBookedPeriods(carId, monthStart, monthEnd);

        List<DayAvailabilityDto> days = new ArrayList<>();
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);

            DayAvailabilityDto dayDto;
            if (carBlocked) {
                dayDto = new DayAvailabilityDto(date, AvailabilityStatus.UNAVAILABLE);
            } else {
                BookedPeriod overlappingRental = rentals.stream()
                        .filter(r -> !date.isBefore(r.startDate()) && !date.isAfter(r.endDate()))
                        .findFirst()
                        .orElse(null);

                if (overlappingRental != null) {
                    dayDto = new DayAvailabilityDto(date, AvailabilityStatus.UNAVAILABLE);
                } else {
                    dayDto = new DayAvailabilityDto(date, AvailabilityStatus.AVAILABLE);
                }
            }
            days.add(dayDto);
        }

        CarAvailabilityCalendarDto calendar = new CarAvailabilityCalendarDto(
                carId,
                car.getBrand() + " " + car.getModel(),
                month,
                days,
                carBlocked,
                blockReason);

        log.info("Generated calendar for car: {} for month: {} with {} days", carId, month, days.size());
        return calendar;
    }

    @Override
    public List<LocalDate> getUnavailableDates(Long carId, LocalDate startDate, LocalDate endDate) {
        log.debug("Getting unavailable dates for car: {} from {} to {}", carId, startDate, endDate);

        Car car = carRepository.findByIdAndIsDeletedFalse(carId)
                .orElseThrow(() -> new CarNotFoundException(carId));

        List<LocalDate> unavailableDates = new ArrayList<>();

        if (Arrays.asList(CarStatusType.getUnavailableStatuses()).contains(car.getCarStatusType())) {
            RentalDays.eachDay(startDate, endDate).forEach(unavailableDates::add);
            log.debug("Car {} has blocking status, all {} dates unavailable", carId, unavailableDates.size());
            return unavailableDates;
        }

        List<BookedPeriod> rentals = bookingCalendar.findBookedPeriods(carId, startDate, endDate);

        for (BookedPeriod rental : rentals) {
            LocalDate rentalStart = rental.startDate().isBefore(startDate) ? startDate : rental.startDate();
            LocalDate rentalEnd = rental.endDate().isAfter(endDate) ? endDate : rental.endDate();

            RentalDays.eachDay(rentalStart, rentalEnd)
                    .filter(date -> !unavailableDates.contains(date))
                    .forEach(unavailableDates::add);
        }

        log.debug("Found {} unavailable dates for car: {}", unavailableDates.size(), carId);
        return unavailableDates;
    }

    private void validateRentalDates(LocalDate startDate, LocalDate endDate) {
        RentalWindow.validate(startDate, endDate, LocalDate.now(clock), availabilityProperties.maxSearchDays());
    }

    private void validatePageSize(Integer size) {
        if (size != null && (size < 1 || size > 100)) {
            throw new AvailabilityValidationException("Page size must be between 1 and 100");
        }
    }

    private void validateCalendarMonth(YearMonth month) {
        YearMonth currentMonth = YearMonth.now(clock);
        YearMonth maxMonth = currentMonth.plusMonths(3);

        if (month.isBefore(currentMonth)) {
            throw new AvailabilityValidationException("Calendar month cannot be in the past");
        }
        if (month.isAfter(maxMonth)) {
            throw new AvailabilityValidationException("Calendar only available up to 3 months in advance");
        }
    }

    private Pageable createPageable(AvailabilitySearchRequest request) {
        String sortBy = request.sortBy() != null ? request.sortBy() : "price";
        String sortDirection = request.sortDirection() != null ? request.sortDirection() : "asc";
        int page = request.page() != null ? request.page() : 0;
        int size = request.size() != null ? request.size() : 20;

        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy);
        return PageRequest.of(page, size, sort);
    }

    private AvailableCarDto convertToAvailableCarDto(Car car, PricingResult pricingResult,
                                                     CurrencyType targetCurrency) {
        List<String> appliedDiscounts = pricingResult.appliedModifiers().stream()
                .map(PriceModifier::description)
                .toList();

        CurrencyType originalCurrency = car.getCurrencyType() != null ? car.getCurrencyType() : CurrencyType.TRY;
        CurrencyType displayCurrency = targetCurrency != null ? targetCurrency : originalCurrency;

        BigDecimal dailyRate = pricingResult.effectiveDailyPrice();
        BigDecimal totalPrice = pricingResult.finalPrice();

        if (!originalCurrency.equals(displayCurrency)) {
            ConversionResult dailyConversion = currencyConversionService.convert(
                    dailyRate, originalCurrency, displayCurrency);
            ConversionResult totalConversion = currencyConversionService.convert(
                    totalPrice, originalCurrency, displayCurrency);

            dailyRate = dailyConversion.convertedAmount();
            totalPrice = totalConversion.convertedAmount();
        }

        return new AvailableCarDto(
                car.getId(),
                car.getBrand(),
                car.getModel(),
                car.getProductionYear(),
                car.getBodyType(),
                car.getFuelType(),
                car.getTransmissionType(),
                car.getSeats(),
                car.getImageUrl(),
                car.getRating(),
                dailyRate,
                totalPrice,
                displayCurrency,
                appliedDiscounts);
    }
}