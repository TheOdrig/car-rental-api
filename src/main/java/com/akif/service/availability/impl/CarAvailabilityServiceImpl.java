package com.akif.service.availability.impl;

import com.akif.dto.availability.*;
import com.akif.dto.currency.ConversionResult;
import com.akif.enums.AvailabilityStatus;
import com.akif.enums.CarStatusType;
import com.akif.enums.CurrencyType;
import com.akif.enums.RentalStatus;
import com.akif.exception.CarNotFoundException;
import com.akif.exception.RentalValidationException;
import com.akif.model.Car;
import com.akif.model.Rental;
import com.akif.repository.CarRepository;
import com.akif.repository.RentalRepository;
import com.akif.service.availability.ICarAvailabilityService;
import com.akif.service.currency.ICurrencyConversionService;
import com.akif.service.pricing.IDynamicPricingService;
import com.akif.service.pricing.PriceModifier;
import com.akif.service.pricing.PricingResult;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CarAvailabilityServiceImpl implements ICarAvailabilityService {

    private final CarRepository carRepository;
    private final RentalRepository rentalRepository;
    private final IDynamicPricingService dynamicPricingService;
    private final ICurrencyConversionService currencyConversionService;

    @Override
    public AvailabilitySearchResponseDto searchAvailableCars(AvailabilitySearchRequestDto request) {
        log.debug("Searching available cars for date range: {} to {}", request.getStartDate(), request.getEndDate());

        validateRentalDates(request.getStartDate(), request.getEndDate());

        validatePageSize(request.getSize());

        Pageable pageable = createPageable(request);

        List<CarStatusType> blockingStatuses = Arrays.asList(CarStatusType.getUnavailableStatuses());

        Page<Car> availableCars = carRepository.findAvailableCarsForDateRange(
                request.getStartDate(),
                request.getEndDate(),
                blockingStatuses,
                request.getBrand(),
                request.getModel(),
                request.getFuelType(),
                request.getTransmissionType(),
                request.getBodyType(),
                request.getMinSeats(),
                request.getMinPrice(),
                request.getMaxPrice(),
                request.getMinProductionYear(),
                request.getMaxProductionYear(),
                pageable
        );

        int rentalDays = (int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;

        List<AvailableCarDto> carDtos = availableCars.getContent().stream()
                .map(car -> convertToAvailableCarDto(car, request.getStartDate(), request.getEndDate(), request.getTargetCurrency()))
                .toList();

        AvailabilitySearchResponseDto response = AvailabilitySearchResponseDto.builder()
                .cars(carDtos)
                .totalElements(availableCars.getTotalElements())
                .totalPages(availableCars.getTotalPages())
                .currentPage(availableCars.getNumber())
                .pageSize(availableCars.getSize())
                .searchStartDate(request.getStartDate())
                .searchEndDate(request.getEndDate())
                .rentalDays(rentalDays)
                .build();

        log.info("Found {} available cars for date range {} to {}", 
                response.getTotalElements(), request.getStartDate(), request.getEndDate());

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

        long overlappingCount = rentalRepository.countOverlappingRentals(carId, startDate, endDate);
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
        String blockReason = carBlocked ? car.getCarStatusType().getDisplayName() : null;

        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        List<Rental> rentals = rentalRepository.findOverlappingRentalsForCar(
                carId,
                monthStart,
                monthEnd,
                Arrays.asList(RentalStatus.CONFIRMED, RentalStatus.IN_USE)
        );

        List<DayAvailabilityDto> days = new ArrayList<>();
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            
            DayAvailabilityDto dayDto;
            if (carBlocked) {
                dayDto = DayAvailabilityDto.builder()
                        .date(date)
                        .status(AvailabilityStatus.UNAVAILABLE)
                        .build();
            } else {
                Rental overlappingRental = rentals.stream()
                        .filter(r -> !date.isBefore(r.getStartDate()) && !date.isAfter(r.getEndDate()))
                        .findFirst()
                        .orElse(null);

                if (overlappingRental != null) {
                    dayDto = DayAvailabilityDto.builder()
                            .date(date)
                            .status(AvailabilityStatus.UNAVAILABLE)
                            .rentalId(overlappingRental.getId())
                            .build();
                } else {
                    dayDto = DayAvailabilityDto.builder()
                            .date(date)
                            .status(AvailabilityStatus.AVAILABLE)
                            .build();
                }
            }
            days.add(dayDto);
        }

        CarAvailabilityCalendarDto calendar = CarAvailabilityCalendarDto.builder()
                .carId(carId)
                .carName(car.getBrand() + " " + car.getModel())
                .month(month)
                .days(days)
                .carBlocked(carBlocked)
                .blockReason(blockReason)
                .build();

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
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                unavailableDates.add(current);
                current = current.plusDays(1);
            }
            log.debug("Car {} has blocking status, all {} dates unavailable", carId, unavailableDates.size());
            return unavailableDates;
        }

        List<Rental> rentals = rentalRepository.findOverlappingRentalsForCar(
                carId,
                startDate,
                endDate,
                Arrays.asList(RentalStatus.CONFIRMED, RentalStatus.IN_USE)
        );

        for (Rental rental : rentals) {
            LocalDate rentalStart = rental.getStartDate().isBefore(startDate) ? startDate : rental.getStartDate();
            LocalDate rentalEnd = rental.getEndDate().isAfter(endDate) ? endDate : rental.getEndDate();

            LocalDate current = rentalStart;
            while (!current.isAfter(rentalEnd)) {
                if (!unavailableDates.contains(current)) {
                    unavailableDates.add(current);
                }
                current = current.plusDays(1);
            }
        }

        log.debug("Found {} unavailable dates for car: {}", unavailableDates.size(), carId);
        return unavailableDates;
    }


    private void validateRentalDates(LocalDate startDate, LocalDate endDate) {
        if (startDate.isBefore(LocalDate.now())) {
            throw new RentalValidationException("Start date cannot be in the past");
        }
        if (endDate.isBefore(startDate)) {
            throw new RentalValidationException("End date must be after start date");
        }
        if (startDate.isAfter(endDate)) {
            throw new RentalValidationException("Invalid date range");
        }

        long days = ChronoUnit.DAYS.between(startDate, endDate);
        if (days > 90) {
            throw new RentalValidationException("Rental period cannot exceed 90 days");
        }
    }

    private void validatePageSize(Integer size) {
        if (size != null && (size < 1 || size > 100)) {
            throw new RentalValidationException("Page size must be between 1 and 100");
        }
    }

    private void validateCalendarMonth(YearMonth month) {
        YearMonth currentMonth = YearMonth.now();
        YearMonth maxMonth = currentMonth.plusMonths(3);

        if (month.isBefore(currentMonth)) {
            throw new RentalValidationException("Calendar month cannot be in the past");
        }
        if (month.isAfter(maxMonth)) {
            throw new RentalValidationException("Calendar only available up to 3 months in advance");
        }
    }

    private Pageable createPageable(AvailabilitySearchRequestDto request) {
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "price";
        String sortDirection = request.getSortDirection() != null ? request.getSortDirection() : "asc";
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;
        
        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy
        );
        return PageRequest.of(page, size, sort);
    }

    private AvailableCarDto convertToAvailableCarDto(Car car, LocalDate startDate, LocalDate endDate, CurrencyType targetCurrency) {
        PricingResult pricingResult = dynamicPricingService.calculatePrice(
                car.getId(),
                startDate,
                endDate,
                LocalDate.now()
        );

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

        return AvailableCarDto.builder()
                .id(car.getId())
                .brand(car.getBrand())
                .model(car.getModel())
                .productionYear(car.getProductionYear())
                .bodyType(car.getBodyType())
                .fuelType(car.getFuelType())
                .transmissionType(car.getTransmissionType())
                .seats(car.getSeats())
                .imageUrl(car.getImageUrl())
                .rating(car.getRating())
                .dailyRate(dailyRate)
                .totalPrice(totalPrice)
                .currency(displayCurrency)
                .appliedDiscounts(appliedDiscounts)
                .build();
    }
}
