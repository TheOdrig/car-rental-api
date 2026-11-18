package com.akif.car.internal.service.pricing;

import com.akif.car.internal.dto.pricing.PricingResult;

import java.time.LocalDate;
import java.util.List;

public interface DynamicPricingService {

    PricingResult calculatePrice(Long carId, LocalDate startDate, LocalDate endDate, LocalDate bookingDate);

    PricingResult previewPrice(Long carId, LocalDate startDate, LocalDate endDate);

    List<PricingStrategy> getEnabledStrategies();
}
