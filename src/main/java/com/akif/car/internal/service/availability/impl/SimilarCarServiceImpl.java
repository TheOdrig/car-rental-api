package com.akif.car.internal.service.availability.impl;

import com.akif.car.api.PricingResult;
import com.akif.car.internal.config.SimilarCarProperties;
import com.akif.car.internal.dto.availability.SimilarCarDto;
import com.akif.car.internal.service.pricing.BatchPricing;
import com.akif.car.api.CarStatusType;
import com.akif.car.internal.exception.CarNotFoundException;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.car.api.CarBookingCalendar;
import com.akif.car.internal.service.availability.SimilarCarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SimilarCarServiceImpl implements SimilarCarService {

    private final CarRepository carRepository;
    private final CarBookingCalendar bookingCalendar;
    private final BatchPricing batchPricing;
    private final SimilarCarProperties properties;
    private final Clock clock;

    @Override
    public List<SimilarCarDto> findSimilarAvailableCars(Long carId, LocalDate startDate, LocalDate endDate, int limit) {
        log.debug("Finding similar available cars for car: {} from {} to {}, limit: {}",
                carId, startDate, endDate, limit);

        Car referenceCar = carRepository.findByIdAndIsDeletedFalse(carId)
                .orElseThrow(() -> new CarNotFoundException(carId));

        BigDecimal referencePrice = referenceCar.getPrice();
        BigDecimal minPrice = referencePrice.multiply(BigDecimal.ONE.subtract(properties.priceBand()));
        BigDecimal maxPrice = referencePrice.multiply(BigDecimal.ONE.add(properties.priceBand()));

        log.debug("Reference car: {} {}, price: {}, range: {} - {}",
                referenceCar.getBrand(), referenceCar.getModel(), referencePrice, minPrice, maxPrice);

        List<CarStatusType> blockingStatuses = Arrays.asList(CarStatusType.getUnavailableStatuses());
        Pageable pageable = PageRequest.of(0, limit * properties.candidateMultiplier());

        Page<Car> similarCarsPage = carRepository.findSimilarCars(
                referenceCar.getBodyType(),
                minPrice,
                maxPrice,
                carId,
                blockingStatuses,
                pageable);

        Set<Long> bookedCarIds = bookingCalendar.findBookedCarIds(startDate, endDate);

        List<Car> candidates = new ArrayList<>();
        for (Car car : similarCarsPage.getContent()) {
            if (candidates.size() >= limit) {
                break;
            }
            if (bookedCarIds.contains(car.getId())) {
                log.debug("Car {} is not available, skipping", car.getId());
                continue;
            }
            candidates.add(car);
        }

        Map<Long, PricingResult> prices = candidates.isEmpty()
                ? Map.of()
                : batchPricing.priceCars(candidates, startDate, endDate, LocalDate.now(clock));

        List<SimilarCarDto> similarCars = new ArrayList<>();

        for (Car car : candidates) {

            int similarityScore = 0;
            List<String> similarityReasons = new ArrayList<>();

            if (referenceCar.getBodyType() != null &&
                    referenceCar.getBodyType().equalsIgnoreCase(car.getBodyType())) {
                similarityScore += properties.bodyTypeWeight();
                similarityReasons.add("Same body type");
            }

            if (referenceCar.getBrand() != null &&
                    referenceCar.getBrand().equalsIgnoreCase(car.getBrand())) {
                similarityScore += properties.brandWeight();
                similarityReasons.add("Same brand");
            }

            if (car.getPrice() != null &&
                    car.getPrice().compareTo(minPrice) >= 0 &&
                    car.getPrice().compareTo(maxPrice) <= 0) {
                similarityScore += properties.priceWeight();
                similarityReasons.add("Similar price");
            }

            PricingResult pricing = prices.get(car.getId());
            BigDecimal dailyRate = pricing.effectiveDailyPrice();
            BigDecimal totalPrice = pricing.finalPrice();

            SimilarCarDto similarCarDto = new SimilarCarDto(
                    car.getId(),
                    car.getBrand(),
                    car.getModel(),
                    car.getProductionYear(),
                    car.getBodyType(),
                    dailyRate,
                    totalPrice,
                    car.getCurrencyType(),
                    car.getImageUrl(),
                    similarityReasons,
                    similarityScore);

            similarCars.add(similarCarDto);
        }

        similarCars.sort((a, b) -> b.similarityScore().compareTo(a.similarityScore()));

        log.info("Found {} similar available cars for car: {}", similarCars.size(), carId);
        return similarCars;
    }
}