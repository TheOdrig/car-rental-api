package com.akif.service.availability.impl;

import com.akif.dto.availability.SimilarCarDto;
import com.akif.shared.enums.CarStatusType;
import com.akif.exception.CarNotFoundException;
import com.akif.model.Car;
import com.akif.repository.CarRepository;
import com.akif.service.availability.ICarAvailabilityService;
import com.akif.service.availability.ISimilarCarService;
import com.akif.service.pricing.IDynamicPricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SimilarCarServiceImpl implements ISimilarCarService {

    private final CarRepository carRepository;
    private final ICarAvailabilityService carAvailabilityService;
    private final IDynamicPricingService dynamicPricingService;

    @Override
    public List<SimilarCarDto> findSimilarAvailableCars(Long carId, LocalDate startDate, LocalDate endDate, int limit) {
        log.debug("Finding similar available cars for car: {} from {} to {}, limit: {}", 
                carId, startDate, endDate, limit);

        Car referenceCar = carRepository.findByIdAndIsDeletedFalse(carId)
                .orElseThrow(() -> new CarNotFoundException(carId));

        BigDecimal referencePrice = referenceCar.getPrice();
        BigDecimal minPrice = referencePrice.multiply(BigDecimal.valueOf(0.8));
        BigDecimal maxPrice = referencePrice.multiply(BigDecimal.valueOf(1.2));

        log.debug("Reference car: {} {}, price: {}, range: {} - {}", 
                referenceCar.getBrand(), referenceCar.getModel(), referencePrice, minPrice, maxPrice);

        List<CarStatusType> blockingStatuses = Arrays.asList(CarStatusType.getUnavailableStatuses());
        Pageable pageable = PageRequest.of(0, limit * 3);
        
        Page<Car> similarCarsPage = carRepository.findSimilarCars(
                referenceCar.getBodyType(),
                minPrice,
                maxPrice,
                carId,
                blockingStatuses,
                pageable
        );

        List<SimilarCarDto> similarCars = new ArrayList<>();

        for (Car car : similarCarsPage.getContent()) {
            if (similarCars.size() >= limit) {
                break;
            }

            boolean available = carAvailabilityService.isCarAvailable(car.getId(), startDate, endDate);
            if (!available) {
                log.debug("Car {} is not available, skipping", car.getId());
                continue;
            }

            int similarityScore = 0;
            List<String> similarityReasons = new ArrayList<>();

            if (referenceCar.getBodyType() != null && 
                referenceCar.getBodyType().equalsIgnoreCase(car.getBodyType())) {
                similarityScore += 50;
                similarityReasons.add("Same body type");
            }

            if (referenceCar.getBrand() != null && 
                referenceCar.getBrand().equalsIgnoreCase(car.getBrand())) {
                similarityScore += 30;
                similarityReasons.add("Same brand");
            }

            if (car.getPrice() != null && 
                car.getPrice().compareTo(minPrice) >= 0 && 
                car.getPrice().compareTo(maxPrice) <= 0) {
                similarityScore += 20;
                similarityReasons.add("Similar price");
            }

            var pricingResult = dynamicPricingService.calculatePrice(
                    car.getId(),
                    startDate,
                    endDate,
                    LocalDate.now()
            );

            SimilarCarDto similarCarDto = SimilarCarDto.builder()
                    .id(car.getId())
                    .brand(car.getBrand())
                    .model(car.getModel())
                    .productionYear(car.getProductionYear())
                    .bodyType(car.getBodyType())
                    .dailyRate(pricingResult.effectiveDailyPrice())
                    .totalPrice(pricingResult.finalPrice())
                    .currency(car.getCurrencyType())
                    .imageUrl(car.getImageUrl())
                    .similarityReasons(similarityReasons)
                    .similarityScore(similarityScore)
                    .build();

            similarCars.add(similarCarDto);
        }

        similarCars.sort((a, b) -> b.getSimilarityScore().compareTo(a.getSimilarityScore()));

        log.info("Found {} similar available cars for car: {}", similarCars.size(), carId);
        return similarCars;
    }
}
